import type { Env } from "./types";

/**
 * Accepts either header style so the Flutter client (which sends
 * `Authorization: Bearer <token>`) and any existing tooling using the
 * original `x-gateway-token` header both work against the same gateway
 * token, without the client and backend having to renegotiate a header
 * name.
 */
export function isAuthorized(request: Request, env: Env): boolean {
  const legacyToken = request.headers.get("x-gateway-token");
  if (legacyToken && legacyToken === env.GATEWAY_AUTH_TOKEN) return true;

  const authHeader = request.headers.get("authorization") || request.headers.get("Authorization");
  if (authHeader?.toLowerCase().startsWith("bearer ")) {
    const bearerToken = authHeader.slice(7).trim();
    if (bearerToken && bearerToken === env.GATEWAY_AUTH_TOKEN) return true;
  }

  return false;
}
