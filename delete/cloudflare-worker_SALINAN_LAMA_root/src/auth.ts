import type { Env } from "./types";

export function isAuthorized(request: Request, env: Env): boolean {
  const token = request.headers.get("x-gateway-token");
  return !!token && token === env.GATEWAY_AUTH_TOKEN;
}
