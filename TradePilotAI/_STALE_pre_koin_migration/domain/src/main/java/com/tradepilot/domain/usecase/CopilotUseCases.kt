package com.tradepilot.domain.usecase

import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeDirection

/**
 * AI Copilot (versi 4): dari satu AnalysisResult, turunkan pesan notifikasi
 * singkat ("EURUSD sedang mendekati Order Block", dst) berdasarkan keyword
 * yang muncul di reasoning/method — heuristik ringan, bukan NLP kompleks.
 */
object DeriveCopilotSignalUseCase {

    private val keywordCategories = listOf(
        "Order Block" to "OrderBlock",
        "Liquidity" to "Liquidity",
        "BOS" to "BOS",
        "CHOCH" to "CHOCH",
        "Fair Value Gap" to "FVG",
        "FVG" to "FVG",
        "PDH" to "PDH",
        "PDL" to "PDL"
    )

    fun invoke(result: AnalysisResult): List<Pair<String, String>> {
        // Pair<category, message>
        val text = result.reasoning
        val hits = keywordCategories.filter { (kw, _) -> text.contains(kw, ignoreCase = true) }

        if (hits.isEmpty() && result.signal != TradeDirection.NONE) {
            return listOf("Signal" to "${result.pair}: sinyal ${result.signal} terdeteksi (confidence ${result.confidence}%)")
        }

        return hits.map { (kw, category) ->
            category to "${result.pair}: terdeteksi $kw — ${result.trend}"
        }
    }
}
