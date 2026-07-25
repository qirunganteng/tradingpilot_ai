// Memberi tahu @cloudflare/vitest-pool-workers bentuk `env` di dalam test
// (import { env } from "cloudflare:test") supaya cocok dengan binding
// nyata di wrangler.toml (DB, CHART_BUCKET, RATE_LIMIT_KV, dst).
import type { Env } from "../src/types";

declare module "cloudflare:test" {
  interface ProvidedEnv extends Env {}
}
