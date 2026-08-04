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
