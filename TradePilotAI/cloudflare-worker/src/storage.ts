import type { Env } from "./types";

export async function storeChartImage(
  env: Env,
  deviceId: string,
  imageBytes: ArrayBuffer,
  mimeType: string
): Promise<string> {
  const safeDeviceId = deviceId.replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 64) || "unknown";
  const key = `charts/${safeDeviceId}/${Date.now()}-${crypto.randomUUID()}.jpg`;

  await env.CHART_BUCKET.put(key, imageBytes, {
    httpMetadata: { contentType: mimeType || "image/jpeg" }
  });

  return key;
}

export async function getChartImage(env: Env, key: string): Promise<R2ObjectBody | null> {
  return env.CHART_BUCKET.get(key);
}
