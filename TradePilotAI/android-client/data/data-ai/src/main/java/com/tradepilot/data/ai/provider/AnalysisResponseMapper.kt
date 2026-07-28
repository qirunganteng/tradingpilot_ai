package com.tradepilot.data.ai.provider

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeDirection

/**
 * Parse teks JSON hasil Gemini (lihat format di PromptBuilder) menjadi
 * AnalysisResult domain model. Gemini kadang membungkus JSON dalam
 * code fence ```json ... ``` — mapper ini membersihkannya dulu.
 */
class AnalysisResponseMapper constructor(
    private val moshi: Moshi
) {
    private data class RawAnalysis(
        val pair: String? = null,
        val trend: String? = null,
        val signal: String? = null,
        val confidence: Double? = null,
        val entry: String? = null,
        val stop_loss: String? = null,
        val take_profit: String? = null,
        val risk_reward: String? = null,
        val reasoning: String? = null
    )

    fun map(rawText: String, providerName: String, methods: List<String>): Result<AnalysisResult> {
        return try {
            val cleaned = rawText
                .trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(RawAnalysis::class.java)
            val raw = adapter.fromJson(cleaned)
                ?: return Result.failure(IllegalStateException("Response Gemini kosong/tidak valid"))

            val signal = when (raw.signal?.uppercase()) {
                "BUY" -> TradeDirection.BUY
                "SELL" -> TradeDirection.SELL
                else -> TradeDirection.NONE
            }

            Result.success(
                AnalysisResult(
                    pair = raw.pair ?: "N/A",
                    trend = raw.trend ?: "N/A",
                    signal = signal,
                    confidence = raw.confidence ?: 0.0,
                    entry = raw.entry ?: "-",
                    stopLoss = raw.stop_loss ?: "-",
                    takeProfit = raw.take_profit ?: "-",
                    riskReward = raw.risk_reward ?: "-",
                    reasoning = raw.reasoning ?: "-",
                    method = methods,
                    providerUsed = providerName,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
