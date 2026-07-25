import type { Env } from "./types";

/**
 * Otentikasi sederhana: app Android mengirim header x-gateway-token yang
 * cocok dengan secret GATEWAY_AUTH_TOKEN. Ini BUKAN pengganti otentikasi
 * user sungguhan (tidak ada login user di app ini) — tujuannya hanya
 * mencegah endpoint dipanggil sembarangan orang di luar app resmi.
 * Untuk keamanan lebih kuat di masa depan: ganti ke JWT per-install atau
 * App Attestation (Play Integrity API).
 */
export function isAuthorized(request: Request, env: Env): boolean {
  const token = request.headers.get("x-gateway-token");
  return !!token && token === env.GATEWAY_AUTH_TOKEN;
}
