package com.tradepilot.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateRiskUseCaseTest {

    private val calculateRiskUseCase = CalculateRiskUseCase()

    @Test
    fun calculateRisk_returnsCorrectLotAndRR() {
        val result = calculateRiskUseCase(
            balance = 10000.0,
            riskPercent = 1.0,
            entryPrice = 1.1000,
            stopLossPrice = 1.0950,
            takeProfitPrice = 1.1100
        )

        // Risk $100 on 50 pips SL ($10/pip per lot) -> 0.20 lot
        assertEquals(1.0, result.riskPercent, 0.001)
        assertEquals(0.20, result.lot, 0.01)
        assertEquals(2.0, result.riskRewardRatio, 0.01)
        assertEquals(300.0, result.maxDailyLoss, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun calculateRisk_throwsOnNegativeBalance() {
        calculateRiskUseCase(
            balance = -100.0,
            riskPercent = 1.0,
            entryPrice = 1.1000,
            stopLossPrice = 1.0950,
            takeProfitPrice = 1.1100
        )
    }
}
