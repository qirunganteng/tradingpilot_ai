package com.tradepilot.core.common

/**
 * Event lintas-modul. feature-* TIDAK boleh saling bergantung langsung;
 * komunikasi antar modul (mis. AI Copilot mendeteksi Order Block lalu
 * Notification Center harus tahu) lewat EventBus berbasis SharedFlow ini.
 */
sealed class AppEvent {
    data class MarketSignalDetected(
        val pair: String,
        val category: String, // "OrderBlock" | "BOS" | "CHOCH" | "Liquidity" | dst
        val message: String
    ) : AppEvent()

    data class TradeJournalSaved(val tradeId: Long) : AppEvent()

    data class RiskLimitReached(val reason: String) : AppEvent()
}
