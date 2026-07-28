package com.tradepilot.domain.browser

/**
 * Abstraksi Browser Engine (Konstitusi bagian "BROWSER ENGINE"):
 * Business Logic tidak boleh tahu apakah dia jalan di atas Android WebView
 * atau JCEF (Chromium Embedded Framework, dipakai desktop-client).
 *
 * Implementasi konkret ada di masing-masing Platform Client:
 *  - android-client -> WebViewBrowserEngine (feature-browser, bungkus
 *    android.webkit.WebView milik ExnessWebView yang sudah ada)
 *  - desktop-client  -> JCEFBrowserEngine (Fase 5, belum diimplementasikan)
 *
 * CATATAN: pengambilan screenshot chart TIDAK ada di sini — itu sudah
 * diabstraksi lebih dulu lewat `ChartSnapshotProvider` (lihat
 * domain/repository/ChartSnapshotProvider.kt), jangan dobel-abstraksi.
 * BrowserEngine ini fokus murni ke navigasi (url/back/forward/reload).
 */
interface BrowserEngine {
    /** URL yang sedang aktif. Kosong ("") kalau belum ada halaman yang dimuat. */
    val currentUrl: String

    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
    fun reload()
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean
}
