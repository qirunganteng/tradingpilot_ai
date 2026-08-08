import type { AiProviderName, Env } from "./types";

/**
 * Fase 7 -- CONSTITUTION.md "AI GATEWAY":
 *   "AI tidak boleh berjalan lokal di Client. Semua request AI harus
 *    melewati Cloudflare AI Gateway. Gateway mendukung: Gemini, OpenAI,
 *    Claude, DeepSeek, Qwen."
 *
 * Every provider below speaks its own streaming wire format. This module's
 * only job is to call the right upstream with the right shape, and yield a
 * single, unified sequence of plain-text chunks -- so index.ts (and the
 * Flutter client) never have to know which provider answered.
 */

export class ChatProviderError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

interface StreamChatArgs {
  env: Env;
  provider: AiProviderName;
  prompt: string;
  systemContext?: Record<string, unknown> | null;
}

function withSystemContext(prompt: string, systemContext?: Record<string, unknown> | null): string {
  if (!systemContext || Object.keys(systemContext).length === 0) return prompt;
  return `Context: ${JSON.stringify(systemContext)}\n\n${prompt}`;
}

export async function* streamChat(args: StreamChatArgs): AsyncGenerator<string> {
  const { env, provider, prompt, systemContext } = args;
  const fullPrompt = withSystemContext(prompt, systemContext);

  switch (provider) {
    case "gemini":
      yield* streamGemini(env, fullPrompt);
      return;
    case "claude":
      yield* streamClaude(env, fullPrompt);
      return;
    case "openai":
      yield* streamOpenAiCompatible(fullPrompt, {
        apiKey: env.OPENAI_API_KEY,
        model: env.OPENAI_MODEL || "gpt-4o-mini",
        url: "https://api.openai.com/v1/chat/completions",
        providerLabel: "OpenAI",
      });
      return;
    case "deepseek":
      yield* streamOpenAiCompatible(fullPrompt, {
        apiKey: env.DEEPSEEK_API_KEY,
        model: env.DEEPSEEK_MODEL || "deepseek-chat",
        url: "https://api.deepseek.com/chat/completions",
        providerLabel: "DeepSeek",
      });
      return;
    case "qwen":
      yield* streamOpenAiCompatible(fullPrompt, {
        apiKey: env.QWEN_API_KEY,
        model: env.QWEN_MODEL || "qwen-plus",
        url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        providerLabel: "Qwen",
      });
      return;
    default:
      throw new ChatProviderError(`Unknown provider: ${provider}`, 400);
  }
}

// ---------------------------------------------------------------------------
// Gemini -- Google's own SSE shape (?alt=sse), one JSON object per event.
// ---------------------------------------------------------------------------
async function* streamGemini(env: Env, prompt: string): AsyncGenerator<string> {
  if (!env.GEMINI_API_KEY) {
    throw new ChatProviderError("GEMINI_API_KEY isn't configured on the backend.", 503);
  }
  const model = env.GEMINI_MODEL || "gemini-2.5-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:streamGenerateContent?alt=sse`;

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
    body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
  });
  if (!response.ok || !response.body) {
    const errText = await response.text().catch(() => "");
    throw new ChatProviderError(`Gemini error ${response.status}: ${errText.slice(0, 300)}`, response.status);
  }

  for await (const line of sseLines(response.body)) {
    try {
      const json = JSON.parse(line) as {
        candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
      };
      const text = json.candidates?.[0]?.content?.parts?.map((p) => p.text ?? "").join("");
      if (text) yield text;
    } catch {
      // Partial/non-JSON keepalive line -- ignore.
    }
  }
}

// ---------------------------------------------------------------------------
// Claude -- Anthropic Messages API streaming, event-typed SSE.
// ---------------------------------------------------------------------------
async function* streamClaude(env: Env, prompt: string): AsyncGenerator<string> {
  if (!env.ANTHROPIC_API_KEY) {
    throw new ChatProviderError("ANTHROPIC_API_KEY isn't configured on the backend.", 503);
  }
  const model = env.CLAUDE_MODEL || "claude-sonnet-4-6";

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": env.ANTHROPIC_API_KEY,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({
      model,
      max_tokens: 2048,
      stream: true,
      messages: [{ role: "user", content: prompt }],
    }),
  });
  if (!response.ok || !response.body) {
    const errText = await response.text().catch(() => "");
    throw new ChatProviderError(`Claude error ${response.status}: ${errText.slice(0, 300)}`, response.status);
  }

  for await (const line of sseLines(response.body)) {
    try {
      const json = JSON.parse(line) as {
        type?: string;
        delta?: { type?: string; text?: string };
      };
      if (json.type === "content_block_delta" && json.delta?.text) {
        yield json.delta.text;
      }
    } catch {
      // ignore non-JSON / control lines
    }
  }
}

// ---------------------------------------------------------------------------
// OpenAI-compatible (OpenAI itself, DeepSeek, Qwen/DashScope) -- all three
// speak the same `choices[0].delta.content` streaming shape.
// ---------------------------------------------------------------------------
async function* streamOpenAiCompatible(
  prompt: string,
  opts: { apiKey: string | undefined; model: string; url: string; providerLabel: string }
): AsyncGenerator<string> {
  if (!opts.apiKey) {
    throw new ChatProviderError(`${opts.providerLabel} API key isn't configured on the backend.`, 503);
  }

  const response = await fetch(opts.url, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${opts.apiKey}` },
    body: JSON.stringify({
      model: opts.model,
      stream: true,
      messages: [{ role: "user", content: prompt }],
    }),
  });
  if (!response.ok || !response.body) {
    const errText = await response.text().catch(() => "");
    throw new ChatProviderError(`${opts.providerLabel} error ${response.status}: ${errText.slice(0, 300)}`, response.status);
  }

  for await (const line of sseLines(response.body)) {
    if (line === "[DONE]") return;
    try {
      const json = JSON.parse(line) as {
        choices?: Array<{ delta?: { content?: string } }>;
      };
      const text = json.choices?.[0]?.delta?.content;
      if (text) yield text;
    } catch {
      // ignore keepalive/comment lines
    }
  }
}

// ---------------------------------------------------------------------------
// Shared: decode a fetch() ReadableStream of `data: {...}` SSE lines into
// the raw JSON-string payload of each event, buffering across chunk
// boundaries so a JSON object split across two TCP reads still parses.
// ---------------------------------------------------------------------------
async function* sseLines(body: ReadableStream<Uint8Array>): AsyncGenerator<string> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      const parts = buffer.split("\n");
      buffer = parts.pop() ?? "";
      for (const part of parts) {
        const line = part.trim();
        if (line.startsWith("data:")) {
          const payload = line.slice(5).trim();
          if (payload) yield payload;
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}
