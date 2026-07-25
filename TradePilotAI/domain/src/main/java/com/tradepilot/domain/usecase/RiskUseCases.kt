package com.tradepilot.domain.usecase

import com.tradepilot.domain.model.RiskRecommendation
import javax.inject.Inject
import kotlin.math.abs

/**
 * Kalkulasi Money Management (versi 2) memakai rumus trading standar:
 *
 *  riskAmount   = balance * (riskPercent / 100)
 *  slDistance   = |entryPrice - stopLossPrice|
 *  lot          = riskAmount / (slDistance / pipValue * pipCost)
 *
 * Disederhanakan di sini dengan pendekatan "risk per pip" umum untuk pair
 * mayor forex (pipValuePerLotUsd dapat dikonfigurasi per instrumen di masa depan).
 */
class CalculateRiskUseCase @Inject constructor() {

    operator fun invoke(
        balance: Double,
        riskPercent: Double,
        entryPrice: Double,
        stopLossPrice: Double,
        takeProfitPrice: Double,
        pipValuePerLotUsd: Double = 10.0, // default untuk pair mayor XXX/USD standard lot
        pipSize: Double = 0.0001
    ): RiskRecommendation {
        require(balance > 0) { "Balance harus lebih besar dari 0" }
        require(riskPercent > 0) { "Risk percent harus lebih besar dari 0" }

        val riskAmount = balance * (riskPercent / 100.0)
        val slDistancePips = abs(entryPrice - stopLossPrice) / pipSize
        val tpDistancePips = abs(takeProfitPrice - entryPrice) / pipSize

        val lot = if (slDistancePips > 0) {
            riskAmount / (slDistancePips * pipValuePerLotUsd)
        } else 0.0

        val rr = if (slDistancePips > 0) tpDistancePips / slDistancePips else 0.0

        return RiskRecommendation(
            riskPercent = riskPercent,
            lot = roundTo(lot, 2),
            stopLoss = stopLossPrice,
            takeProfit = takeProfitPrice,
            riskRewardRatio = roundTo(rr, 2),
            maxDailyLoss = roundTo(riskAmount * 3, 2), // default: maks 3x risk per hari, dapat dikonfigurasi
            maxTrade = 3 // default, dapat dikonfigurasi di Settings
        )
    }

    private fun roundTo(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return Math.round(value * factor) / factor
    }
}
