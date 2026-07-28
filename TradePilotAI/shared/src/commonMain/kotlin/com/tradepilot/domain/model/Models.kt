package com.tradepilot.domain.model

/** Model domain murni — tidak boleh mengandung anotasi Room/Retrofit. */

data class AccountInfo(
    val balance: Double,
    val equity: Double,
    val margin: Double,
    val freeMargin: Double,
    val leverage: Int,
    val openPositions: Int,
    val floatingProfit: Double,
    val floatingLoss: Double
)

data class RiskRecommendation(
    val riskPercent: Double,
    val lot: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val riskRewardRatio: Double,
    val maxDailyLoss: Double,
    val maxTrade: Int
)

enum class TradeDirection { BUY, SELL, NONE }

data class AnalysisResult(
    val pair: String,
    val trend: String,
    val signal: TradeDirection,
    val confidence: Double,
    val entry: String,
    val stopLoss: String,
    val takeProfit: String,
    val riskReward: String,
    val reasoning: String,
    val method: List<String>, // ICT, SMC, Liquidity, Order Block, FVG, BOS, CHOCH, dst
    val providerUsed: String,
    val timestampMillis: Long
)

data class TradeEntry(
    val id: Long = 0,
    val pair: String,
    val direction: TradeDirection,
    val entry: Double,
    val exit: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val lot: Double,
    val profitLoss: Double,
    val riskRewardRatio: Double,
    val balanceAfter: Double,
    val timestampMillis: Long,
    val notes: String = ""
)

data class MentorFeedback(
    val whyGood: String?,
    val whyBad: String?,
    val slTooTight: String?,
    val tpTooFar: String?,
    val betterEntrySuggestion: String?
)
