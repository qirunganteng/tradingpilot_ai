import { defineWorkersConfig } from "@cloudflare/vitest-pool-workers/config";

export default defineWorkersConfig({
  test: {
    poolOptions: {
      workers: {
        wrangler: { configPath: "./wrangler.toml" },
        miniflare: {
          d1Databases: ["DB"],
          r2Buckets: ["CHART_BUCKET"],
          kvNamespaces: ["RATE_LIMIT_KV"],
          bindings: {
            GEMINI_API_KEY: "test-fake-key",
            GATEWAY_AUTH_TOKEN: "test-gateway-token",
            GEMINI_MODEL: "gemini-2.5-flash",
            ENVIRONMENT: "test"
          }
        }
      }
    }
  }
});
