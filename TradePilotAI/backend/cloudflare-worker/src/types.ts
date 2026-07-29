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

/**
 * Fase 6 (Konstitusi bagian BACKEND: "OCR" eksplisit disebut sebagai
 * tanggung jawab Backend, beda dari Risk Engine/Drawing Engine yang
 * SENGAJA tetap di Shared Module -- lihat catatan di ocr.ts).
 */
export interface OcrLabel {
  /** Teks/angka persis seperti yang tampil di chart, mis. "1.10523" atau "SNR 1.1050". */
  text: string;
  /** Konfidensi 0-1 seberapa yakin model terhadap pembacaan ini. */
  confidence: number;
}

export interface OcrResult {
  id: string;
  labels: OcrLabel[];
  timestampMillis: number;
}

export interface OcrRequestBody {
  imageBase64: string;
  mimeType?: string;
  deviceId: string;
}

/**
 * Fase 6 lanjutan: Risk Engine dipindah dari :shared (client) ke Backend,
 * atas keputusan eksplisit pemilik project -- supaya kalkulasi risk
 * benar-benar "satu sumber kebenaran" di server, bukan cuma "kode yang
 * sama di-compile ulang di tiap client".
 */
export interface CalculateRiskRequestBody {
  balance: number;
  riskPercent: number;
  entryPrice: number;
  stopLossPrice: number;
  takeProfitPrice: number;
  pipValuePerLotUsd?: number;
  pipSize?: number;
  deviceId: string;
}

export interface RiskRecommendation {
  riskPercent: number;
  lot: number;
  stopLoss: number;
  takeProfit: number;
  riskRewardRatio: number;
  maxDailyLoss: number;
  maxTrade: number;
}
