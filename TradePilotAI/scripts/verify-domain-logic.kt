import com.tradepilot.domain.model.TradeDirection
import com.tradepilot.domain.model.TradeEntry
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.usecase.CalculateRiskUseCase
import com.tradepilot.domain.usecase.CalculateJournalStatisticsUseCase
import com.tradepilot.domain.usecase.GenerateHistoryInsightUseCase
import com.tradepilot.domain.usecase.GenerateMentorFeedbackUseCase
import com.tradepilot.domain.usecase.DeriveCopilotSignalUseCase

var passed = 0
var failed = 0

fun check(label: String, cond: Boolean) {
    if (cond) { println("  PASS: $label"); passed++ }
    else { println("  FAIL: $label"); failed++ }
}

fun approxEq(a: Double, b: Double, eps: Double = 0.01) = Math.abs(a - b) < eps

fun testCalculateRisk() {
    println("\n[CalculateRiskUseCase]")
    val useCase = CalculateRiskUseCase()

    // Kasus umum: balance 1000, risk 1%, entry 1.1000, SL 1.0950 (50 pips), TP 1.1100 (100 pips)
    val r1 = useCase(
        balance = 1000.0, riskPercent = 1.0,
        entryPrice = 1.1000, stopLossPrice = 1.0950, takeProfitPrice = 1.1100
    )
    // riskAmount = 10, slDistancePips = 50, lot = 10 / (50*10) = 0.02
    check("riskPercent tersimpan benar", r1.riskPercent == 1.0)
    check("lot dihitung benar (0.02)", approxEq(r1.lot, 0.02))
    check("RR dihitung benar (100/50=2.0)", approxEq(r1.riskRewardRatio, 2.0))
    check("maxDailyLoss = 3x riskAmount (30)", approxEq(r1.maxDailyLoss, 30.0))
    check("maxTrade default 3", r1.maxTrade == 3)

    // Kasus SL sangat dekat -> lot lebih besar
    val r2 = useCase(
        balance = 1000.0, riskPercent = 1.0,
        entryPrice = 1.1000, stopLossPrice = 1.0990, takeProfitPrice = 1.1100 // SL 10 pips
    )
    check("SL lebih dekat -> lot lebih besar dari r1", r2.lot > r1.lot)

    // Kasus balance lebih besar -> lot proporsional lebih besar
    val r3 = useCase(
        balance = 10000.0, riskPercent = 1.0,
        entryPrice = 1.1000, stopLossPrice = 1.0950, takeProfitPrice = 1.1100
    )
    check("balance 10x -> lot 10x lebih besar", approxEq(r3.lot, r1.lot * 10))

    // Validasi input tidak valid
    var threw = false
    try { useCase(balance = 0.0, riskPercent = 1.0, entryPrice = 1.0, stopLossPrice = 0.9, takeProfitPrice = 1.1) }
    catch (e: IllegalArgumentException) { threw = true }
    check("balance <= 0 -> throw IllegalArgumentException", threw)

    threw = false
    try { useCase(balance = 1000.0, riskPercent = 0.0, entryPrice = 1.0, stopLossPrice = 0.9, takeProfitPrice = 1.1) }
    catch (e: IllegalArgumentException) { threw = true }
    check("riskPercent <= 0 -> throw IllegalArgumentException", threw)
}

fun testJournalStatistics() {
    println("\n[CalculateJournalStatisticsUseCase]")
    val useCase = CalculateJournalStatisticsUseCase()

    check("list kosong -> semua 0", useCase(emptyList()).totalTrades == 0)

    val trades = listOf(
        TradeEntry(1, "EURUSD", TradeDirection.BUY, 1.1, 1.11, 1.095, 1.12, 0.1, 100.0, 2.0, 1100.0, 1000L),
        TradeEntry(2, "EURUSD", TradeDirection.SELL, 1.1, 1.09, 1.105, 1.08, 0.1, -50.0, 1.0, 1050.0, 2000L),
        TradeEntry(3, "GBPUSD", TradeDirection.BUY, 1.3, 1.31, 1.295, 1.32, 0.1, 100.0, 3.0, 1150.0, 3000L)
    )
    val stats = useCase(trades)
    check("totalTrades = 3", stats.totalTrades == 3)
    check("winRate = 66.67% (2 dari 3)", approxEq(stats.winRate, 66.67, 0.1))
    check("totalProfit = 150 (100-50+100)", approxEq(stats.totalProfit, 150.0))
    check("profitFactor = 200/50 = 4.0", approxEq(stats.profitFactor, 4.0))
    check("averageRR = (2+1+3)/3 = 2.0", approxEq(stats.averageRR, 2.0))
}

fun testHistoryInsight() {
    println("\n[GenerateHistoryInsightUseCase]")
    val useCase = GenerateHistoryInsightUseCase()

    val fewTrades = List(50) {
        TradeEntry(it.toLong(), "EURUSD", TradeDirection.BUY, 1.1, 1.11, 1.095, 1.12, 0.1, 10.0, 2.0, 1000.0, it * 1000L)
    }
    check("< 100 trade -> hasEnoughData false", !useCase(fewTrades).hasEnoughData)
    check("< 100 trade -> tradesAnalyzed tetap dilaporkan (50)", useCase(fewTrades).tradesAnalyzed == 50)

    val manyTrades = List(120) { i ->
        val pair = if (i % 3 == 0) "EURUSD" else "GBPUSD"
        val profit = if (i % 4 == 0) -10.0 else 20.0 // GBPUSD & EURUSD campur win/loss
        TradeEntry(i.toLong(), pair, TradeDirection.BUY, 1.1, 1.11, 1.095, 1.12, 0.1, profit, 2.0, 1000.0, i * 3_600_000L)
    }
    val insight = useCase(manyTrades)
    check(">= 100 trade -> hasEnoughData true", insight.hasEnoughData)
    check("tradesAnalyzed = 120", insight.tradesAnalyzed == 120)
    check("bestHourOfDay terisi (0-23)", insight.bestHourOfDay != null && insight.bestHourOfDay!! in 0..23)
    check("bestPair terisi", insight.bestPair != null)
    check("winRate masuk akal (0-100)", insight.winRate in 0.0..100.0)
}

fun testMentorFeedback() {
    println("\n[GenerateMentorFeedbackUseCase]")
    val useCase = GenerateMentorFeedbackUseCase()

    val goodTrade = TradeEntry(1, "EURUSD", TradeDirection.BUY, 1.1000, 1.1100, 1.0950, 1.1150, 0.1, 100.0, 2.0, 1100.0, 1000L)
    val fb1 = useCase(goodTrade)
    check("trade profit + RR tinggi -> ada whyGood", fb1.whyGood != null)
    check("trade profit -> tidak ada whyBad", fb1.whyBad == null)

    val badTrade = TradeEntry(2, "EURUSD", TradeDirection.SELL, 1.1000, 1.1050, 1.0990, 1.1010, 0.1, -50.0, 0.5, 950.0, 2000L)
    val fb2 = useCase(badTrade)
    check("trade rugi + RR rendah -> ada whyBad", fb2.whyBad != null)
    check("trade rugi -> ada saran entry lebih baik", fb2.betterEntrySuggestion != null)

    val tightSlTrade = TradeEntry(3, "EURUSD", TradeDirection.BUY, 1.1000, 1.1500, 1.0995, 1.1500, 0.1, 200.0, 10.0, 1200.0, 3000L)
    val fb3 = useCase(tightSlTrade)
    check("SL sangat dekat dibanding TP -> ada peringatan slTooTight", fb3.slTooTight != null)
}

fun testCopilotSignal() {
    println("\n[DeriveCopilotSignalUseCase]")
    val result = AnalysisResult(
        pair = "EURUSD", trend = "Bullish", signal = TradeDirection.BUY, confidence = 80.0,
        entry = "1.1000", stopLoss = "1.0950", takeProfit = "1.1100", riskReward = "1:2",
        reasoning = "Order Block terkonfirmasi setelah BOS bullish, liquidity sudah tersapu.",
        method = listOf("ICT", "SMC"), providerUsed = "gemini", timestampMillis = 1000L
    )
    val signals = DeriveCopilotSignalUseCase.invoke(result)
    check("mendeteksi minimal 1 sinyal dari reasoning", signals.isNotEmpty())
    check("mendeteksi Order Block", signals.any { it.first == "OrderBlock" })
    check("mendeteksi BOS", signals.any { it.first == "BOS" })
    check("mendeteksi Liquidity", signals.any { it.first == "Liquidity" })

    val noKeywordResult = result.copy(reasoning = "Analisa umum tanpa keyword spesifik.")
    val signals2 = DeriveCopilotSignalUseCase.invoke(noKeywordResult)
    check("tanpa keyword tapi ada signal BUY -> fallback ke kategori Signal", signals2.any { it.first == "Signal" })
}

fun main() {
    testCalculateRisk()
    testJournalStatistics()
    testHistoryInsight()
    testMentorFeedback()
    testCopilotSignal()
    println("\n=== HASIL AKHIR: $passed PASS, $failed FAIL ===")
    if (failed > 0) kotlin.system.exitProcess(1)
}
