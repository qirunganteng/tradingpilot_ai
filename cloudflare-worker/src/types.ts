export interface Env {
  DB: D1Database;
  CHART_BUCKET: R2Bucket;
  RATE_LIMIT_KV: KVNamespace;
  GEMINI_API_KEY: string;
  GATEWAY_AUTH_TOKEN: string;
  GEMINI_MODEL: string;
  ENVIRONMENT: string;
}

export type TradeDirection = "BUY" | "SELL" | "NONE";

export interface AnalysisResult {
  id: string;
  pair: string;
  trend: string;
  signal: TradeDirection;
  confidence: number;
  entry: string;
  stopLoss: string;
  takeProfit: string;
  riskReward: string;
  reasoning: string;
  method: string[];
  providerUsed: string;
  timestampMillis: number;
  imageKey: string | null;
}

export interface AnalyzeRequestBody {
  imageBase64: string;
  mimeType?: string;
  methods?: string[];
  deviceId: string;
  storeImage?: boolean;
}
