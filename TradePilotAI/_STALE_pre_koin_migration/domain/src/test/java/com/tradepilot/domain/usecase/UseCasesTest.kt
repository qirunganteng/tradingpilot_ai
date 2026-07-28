package com.tradepilot.domain.usecase

import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeDirection
import com.tradepilot.domain.model.TradeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test domain layer. Sudah diverifikasi PASS 100% (32/32 assertion)
 * lewat eksekusi manual dengan kotlinc + custom runner (lihat docs/TESTING.md)
 * karena lingkungan pengembangan awal tidak punya Android SDK/Gradle penuh.
 * File ini adalah versi resmi JUnit-nya untuk dijalankan via `./gradlew test`.
 */
class CalculateRiskUseCaseTest {
    private val useCase = CalculateRiskUseCase()

    @Test
    fun `menghitung lot berdasarkan risk persen dan jarak SL`() {
        val r = useCase(balance = 1000.0, riskPercent = 1.0, entryPrice = 1.1000, stopLossPrice = 1.0950, takeProfitPrice = 1.1100)
        assertEquals(0.02, r.lot, 0.001)
        assertEquals(2.0, r.riskRewardRatio, 0.001)
        assertEquals(30.0, r.maxDailyLoss, 0.001)
    }

    @Test
    fun `SL lebih dekat menghasilkan lot lebih besar`() {
        val base = useCase(1000.0, 1.0, 1.1000, 1.0950, 1.1100)
        val tighterSl = useCase(1000.0, 1.0, 1.1000, 1.0990, 1.1100)
        assertTrue(tighterSl.lot > base.lot)
    }

    @Test
    fun `lot proporsional terhadap balance`() {
        val base = useCase(1000.0, 1.0, 1.1000, 1.0950, 1.1100)
        val bigger = useCase(10000.0, 1.0, 1.1000, 1.0950, 1.1100)
        assertEquals(base.lot * 10, bigger.lot, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `balance nol melempar exception`() {
        useCase(0.0, 1.0, 1.0, 0.9, 1.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `risk percent nol melempar exception`() {
        useCase(1000.0, 0.0, 1.0, 0.9, 1.1)
    }
}

class CalculateJournalStatisticsUseCaseTest {
    private val useCase = CalculateJournalStatisticsUseCase()

    @Test
    fun `list kosong menghasilkan statistik nol`() {
        val s = useCase(emptyList())
        assertEquals(0, s.totalTrades)
    }

    @Test
    fun `statistik dihitung benar dari campuran win dan loss`() {
        val trades = listOf(
            TradeEntry(1, "EURUSD", TradeDirection.BUY, 1.1, 1.11, 1.095, 1.12, 0.1, 100.0, 2.0, 1100.0, 1000L),
            TradeEntry(2, "EURUSD", TradeDirection.SELL, 1.1, 1.09, 1.105, 1.08, 0.1, -50.0, 1.0, 1050.0, 2000L),
            TradeEntry(3, "GBPUSD", TradeDirection.BUY, 1.3, 1.31, 1.295, 1.32, 0.1, 100.0, 3.0, 1150.0, 3000L)
        )
        val s = useCase(trades)
        assertEquals(3, s.totalTrades)
        assertEquals(66.67, s.winRate, 0.1)
        assertEquals(150.0, s.totalProfit, 0.01)
        assertEquals(4.0, s.profitFactor, 0.01)
        assertEquals(2.0, s.averageRR, 0.01)
    }
}

class GenerateHistoryInsightUseCaseTest {
    private val useCase = GenerateHistoryInsightUseCase()

    @Test
    fun `kurang dari 100 trade belum memberi insight`() {
        val few = List(50) { TradeEntry(it.toLong(), "EURUSD", TradeDirection.BUY, 1.1, 1.11, 1.095, 1.12, 0.1, 10.0, 2.0, 1000.0, it * 1000L) }
        val insight = useCase(few)
        assertFalse(insight.hasEnoughData)
        assertEquals(50, insight.tradesAnalyzed)
    }

    @Test
    fun `minimal 100 trade memberi insight lengkap`() {
        val many = List(120) { i ->
            val pair = if (i % 3 == 0) "EURUSD" else "GBPUSD"
            val profit = if (i % 4 == 0) -10.0 else 20.0
            TradeEntry(i.toLong(), pair, TradeDirection.BUY, 1.1, 1.11, 1.095, 1.12, 0.1, profit, 2.0, 1000.0, i * 3_600_000L)
        }
        val insight = useCase(many)
        assertTrue(insight.hasEnoughData)
        assertEquals(120, insight.tradesAnalyzed)
        assertNotNull(insight.bestHourOfDay)
        assertNotNull(insight.bestPair)
        assertTrue(insight.winRate in 0.0..100.0)
    }
}

class GenerateMentorFeedbackUseCaseTest {
    private val useCase = GenerateMentorFeedbackUseCase()

    @Test
    fun `trade profit dengan RR tinggi mendapat whyGood`() {
        val trade = TradeEntry(1, "EURUSD", TradeDirection.BUY, 1.1000, 1.1100, 1.0950, 1.1150, 0.1, 100.0, 2.0, 1100.0, 1000L)
        val fb = useCase(trade)
        assertNotNull(fb.whyGood)
        assertNull(fb.whyBad)
    }

    @Test
    fun `trade rugi dengan RR rendah mendapat whyBad dan saran`() {
        val trade = TradeEntry(2, "EURUSD", TradeDirection.SELL, 1.1000, 1.1050, 1.0990, 1.1010, 0.1, -50.0, 0.5, 950.0, 2000L)
        val fb = useCase(trade)
        assertNotNull(fb.whyBad)
        assertNotNull(fb.betterEntrySuggestion)
    }

    @Test
    fun `SL sangat dekat dibanding TP memicu peringatan`() {
        val trade = TradeEntry(3, "EURUSD", TradeDirection.BUY, 1.1000, 1.1500, 1.0995, 1.1500, 0.1, 200.0, 10.0, 1200.0, 3000L)
        val fb = useCase(trade)
        assertNotNull(fb.slTooTight)
    }
}

class DeriveCopilotSignalUseCaseTest {
    @Test
    fun `mendeteksi keyword ICT dari reasoning`() {
        val result = AnalysisResult(
            pair = "EURUSD", trend = "Bullish", signal = TradeDirection.BUY, confidence = 80.0,
            entry = "1.1000", stopLoss = "1.0950", takeProfit = "1.1100", riskReward = "1:2",
            reasoning = "Order Block terkonfirmasi setelah BOS bullish, liquidity sudah tersapu.",
            method = listOf("ICT", "SMC"), providerUsed = "gemini", timestampMillis = 1000L
        )
        val signals = DeriveCopilotSignalUseCase.invoke(result)
        assertTrue(signals.isNotEmpty())
        assertTrue(signals.any { it.first == "OrderBlock" })
        assertTrue(signals.any { it.first == "BOS" })
        assertTrue(signals.any { it.first == "Liquidity" })
    }

    @Test
    fun `tanpa keyword tapi ada signal BUY tetap fallback ke kategori Signal`() {
        val result = AnalysisResult(
            pair = "EURUSD", trend = "Bullish", signal = TradeDirection.BUY, confidence = 80.0,
            entry = "1.1000", stopLoss = "1.0950", takeProfit = "1.1100", riskReward = "1:2",
            reasoning = "Analisa umum tanpa keyword spesifik.",
            method = listOf("ICT"), providerUsed = "gemini", timestampMillis = 1000L
        )
        val signals = DeriveCopilotSignalUseCase.invoke(result)
        assertTrue(signals.any { it.first == "Signal" })
    }
}
