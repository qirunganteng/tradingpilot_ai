import type { AnalysisResult, AnalyzeRequestBody, CalculateRiskRequestBody, ChatStreamRequestBody, Env, OcrRequestBody, OcrResult, SyncPushRequestBody } from "./types";
import { analyzeChartWithGemini, GeminiError } from "./gemini";
import { streamChat, ChatProviderError } from "./chat";
import { extractLabelsWithGemini } from "./ocr";
import { calculateRisk, RiskValidationError } from "./riskEngine";
import { storeChartImage } from "./storage";
import { insertAnalysis, listAnalyses, listSyncBlobs, logRequest, upsertSyncBlob } from "./db";
import { checkRateLimit } from "./rateLimit";
import { isAuthorized } from "./auth";
import { DEFAULT_METHODS } from "./promptBuilder";

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" }
  });
}

function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

async function handleAnalyze(request: Request, env: Env): Promise<Response> {
  const started = Date.now();
  let deviceId = "unknown";

  try {
    const body = (await request.json()) as AnalyzeRequestBody;
    deviceId = body.deviceId || "unknown";

    if (!body.imageBase64) {
      return json({ error: "imageBase64 wajib diisi" }, 400);
    }

    const rate = await checkRateLimit(env, deviceId);
    if (!rate.allowed) {
      await logRequest(env, deviceId, "/api/v1/analyze", 429, "rate limited");
      return json({ error: "Terlalu banyak permintaan, coba lagi sebentar lagi." }, 429);
    }

    const methods = body.methods && body.methods.length > 0 ? body.methods : DEFAULT_METHODS;
    const mimeType = body.mimeType || "image/jpeg";

    const outcome = await analyzeChartWithGemini(env, body.imageBase64, mimeType, methods);

    let imageKey: string | null = null;
    if (body.storeImage !== false) {
      try {
        imageKey = await storeChartImage(env, deviceId, base64ToArrayBuffer(body.imageBase64), mimeType);
      } catch (e) {
        console.error("Gagal simpan gambar ke R2:", e);
      }
    }

    const result: AnalysisResult = {
      id: crypto.randomUUID(),
      pair: outcome.pair,
      trend: outcome.trend,
      signal: outcome.signal,
      confidence: outcome.confidence,
      entry: outcome.entry,
      stopLoss: outcome.stopLoss,
      takeProfit: outcome.takeProfit,
      riskReward: outcome.riskReward,
      reasoning: outcome.reasoning,
      method: methods,
      providerUsed: "gemini",
      timestampMillis: Date.now(),
      imageKey
    };

    const latencyMs = Date.now() - started;

    try {
      await insertAnalysis(env, deviceId, result, latencyMs);
    } catch (e) {
      console.error("Gagal insert D1:", e);
    }

    try {
      await logRequest(env, deviceId, "/api/v1/analyze", 200, null);
    } catch (e) {
      console.error("Gagal insert request_log:", e);
    }

    return json(result, 200);
  } catch (e) {
    const isGeminiError = e instanceof GeminiError;
    const status = isGeminiError ? e.status : 500;
    const message = e instanceof Error ? e.message : "Internal error";
    await logRequest(env, deviceId, "/api/v1/analyze", status, message).catch(() => {});
    return json({ error: message }, status >= 400 && status < 600 ? status : 500);
  }
}

/**
 * Fase 6: OCR (Konstitusi bagian BACKEND). Beda dari /api/v1/analyze --
 * endpoint ini HANYA membaca ulang teks/angka di gambar, tidak membuat
 * kesimpulan trading. Rate limit & auth pakai jalur yang sama dengan
 * /analyze supaya tidak ada celah baru.
 */
async function handleOcr(request: Request, env: Env): Promise<Response> {
  let deviceId = "unknown";

  try {
    const body = (await request.json()) as OcrRequestBody;
    deviceId = body.deviceId || "unknown";

    if (!body.imageBase64) {
      return json({ error: "imageBase64 wajib diisi" }, 400);
    }

    const rate = await checkRateLimit(env, deviceId);
    if (!rate.allowed) {
      await logRequest(env, deviceId, "/api/v1/ocr", 429, "rate limited");
      return json({ error: "Terlalu banyak permintaan, coba lagi sebentar lagi." }, 429);
    }

    const mimeType = body.mimeType || "image/jpeg";
    const labels = await extractLabelsWithGemini(env, body.imageBase64, mimeType);

    const result: OcrResult = {
      id: crypto.randomUUID(),
      labels,
      timestampMillis: Date.now()
    };

    await logRequest(env, deviceId, "/api/v1/ocr", 200, null).catch(() => {});
    return json(result, 200);
  } catch (e) {
    const isGeminiError = e instanceof GeminiError;
    const status = isGeminiError ? e.status : 500;
    const message = e instanceof Error ? e.message : "Internal error";
    await logRequest(env, deviceId, "/api/v1/ocr", status, message).catch(() => {});
    return json({ error: message }, status >= 400 && status < 600 ? status : 500);
  }
}

/**
 * Fase 6 lanjutan: Risk Engine (Konstitusi bagian BACKEND: "Risk Service").
 * Kalkulasi murni matematika (tidak panggil Gemini), jadi TIDAK pakai
 * checkRateLimit yang sama dengan /analyze & /ocr (itu buat jaga kuota AI
 * yang berbayar/berbatas -- risk calc gratis & murah, tidak perlu dibatasi
 * seketat itu). Tetap wajib auth token seperti endpoint lain.
 */
async function handleCalculateRisk(request: Request, env: Env): Promise<Response> {
  let deviceId = "unknown";

  try {
    const body = (await request.json()) as CalculateRiskRequestBody;
    deviceId = body.deviceId || "unknown";

    const result = calculateRisk(body);

    await logRequest(env, deviceId, "/api/v1/calculate-risk", 200, null).catch(() => {});
    return json(result, 200);
  } catch (e) {
    const isValidationError = e instanceof RiskValidationError;
    const status = isValidationError ? 400 : 500;
    const message = e instanceof Error ? e.message : "Internal error";
    await logRequest(env, deviceId, "/api/v1/calculate-risk", status, message).catch(() => {});
    return json({ error: message }, status);
  }
}

async function handleHistory(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const deviceId = url.searchParams.get("deviceId");
  if (!deviceId) return json({ error: "deviceId wajib diisi" }, 400);

  const limitParam = url.searchParams.get("limit");
  const limit = limitParam ? Math.min(parseInt(limitParam, 10) || 50, 200) : 50;

  const results = await listAnalyses(env, deviceId, limit);
  return json({ analyses: results }, 200);
}

/**
 * PRD 10.5 "Sync" -- `POST /api/v1/sync` pushes one or more data-type
 * blobs (workspaces, bookmarks, history, watchlist, journal,
 * price_alerts, ...) for a device; `GET /api/v1/sync?deviceId=...`
 * (optionally `&dataType=...` for just one) pulls them back. No AI cost
 * involved, so no checkRateLimit here -- same reasoning as
 * handleCalculateRisk. See db.ts's upsertSyncBlob/listSyncBlobs and
 * migrations/0002_sync_blobs.sql for why this is one generic blob table
 * rather than PRD §9.2's fully normalized per-column schema (short
 * version: there's no user-account layer yet for a `user_id` foreign key
 * to point at, only per-device tokens -- see that migration's header
 * comment for the full reasoning and upgrade path).
 */
async function handleSyncPush(request: Request, env: Env): Promise<Response> {
  let deviceId = "unknown";
  try {
    const body = (await request.json()) as SyncPushRequestBody;
    deviceId = body.deviceId;
    if (!deviceId) return json({ error: "deviceId wajib diisi" }, 400);
    if (!Array.isArray(body.items) || body.items.length === 0) {
      return json({ error: "items wajib diisi (array, minimal 1)" }, 400);
    }
    for (const item of body.items) {
      if (!item.dataType) return json({ error: "Setiap item wajib punya dataType" }, 400);
      await upsertSyncBlob(env, deviceId, item.dataType, JSON.stringify(item.payload));
    }
    await logRequest(env, deviceId, "/api/v1/sync", 200, null).catch(() => {});
    return json({ synced: true, count: body.items.length, timestamp: Date.now() }, 200);
  } catch (e) {
    const message = e instanceof Error ? e.message : "Internal error";
    await logRequest(env, deviceId, "/api/v1/sync", 500, message).catch(() => {});
    return json({ error: message }, 500);
  }
}

async function handleSyncPull(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const deviceId = url.searchParams.get("deviceId");
  if (!deviceId) return json({ error: "deviceId wajib diisi" }, 400);
  const dataType = url.searchParams.get("dataType") ?? undefined;

  const rows = await listSyncBlobs(env, deviceId, dataType);
  const items = rows.map((row) => ({
    dataType: row.data_type,
    payload: JSON.parse(row.payload),
    updatedAt: row.updated_at
  }));
  return json({ items }, 200);
}

/**
 * Fase 7 -- the endpoint the Flutter client's AiStreamService actually
 * calls (`/api/v1/chat/stream`). Wraps chat.ts's unified async-generator
 * stream in the exact SSE wire format the client already knows how to
 * parse: `data: {"text": "..."}\n\n`, terminated with `data: [DONE]\n\n`.
 */
function handleChatStream(request: Request, env: Env): Promise<Response> {
  return (async () => {
    let deviceId = "unknown";
    let body: ChatStreamRequestBody;
    try {
      body = (await request.json()) as ChatStreamRequestBody;
    } catch {
      return json({ error: "Body harus JSON valid" }, 400);
    }
    deviceId = body.deviceId || "unknown";

    if (!body.prompt || !body.prompt.trim()) {
      return json({ error: "prompt wajib diisi" }, 400);
    }

    const rate = await checkRateLimit(env, deviceId);
    if (!rate.allowed) {
      await logRequest(env, deviceId, "/api/v1/chat/stream", 429, "rate limited").catch(() => {});
      return json({ error: "Terlalu banyak permintaan, coba lagi sebentar lagi." }, 429);
    }

    const provider = body.provider || "gemini";
    const encoder = new TextEncoder();

    const stream = new ReadableStream<Uint8Array>({
      async start(controller) {
        try {
          for await (const chunk of streamChat({ env, provider, prompt: body.prompt, systemContext: body.system_context })) {
            const event = `data: ${JSON.stringify({ text: chunk })}\n\n`;
            controller.enqueue(encoder.encode(event));
          }
          controller.enqueue(encoder.encode("data: [DONE]\n\n"));
          await logRequest(env, deviceId, "/api/v1/chat/stream", 200, null).catch(() => {});
        } catch (e) {
          const message = e instanceof Error ? e.message : "Internal error";
          const status = e instanceof ChatProviderError ? e.status : 500;
          const event = `data: ${JSON.stringify({ text: `\n\n[Gateway error ${status}: ${message}]` })}\n\n`;
          controller.enqueue(encoder.encode(event));
          controller.enqueue(encoder.encode("data: [DONE]\n\n"));
          await logRequest(env, deviceId, "/api/v1/chat/stream", status, message).catch(() => {});
        } finally {
          controller.close();
        }
      },
    });

    return new Response(stream, {
      status: 200,
      headers: {
        "Content-Type": "text/event-stream; charset=utf-8",
        "Cache-Control": "no-cache",
        Connection: "keep-alive",
      },
    });
  })();
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/api/v1/health") {
      return json({ status: "ok", environment: env.ENVIRONMENT, time: Date.now() });
    }

    if (!isAuthorized(request, env)) {
      return json({ error: "Unauthorized" }, 401);
    }

    if (url.pathname === "/api/v1/analyze" && request.method === "POST") {
      return handleAnalyze(request, env);
    }

    if (url.pathname === "/api/v1/ocr" && request.method === "POST") {
      return handleOcr(request, env);
    }

    if (url.pathname === "/api/v1/calculate-risk" && request.method === "POST") {
      return handleCalculateRisk(request, env);
    }

    if (url.pathname === "/api/v1/chat/stream" && request.method === "POST") {
      return handleChatStream(request, env);
    }

    if (url.pathname === "/api/v1/analyses" && request.method === "GET") {
      return handleHistory(request, env);
    }

    if (url.pathname === "/api/v1/sync" && request.method === "POST") {
      return handleSyncPush(request, env);
    }

    if (url.pathname === "/api/v1/sync" && request.method === "GET") {
      return handleSyncPull(request, env);
    }

    return json({ error: "Not found" }, 404);
  }
} satisfies ExportedHandler<Env>;
