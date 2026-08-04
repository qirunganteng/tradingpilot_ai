import { describe, it, expect, beforeAll, vi, afterEach } from "vitest";
import { env, SELF } from "cloudflare:test";

const SCHEMA_SQL = `
CREATE TABLE IF NOT EXISTS analyses (
    id TEXT PRIMARY KEY, device_id TEXT NOT NULL, pair TEXT NOT NULL, trend TEXT NOT NULL,
    signal TEXT NOT NULL, confidence REAL NOT NULL, entry TEXT NOT NULL, stop_loss TEXT NOT NULL,
    take_profit TEXT NOT NULL, risk_reward TEXT NOT NULL, reasoning TEXT NOT NULL, methods TEXT NOT NULL,
    provider TEXT NOT NULL DEFAULT 'gemini', image_r2_key TEXT, latency_ms INTEGER, created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS request_log (
    id TEXT PRIMARY KEY, device_id TEXT NOT NULL, endpoint TEXT NOT NULL, status_code INTEGER NOT NULL,
    error_message TEXT, created_at INTEGER NOT NULL
);
`;

beforeAll(async () => {
  await env.DB.exec(SCHEMA_SQL.replace(/\n/g, " ").trim());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const AUTH_HEADERS = { "x-gateway-token": "test-gateway-token", "Content-Type": "application/json" };

function fakeGeminiResponse(overrides: Record<string, unknown> = {}) {
  const payload = {
    pair: "EURUSD",
    trend: "Bullish",
    signal: "BUY",
    confidence: 78,
    entry: "1.0850",
    stop_loss: "1.0820",
    take_profit: "1.0910",
    risk_reward: "1:2",
    reasoning: "Order Block terkonfirmasi setelah BOS bullish.",
    ...overrides
  };
  return {
    candidates: [{ content: { parts: [{ text: JSON.stringify(payload) }] } }]
  };
}

describe("GET /api/v1/health", () => {
  it("mengembalikan 200 tanpa perlu auth", async () => {
    const res = await SELF.fetch("https://example.com/api/v1/health");
    expect(res.status).toBe(200);
    const body = await res.json<{ status: string }>();
    expect(body.status).toBe("ok");
  });
});

describe("Auth", () => {
  it("menolak request tanpa token dengan 401", async () => {
    const res = await SELF.fetch("https://example.com/api/v1/analyze", {
      method: "POST",
      body: JSON.stringify({ imageBase64: "abc", deviceId: "device-1" })
    });
    expect(res.status).toBe(401);
  });

  it("menolak request dengan token salah", async () => {
    const res = await SELF.fetch("https://example.com/api/v1/analyze", {
      method: "POST",
      headers: { "x-gateway-token": "salah", "Content-Type": "application/json" },
      body: JSON.stringify({ imageBase64: "abc", deviceId: "device-1" })
    });
    expect(res.status).toBe(401);
  });
});

describe("POST /api/v1/analyze", () => {
  it("menolak jika imageBase64 kosong", async () => {
    const res = await SELF.fetch("https://example.com/api/v1/analyze", {
      method: "POST",
      headers: AUTH_HEADERS,
      body: JSON.stringify({ deviceId: "device-1" })
    });
    expect(res.status).toBe(400);
  });

  it("berhasil menganalisa, menyimpan ke D1 & R2, dan mengembalikan bentuk yang benar", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string) => {
        if (typeof url === "string" && url.includes("generativelanguage.googleapis.com")) {
          return new Response(JSON.stringify(fakeGeminiResponse()), { status: 200 });
        }
        throw new Error("Unexpected fetch: " + url);
      })
    );

    const tinyBase64 = btoa("fake-jpeg-bytes-for-test");
    const res = await SELF.fetch("https://example.com/api/v1/analyze", {
      method: "POST",
      headers: AUTH_HEADERS,
      body: JSON.stringify({ imageBase64: tinyBase64, deviceId: "device-e2e-test", methods: ["ICT", "SMC"], storeImage: false })
    });

    expect(res.status).toBe(200);
    const body = await res.json<any>();

    expect(body.pair).toBe("EURUSD");
    expect(body.signal).toBe("BUY");
    expect(body.confidence).toBe(78);
    expect(body.stopLoss).toBe("1.0820");
    expect(body.takeProfit).toBe("1.0910");
    expect(body.method).toEqual(["ICT", "SMC"]);
    expect(body.providerUsed).toBe("gemini");
        expect(body.imageKey).toBeNull(); // storeImage:false di test ini; R2 dites terpisah di r2-analyze.test.ts

    // Verifikasi benar-benar tersimpan di D1
    const row = await env.DB.prepare("SELECT * FROM analyses WHERE id = ?").bind(body.id).first();
    expect(row).not.toBeNull();
    expect(row?.pair).toBe("EURUSD");
    expect(row?.device_id).toBe("device-e2e-test");
  });

  it("tetap mengembalikan hasil analisa walau signal tidak dikenali -> NONE", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify(fakeGeminiResponse({ signal: "HOLD" })), { status: 200 }))
    );

    const res = await SELF.fetch("https://example.com/api/v1/analyze", {
      method: "POST",
      headers: AUTH_HEADERS,
      body: JSON.stringify({ imageBase64: btoa("x"), deviceId: "device-2", storeImage: false })
    });

    const body = await res.json<any>();
    expect(body.signal).toBe("NONE");
  });

  it("mengembalikan error terstruktur jika Gemini API gagal", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("quota exceeded", { status: 429 }))
    );

    const res = await SELF.fetch("https://example.com/api/v1/analyze", {
      method: "POST",
      headers: AUTH_HEADERS,
      body: JSON.stringify({ imageBase64: btoa("x"), deviceId: "device-3" })
    });

    expect(res.status).toBe(429);
    const body = await res.json<any>();
    expect(body.error).toBeDefined();
  });
});

describe("GET /api/v1/analyses", () => {
  it("menolak tanpa deviceId", async () => {
    const res = await SELF.fetch("https://example.com/api/v1/analyses", { headers: AUTH_HEADERS });
    expect(res.status).toBe(400);
  });

  it("mengembalikan histori device tertentu saja", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify(fakeGeminiResponse({ pair: "GBPUSD" })), { status: 200 }))
    );
    await SELF.fetch("https://example.com/api/v1/analyze", {
      method: "POST",
      headers: AUTH_HEADERS,
      body: JSON.stringify({ imageBase64: btoa("y"), deviceId: "device-history-test", storeImage: false })
    });

    const res = await SELF.fetch("https://example.com/api/v1/analyses?deviceId=device-history-test", {
      headers: AUTH_HEADERS
    });
    expect(res.status).toBe(200);
    const body = await res.json<any>();
    expect(Array.isArray(body.analyses)).toBe(true);
    expect(body.analyses.length).toBeGreaterThan(0);
    expect(body.analyses[0].pair).toBe("GBPUSD");
  });
});

describe("Rate limiting", () => {
  it("menolak setelah melewati batas per menit", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify(fakeGeminiResponse()), { status: 200 }))
    );

    const deviceId = "device-ratelimit-test";
    let lastStatus = 200;
    for (let i = 0; i < 12; i++) {
      const res = await SELF.fetch("https://example.com/api/v1/analyze", {
        method: "POST",
        headers: AUTH_HEADERS,
        body: JSON.stringify({ imageBase64: btoa("z" + i), deviceId, storeImage: false })
      });
      lastStatus = res.status;
    }
    expect(lastStatus).toBe(429);
  });
});
