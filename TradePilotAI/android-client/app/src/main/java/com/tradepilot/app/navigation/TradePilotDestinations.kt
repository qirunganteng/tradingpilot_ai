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
        // PENTING: pakai "by lazy", BUKAN "val" biasa. Kalau "val" biasa,
        // list ini dihitung saat companion object di-inisialisasi oleh JVM,
        // yang urutannya BISA terjadi SEBELUM semua "data object" saudaranya
        // (Browser, Analysis, dst) selesai dibuat -- menghasilkan null di
        // dalam list ini secara diam-diam, lalu crash NullPointerException
        // saat list itu dipakai (persis kejadian di ActivityBar.kt).
        val activityBarItems: List<TradePilotDestination> by lazy {
            listOf(Browser, Analysis, MoneyManagement, Journal, Statistic, Notification, Settings)
        }
    }
}