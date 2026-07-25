-- TradePilot AI Gateway — D1 schema
-- CATATAN PENTING: tabel ini adalah AUDIT LOG hasil analisa AI di sisi server,
-- BUKAN pengganti Trading Journal lokal di app (Room, versi 3 blueprint).
-- Trading Journal (riwayat trade + PnL user) TETAP 100% lokal di device,
-- sesuai requirement awal "Semua data tersimpan lokal". D1 di sini hanya
-- menyimpan histori PANGGILAN ANALISA AI (untuk audit, rate-limit, dan
-- opsional sinkronisasi lintas-device di masa depan).

CREATE TABLE IF NOT EXISTS analyses (
    id TEXT PRIMARY KEY,
    device_id TEXT NOT NULL,
    pair TEXT NOT NULL,
    trend TEXT NOT NULL,
    signal TEXT NOT NULL,               -- BUY | SELL | NONE
    confidence REAL NOT NULL,
    entry TEXT NOT NULL,
    stop_loss TEXT NOT NULL,
    take_profit TEXT NOT NULL,
    risk_reward TEXT NOT NULL,
    reasoning TEXT NOT NULL,
    methods TEXT NOT NULL,              -- JSON array string, mis. ["ICT","SMC"]
    provider TEXT NOT NULL DEFAULT 'gemini',
    image_r2_key TEXT,                  -- null jika gambar tidak diarsipkan
    latency_ms INTEGER,
    created_at INTEGER NOT NULL         -- epoch millis
);

CREATE INDEX IF NOT EXISTS idx_analyses_device_created
    ON analyses (device_id, created_at DESC);

CREATE TABLE IF NOT EXISTS request_log (
    id TEXT PRIMARY KEY,
    device_id TEXT NOT NULL,
    endpoint TEXT NOT NULL,
    status_code INTEGER NOT NULL,
    error_message TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_request_log_device_created
    ON request_log (device_id, created_at DESC);
