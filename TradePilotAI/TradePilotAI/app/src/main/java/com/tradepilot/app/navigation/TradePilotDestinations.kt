package com.tradepilot.app.navigation

sealed class TradePilotDestination(val route: String, val label: String) {
    data object Browser : TradePilotDestination("browser", "Browser")
    data object Analysis : TradePilotDestination("analysis", "Analisa AI")
    data object Journal : TradePilotDestination("journal", "Jurnal")
    data object AddTrade : TradePilotDestination("add_trade", "Catat Trade")
    data object Statistic : TradePilotDestination("statistic", "Statistik")
    data object Notification : TradePilotDestination("notification", "Notifikasi")
    data object MoneyManagement : TradePilotDestination("money_management", "Risk")
    data object Settings : TradePilotDestination("settings", "Pengaturan")

    companion object {
        val activityBarItems = listOf(Browser, Analysis, MoneyManagement, Journal, Statistic, Notification, Settings)
    }
}
