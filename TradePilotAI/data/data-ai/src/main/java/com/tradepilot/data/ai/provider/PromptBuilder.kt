package com.tradepilot.data.ai.provider

/**
 * Menyusun prompt terstruktur agar Gemini konsisten mengembalikan JSON
 * yang bisa di-parse (lihat AnalysisResponseMapper). Metode analisa
 * mengikuti daftar di Blueprint versi 1: ICT, SMC, Liquidity, Order Block,
 * FVG, BOS, CHOCH, Session, PDH, PDL, Trend, Momentum.
 */
object PromptBuilder {

    fun buildChartAnalysisPrompt(methods: List<String>): String {
        val methodList = if (methods.isEmpty()) {
            listOf("ICT", "SMC", "Liquidity", "Order Block", "Fair Value Gap", "BOS", "CHOCH", "Session", "PDH", "PDL", "Trend", "Momentum")
        } else methods

        return """
        Kamu adalah asisten analisa chart trading forex profesional.
        Analisa screenshot chart yang dilampirkan menggunakan metode berikut: ${methodList.joinToString(", ")}.

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
        }
        """.trimIndent()
    }
}
