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
 * Prioritas 3 -> FASE 3 (Download Manager): SEBELUMNYA paket file ini
 * secara SENGAJA tidak menyertakan CefDownloadHandler (dicatat jujur di
 * sini) -- sekarang sudah disambungkan (lihat JCEFBrowserEngine.kt +
 * DownloadStore.kt). `id` dipakai untuk mencocokkan update progress dari
 * `CefDownloadItem.getId()` ke entry yang tepat di [DownloadStore].
 */
data class DownloadEntry(
    val id: Int,
    val fileName: String,
    val url: String,
    val fullPath: String,
    val progressPercent: Int,
    val isComplete: Boolean,
    val isCanceled: Boolean = false
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
