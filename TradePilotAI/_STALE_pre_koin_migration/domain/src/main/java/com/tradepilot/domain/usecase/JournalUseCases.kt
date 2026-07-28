package com.tradepilot.domain.usecase

import com.tradepilot.domain.model.TradeEntry
import com.tradepilot.domain.repository.TradeJournalRepository
import javax.inject.Inject

class SaveTradeEntryUseCase @Inject constructor(
    private val repository: TradeJournalRepository
) {
    suspend operator fun invoke(entry: TradeEntry) = repository.save(entry)
}

class ObserveTradeHistoryUseCase @Inject constructor(
    private val repository: TradeJournalRepository
) {
    operator fun invoke() = repository.observeHistory()
}

data class JournalStatistics(
    val totalTrades: Int,
    val winRate: Double,       // persen
    val profitFactor: Double,  // total profit / total loss (absolut)
    val averageRR: Double,
    val totalProfit: Double
)

/**
 * Hitung Winrate, Profit Factor, Average RR, Total Profit (versi 3).
 * Pure function di domain layer — gampang di-unit-test tanpa Android.
 */
class CalculateJournalStatisticsUseCase @Inject constructor() {

    operator fun invoke(trades: List<TradeEntry>): JournalStatistics {
        if (trades.isEmpty()) {
            return JournalStatistics(0, 0.0, 0.0, 0.0, 0.0)
        }

        val wins = trades.filter { it.profitLoss > 0 }
        val losses = trades.filter { it.profitLoss < 0 }

        val totalProfit = wins.sumOf { it.profitLoss }
        val totalLossAbs = losses.sumOf { kotlin.math.abs(it.profitLoss) }

        val winRate = (wins.size.toDouble() / trades.size) * 100.0
        val profitFactor = if (totalLossAbs > 0) totalProfit / totalLossAbs else totalProfit
        val averageRR = trades.map { it.riskRewardRatio }.average()

        return JournalStatistics(
            totalTrades = trades.size,
            winRate = round2(winRate),
            profitFactor = round2(profitFactor),
            averageRR = round2(averageRR),
            totalProfit = round2(trades.sumOf { it.profitLoss })
        )
    }

    private fun round2(v: Double) = Math.round(v * 100) / 100.0
}
