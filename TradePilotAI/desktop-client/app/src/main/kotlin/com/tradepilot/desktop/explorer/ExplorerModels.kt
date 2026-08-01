package com.tradepilot.desktop.explorer

/**
 * Satu baris History (Prioritas 3). Dicatat otomatis setiap kali
 * JCEFBrowserEngine.addressState berubah -- lihat wiring-nya di Main.kt
 * (Workbench) yang mem-push ke [HistoryStore].
 */
data class HistoryEntry(
    val url: String,
    val title: String,
    val visitedAtEpochMillis: Long
)

/** Satu bookmark (Prioritas 3 & Prioritas 10 Ctrl+D). */
data class BookmarkEntry(
    val url: String,
    val title: String
)

/**
 * CATATAN JUJUR (Prioritas 3 - Downloads): paket file ini TIDAK menyertakan
 * implementasi CefDownloadHandler (butuh perubahan di JCEFBootstrap.kt /
 * JCEFBrowserEngine.kt untuk daftar ke CefClient.addDownloadHandler, plus
 * penyimpanan progress per-file). Model ini disiapkan supaya panel Downloads
 * SUDAH punya bentuk UI yang benar sekarang, dan tinggal disambungkan ke
 * data asli begitu handler-nya dibuat -- lihat catatan lebih lengkap di
 * Explorer/ExplorerPanel.kt bagian DownloadsList.
 */
data class DownloadEntry(
    val fileName: String,
    val url: String,
    val progressPercent: Int,
    val isComplete: Boolean
)

/** Shortcut situs yang dipin user supaya gampang diakses ulang (Prioritas 3). */
data class PinnedSite(
    val label: String,
    val url: String
)

/**
 * "Trading Sessions" (Prioritas 3): representasi ringan dari tab-tab browser
 * yang sedang aktif, dikelompokkan sebagai satu sesi kerja. Karena tidak ada
 * penyimpanan sesi persisten di paket file ini, ini murni derived dari
 * daftar BrowserTab yang sedang terbuka (lihat ExplorerPanel.kt).
 */
data class TradingSession(
    val name: String,
    val tabCount: Int
)
