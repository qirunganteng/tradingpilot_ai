import type { Env, TradeDirection } from "./types";
import { buildChartAnalysisPrompt } from "./promptBuilder";

interface RawGeminiAnalysis {
  pair?: string;
  trend?: string;
  signal?: string;
  confidence?: number;
  entry?: string;
  stop_loss?: string;
  take_profit?: string;
  risk_reward?: string;
  reasoning?: string;
}

export interface GeminiAnalysisOutcome {
  pair: string;
  trend: string;
  signal: TradeDirection;
  confidence: number;
  entry: string;
  stopLoss: string;
  takeProfit: string;
  riskReward: string;
  reasoning: string;
}

/**
 * Panggil Gemini generateContent (multimodal: gambar + prompt), lalu parse
 * teks JSON hasilnya. API key HANYA ada di sini (Worker secret), TIDAK PERNAH
 * dikirim ke/tersimpan di app Android — ini keuntungan utama pola AI Gateway.
 */
export async function analyzeChartWithGemini(
  env: Env,
  imageBase64: string,
  mimeType: string,
  methods: string[]
): Promise<GeminiAnalysisOutcome> {
  const prompt = buildChartAnalysisPrompt(methods);
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${env.GEMINI_MODEL}:generateContent`;

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": env.GEMINI_API_KEY
    },
    body: JSON.stringify({
      contents: [
        {
          parts: [
            { text: prompt },
            { inline_data: { mime_type: mimeType, data: imageBase64 } }
          ]
        }
      ]
    })
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => "");
    throw new GeminiError(`Gemini API error ${response.status}: ${errText.slice(0, 300)}`, response.status);
  }

  const data = (await response.json()) as {
    candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
  };

  const rawText = data.candidates?.[0]?.content?.parts?.find((p) => p.text)?.text;
  if (!rawText) {
    throw new GeminiError("Gemini tidak mengembalikan teks analisa", 502);
  }

  return parseGeminiJson(rawText);
}

export class GeminiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

function parseGeminiJson(rawText: string): GeminiAnalysisOutcome {
  const cleaned = rawText.trim().replace(/^```json/, "").replace(/^```/, "").replace(/```$/, "").trim();

  let raw: RawGeminiAnalysis;
  try {
    raw = JSON.parse(cleaned);
  } catch {
    throw new GeminiError("Gagal parse JSON dari Gemini: " + cleaned.slice(0, 200), 502);
  }

  const signalRaw = (raw.signal ?? "NONE").toUpperCase();
  const signal: TradeDirection = signalRaw === "BUY" ? "BUY" : signalRaw === "SELL" ? "SELL" : "NONE";

  return {
    pair: raw.pair ?? "N/A",
    trend: raw.trend ?? "N/A",
    signal,
    confidence: typeof raw.confidence === "number" ? raw.confidence : 0,
    entry: raw.entry ?? "-",
    stopLoss: raw.stop_loss ?? "-",
    takeProfit: raw.take_profit ?? "-",
    riskReward: raw.risk_reward ?? "-",
    reasoning: raw.reasoning ?? "-"
  };
}
