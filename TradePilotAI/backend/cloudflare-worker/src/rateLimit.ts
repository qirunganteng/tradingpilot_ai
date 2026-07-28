import type { Env } from "./types";

const WINDOW_SECONDS = 60;
const MAX_REQUESTS_PER_WINDOW = 10;

export async function checkRateLimit(env: Env, deviceId: string): Promise<{ allowed: boolean; remaining: number }> {
  const windowKey = `ratelimit:${deviceId}:${Math.floor(Date.now() / (WINDOW_SECONDS * 1000))}`;
  const current = parseInt((await env.RATE_LIMIT_KV.get(windowKey)) ?? "0", 10);

  if (current >= MAX_REQUESTS_PER_WINDOW) {
    return { allowed: false, remaining: 0 };
  }

  await env.RATE_LIMIT_KV.put(windowKey, String(current + 1), { expirationTtl: WINDOW_SECONDS * 2 });
  return { allowed: true, remaining: MAX_REQUESTS_PER_WINDOW - current - 1 };
}
