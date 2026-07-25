import { Miniflare } from "miniflare";
import { readFileSync } from "fs";
import { execSync } from "child_process";

// Build ulang bundle dari source terbaru setiap script ini dijalankan,
// supaya E2E test selalu menguji kode src/ yang sebenarnya, bukan bundle basi.
execSync("npx esbuild src/index.ts --bundle --format=esm --platform=neutral --outfile=scripts/worker-bundle.mjs", { stdio: "inherit" });

const script = readFileSync("./scripts/worker-bundle.mjs", "utf8");

// Fungsi JS langsung sebagai "outbound service" — dipanggil Miniflare
// untuk SEMUA fetch() keluar dari worker utama. Cara resmi Miniflare
// untuk mock network eksternal (Gemini API) di level workerd (bukan
// cuma Node undici) sehingga tidak diblokir oleh sandbox network.
async function outboundMock(request) {
  const url = new URL(request.url);
  if (url.hostname === "generativelanguage.googleapis.com") {
    const payload = {
      pair: "EURUSD", trend: "Bullish", signal: "BUY", confidence: 81,
      entry: "1.0850", stop_loss: "1.0820", take_profit: "1.0910",
      risk_reward: "1:2", reasoning: "Manual Miniflare test - Order Block + BOS bullish."
    };
    return new Response(JSON.stringify({
      candidates: [{ content: { parts: [{ text: JSON.stringify(payload) }] } }]
    }), { status: 200, headers: { "Content-Type": "application/json" } });
  }
  return new Response("Not mocked: " + url.href, { status: 502 });
}

const mf = new Miniflare({
  modules: true,
  script,
  scriptPath: "./scripts/worker-bundle.mjs",
  compatibilityDate: "2024-12-30",
  d1Databases: ["DB"],
  r2Buckets: ["CHART_BUCKET"],
  kvNamespaces: ["RATE_LIMIT_KV"],
  bindings: {
    GEMINI_API_KEY: "fake-key-manual-test",
    GATEWAY_AUTH_TOKEN: "manual-test-token",
    GEMINI_MODEL: "gemini-2.5-flash",
    ENVIRONMENT: "manual-test"
  },
  outboundService: outboundMock
});

async function main() {
  const db = await mf.getD1Database("DB");
  await db.exec(
    `CREATE TABLE IF NOT EXISTS analyses ( id TEXT PRIMARY KEY, device_id TEXT NOT NULL, pair TEXT NOT NULL, trend TEXT NOT NULL, signal TEXT NOT NULL, confidence REAL NOT NULL, entry TEXT NOT NULL, stop_loss TEXT NOT NULL, take_profit TEXT NOT NULL, risk_reward TEXT NOT NULL, reasoning TEXT NOT NULL, methods TEXT NOT NULL, provider TEXT NOT NULL DEFAULT 'gemini', image_r2_key TEXT, latency_ms INTEGER, created_at INTEGER NOT NULL )`
  );
  await db.exec(
    `CREATE TABLE IF NOT EXISTS request_log ( id TEXT PRIMARY KEY, device_id TEXT NOT NULL, endpoint TEXT NOT NULL, status_code INTEGER NOT NULL, error_message TEXT, created_at INTEGER NOT NULL )`
  );

  let passed = 0, failed = 0;
  function check(label, cond) {
    if (cond) { console.log(`  PASS: ${label}`); passed++; }
    else { console.log(`  FAIL: ${label}`); failed++; }
  }

  console.log("\n[1] Health check tanpa auth");
  const health = await mf.dispatchFetch("https://example.com/api/v1/health");
  check("status 200", health.status === 200);
  const healthBody = await health.json();
  check("status field ok", healthBody.status === "ok");

  console.log("\n[2] Analyze tanpa token -> 401");
  const unauth = await mf.dispatchFetch("https://example.com/api/v1/analyze", {
    method: "POST",
    body: JSON.stringify({ imageBase64: "abc", deviceId: "d1" })
  });
  check("status 401", unauth.status === 401);

  console.log("\n[3] Analyze dengan token benar -> 200, cek D1 & R2 nyata");
  const imageBase64 = Buffer.from("fake-jpeg-bytes-manual-test").toString("base64");
  const res = await mf.dispatchFetch("https://example.com/api/v1/analyze", {
    method: "POST",
    headers: { "x-gateway-token": "manual-test-token", "Content-Type": "application/json" },
    body: JSON.stringify({ imageBase64, deviceId: "manual-device-1", methods: ["ICT", "SMC"] })
  });
  check("status 200", res.status === 200);
  const body = await res.json();
  if (res.status !== 200) {
    console.log("  DEBUG body:", JSON.stringify(body));
  }
  check("pair EURUSD", body.pair === "EURUSD");
  check("signal BUY", body.signal === "BUY");
  check("imageKey ada", typeof body.imageKey === "string" && body.imageKey.length > 0);

  const r2 = await mf.getR2Bucket("CHART_BUCKET");
  const obj = await r2.get(body.imageKey);
  check("gambar benar-benar ada di R2", obj !== null);
  if (obj) {
    const buf = await obj.arrayBuffer();
    check("ukuran file R2 > 0 byte", buf.byteLength > 0);
  }

  const row = await db.prepare("SELECT * FROM analyses WHERE id = ?").bind(body.id).first();
  check("row tersimpan di D1", row !== null);
  check("row.pair benar", row && row.pair === "EURUSD");
  check("row.device_id benar", row && row.device_id === "manual-device-1");

  console.log("\n[4] History endpoint");
  const hist = await mf.dispatchFetch("https://example.com/api/v1/analyses?deviceId=manual-device-1", {
    headers: { "x-gateway-token": "manual-test-token" }
  });
  check("status 200", hist.status === 200);
  const histBody = await hist.json();
  check("ada minimal 1 record", histBody.analyses.length >= 1);

  console.log("\n[5] Rate limit (>10 request/menit -> 429)");
  let lastStatus = 0;
  for (let i = 0; i < 12; i++) {
    const r = await mf.dispatchFetch("https://example.com/api/v1/analyze", {
      method: "POST",
      headers: { "x-gateway-token": "manual-test-token", "Content-Type": "application/json" },
      body: JSON.stringify({ imageBase64, deviceId: "manual-ratelimit-device", storeImage: false })
    });
    lastStatus = r.status;
  }
  check("request ke-12 kena rate limit (429)", lastStatus === 429);

  console.log(`\n=== HASIL: ${passed} PASS, ${failed} FAIL ===`);
  await mf.dispose();
  process.exit(failed > 0 ? 1 : 0);
}

main().catch((e) => { console.error(e); process.exit(1); });
