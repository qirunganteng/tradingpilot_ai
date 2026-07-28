const DEFAULT_METHODS = [
  "ICT", "SMC", "Liquidity", "Order Block", "Fair Value Gap",
  "BOS", "CHOCH", "Session", "PDH", "PDL", "Trend", "Momentum"
];

export function buildChartAnalysisPrompt(methods: string[] = []): string {
  const methodList = methods.length > 0 ? methods : DEFAULT_METHODS;

  return `Kamu adalah asisten analisa chart trading forex profesional.
Analisa screenshot chart yang dilampirkan menggunakan metode berikut: ${methodList.join(", ")}.

PENTING: kamu HANYA memberikan analisa dan rekomendasi. Kamu TIDAK melakukan transaksi apapun.

Kembalikan jawaban HANYA dalam format JSON valid, tanpa teks tambahan di luar JSON, dengan struktur persis:
{
  "pair": "string, contoh EURUSD",
  "trend": "string, contoh Bullish/Bearish/Ranging",
  "signal": "BUY atau SELL atau NONE",
  "confidence": angka 0-100,
  "entry": "string harga atau area",
  "stop_loss": "string harga",
  "take_profit": "string harga",
  "risk_reward": "string, contoh 1:2",
  "reasoning": "string, penjelasan singkat alasan analisa"
}`;
}

export { DEFAULT_METHODS };
