import type { Env } from "./types";

const WINDOW_SECONDS = 60;
const MAX_REQUESTS_PER_WINDOW = 10; // per device per menit — cukup longgar untuk pemakaian normal, cegah abuse/kuota Gemini jebol

/**
 * Rate limit sederhana berbasis KV (fixed window counter). Cukup untuk
 * skala kecil-menengah; kalau traffic besar, ganti ke Durable Objects
 * untuk counter yang lebih akurat (tidak rawan race condition).
 */
export async function checkRateLimit(env: Env, deviceId: string): Promise<{ allowed: boolean; remaining: number }> {
  const windowKey = `ratelimit:${deviceId}:${Math.floor(Date.now() / (WINDOW_SECONDS * 1000))}`;
  const current = parseInt((await env.RATE_LIMIT_KV.get(windowKey)) ?? "0", 10);

  if (current >= MAX_REQUESTS_PER_WINDOW) {
    return { allowed: false, remaining: 0 };
  }

  await env.RATE_LIMIT_KV.put(windowKey, String(current + 1), { expirationTtl: WINDOW_SECONDS * 2 });
  return { allowed: true, remaining: MAX_REQUESTS_PER_WINDOW - current - 1 };
}
