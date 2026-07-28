package com.tradepilot.domain.usecase

import com.tradepilot.domain.model.TradeEntry
import java.util.Calendar
import javax.inject.Inject

data class HistoryInsight(
    val hasEnoughData: Boolean,     // true jika >= MIN_TRADES_FOR_INSIGHT
    val tradesAnalyzed: Int,
    val bestHourOfDay: Int?,        // 0-23, jam dengan winrate tertinggi
    val bestPair: String?,
    val worstPair: String?,
    val winRate: Double,
    val lossRate: Double,
    val profitFactor: Double,
    val averageRR: Double
)

/**
 * Versi 6: "Setelah memiliki minimal 100 trade, AI harus memberikan
 * rekomendasi berdasarkan histori trader." Di bawah itu, hasIntEnoughData
 * = false dan UI menampilkan pesan "kumpulkan data dulu" alih-alih insight.
 */
class GenerateHistoryInsightUseCase @Inject constructor() {

    companion object {
        const val MIN_TRADES_FOR_INSIGHT = 100
    }

    operator fun invoke(trades: List<TradeEntry>): HistoryInsight {
        if (trades.size < MIN_TRADES_FOR_INSIGHT) {
            return HistoryInsight(
                hasEnoughData = false,
                tradesAnalyzed = trades.size,
                bestHourOfDay = null,
                bestPair = null,
                worstPair = null,
                winRate = 0.0,
                lossRate = 0.0,
                profitFactor = 0.0,
                averageRR = 0.0
            )
        }

        val byHour = trades.groupBy { hourOf(it.timestampMillis) }
        val bestHour = byHour.maxByOrNull { (_, list) -> winRate(list) }?.key

        val byPair = trades.groupBy { it.pair }
        val bestPair = byPair.maxByOrNull { (_, list) -> winRate(list) }?.key
        val worstPair = byPair.minByOrNull { (_, list) -> winRate(list) }?.key

        val wins = trades.count { it.profitLoss > 0 }
        val losses = trades.count { it.profitLoss < 0 }
        val totalProfit = trades.filter { it.profitLoss > 0 }.sumOf { it.profitLoss }
        val totalLossAbs = trades.filter { it.profitLoss < 0 }.sumOf { kotlin.math.abs(it.profitLoss) }

        return HistoryInsight(
            hasEnoughData = true,
            tradesAnalyzed = trades.size,
            bestHourOfDay = bestHour,
            bestPair = bestPair,
            worstPair = worstPair,
            winRate = round2(wins.toDouble() / trades.size * 100.0),
            lossRate = round2(losses.toDouble() / trades.size * 100.0),
            profitFactor = round2(if (totalLossAbs > 0) totalProfit / totalLossAbs else totalProfit),
            averageRR = round2(trades.map { it.riskRewardRatio }.average())
        )
    }

    private fun winRate(list: List<TradeEntry>): Double {
        if (list.isEmpty()) return 0.0
        return list.count { it.profitLoss > 0 }.toDouble() / list.size
    }

    private fun hourOf(timestampMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampMillis
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun round2(v: Double) = Math.round(v * 100) / 100.0
}
