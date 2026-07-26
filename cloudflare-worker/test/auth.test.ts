import { describe, it, expect } from "vitest";
import { isAuthorized } from "../src/auth";
import type { Env } from "../src/types";

describe("auth", () => {
  const fakeEnv = {
    GATEWAY_AUTH_TOKEN: "secret-token-123"
  } as Env;

  it("should return true when token matches header", () => {
    const req = new Request("http://localhost/api/v1/analyze", {
      headers: { "x-gateway-token": "secret-token-123" }
    });
    expect(isAuthorized(req, fakeEnv)).toBe(true);
  });

  it("should return false when token is missing or wrong", () => {
    const req1 = new Request("http://localhost/api/v1/analyze");
    expect(isAuthorized(req1, fakeEnv)).toBe(false);

    const req2 = new Request("http://localhost/api/v1/analyze", {
      headers: { "x-gateway-token": "wrong-token" }
    });
    expect(isAuthorized(req2, fakeEnv)).toBe(false);
  });
});
