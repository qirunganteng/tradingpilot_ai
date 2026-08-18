import type { AnalysisResult, Env } from "./types";

export async function insertAnalysis(env: Env, deviceId: string, result: AnalysisResult, latencyMs: number): Promise<void> {
  await env.DB.prepare(
    `INSERT INTO analyses
      (id, device_id, pair, trend, signal, confidence, entry, stop_loss, take_profit, risk_reward, reasoning, methods, provider, image_r2_key, latency_ms, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(
      result.id,
      deviceId,
      result.pair,
      result.trend,
      result.signal,
      result.confidence,
      result.entry,
      result.stopLoss,
      result.takeProfit,
      result.riskReward,
      result.reasoning,
      JSON.stringify(result.method),
      result.providerUsed,
      result.imageKey,
      latencyMs,
      result.timestampMillis
    )
    .run();
}

export async function listAnalyses(env: Env, deviceId: string, limit = 50) {
  const { results } = await env.DB.prepare(
    `SELECT id, pair, trend, signal, confidence, entry, stop_loss, take_profit, risk_reward,
            reasoning, methods, provider, image_r2_key, created_at
     FROM analyses
     WHERE device_id = ?
     ORDER BY created_at DESC
     LIMIT ?`
  )
    .bind(deviceId, limit)
    .all();
  return results;
}

export async function logRequest(
  env: Env,
  deviceId: string,
  endpoint: string,
  statusCode: number,
  errorMessage: string | null
): Promise<void> {
  await env.DB.prepare(
    `INSERT INTO request_log (id, device_id, endpoint, status_code, error_message, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`
  )
    .bind(crypto.randomUUID(), deviceId, endpoint, statusCode, errorMessage, Date.now())
    .run();
}

/**
 * PRD 2.2.18 "Sync" / 10.5 -- see the header comment in
 * migrations/0002_sync_blobs.sql for why this is one generic
 * (device_id, data_type) -> JSON blob table rather than PRD §9.2's fully
 * normalized per-column tables. `dataType` is whatever the client sends
 * ("workspaces", "bookmarks", "history", "watchlist", "journal",
 * "price_alerts", "passwords", "permissions", "downloads", ...) -- this
 * layer doesn't validate or interpret the shape, it just stores and
 * returns it verbatim, so adding a new syncable data type on the Flutter
 * side never requires a backend migration.
 */
export async function upsertSyncBlob(env: Env, deviceId: string, dataType: string, payload: string): Promise<void> {
  await env.DB.prepare(
    `INSERT INTO sync_blobs (device_id, data_type, payload, updated_at)
     VALUES (?, ?, ?, ?)
     ON CONFLICT (device_id, data_type) DO UPDATE SET payload = excluded.payload, updated_at = excluded.updated_at`
  )
    .bind(deviceId, dataType, payload, Date.now())
    .run();
}

export interface SyncBlobRow {
  data_type: string;
  payload: string;
  updated_at: number;
}

export async function listSyncBlobs(env: Env, deviceId: string, dataType?: string): Promise<SyncBlobRow[]> {
  const query = dataType
    ? env.DB.prepare(`SELECT data_type, payload, updated_at FROM sync_blobs WHERE device_id = ? AND data_type = ?`).bind(
        deviceId,
        dataType
      )
    : env.DB.prepare(`SELECT data_type, payload, updated_at FROM sync_blobs WHERE device_id = ?`).bind(deviceId);
  const { results } = await query.all<SyncBlobRow>();
  return results;
}
