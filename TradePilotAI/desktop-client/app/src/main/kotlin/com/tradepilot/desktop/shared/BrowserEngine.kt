package com.tradepilot.domain.browser

/**
 * Abstraksi Browser Engine (Konstitusi bagian "BROWSER ENGINE"):
 * Business Logic tidak boleh tahu apakah dia jalan di atas Android WebView
 * atau JCEF (Chromium Embedded Framework, dipakai desktop-client).
 *
 * Implementasi konkret ada di masing-masing Platform Client:
 *  - android-client -> WebViewBrowserEngine (feature-browser)
 *  - desktop-client  -> JCEFBrowserEngine (browser/JCEFBrowserEngine.kt)
 *
 * PENTING kalau kamu mau ubah UI browser: interface ini yang dipakai
 * BrowserBar.kt & Main.kt untuk komunikasi ke browser. Kalau nambah method
 * baru di sini, HARUS diimplementasikan juga di JCEFBrowserEngine.kt
 * (desktop) dan WebViewBrowserEngine.kt (android, tidak disertakan di paket
 * ini karena kamu minta fokus desktop-client).
 *
 * PERUBAHAN vs versi lama (untuk Prioritas 10 -- Ctrl+F Find in page):
 *   + find(text, forward)
 *   + stopFind()
 * Kalau kamu TIDAK mau implementasi find-in-page di android-client dulu,
 * boleh kasih default implementation kosong di WebViewBrowserEngine supaya
 * tidak wajib langsung dikerjakan sekarang -- yang penting compile jalan.
 */
interface BrowserEngine {
    /** URL yang sedang aktif. Kosong (\"\") kalau belum ada halaman yang dimuat. */
    val currentUrl: String

    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
    fun reload()
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean

    /** Cari teks di halaman yang sedang aktif (Ctrl+F). */
    fun find(text: String, forward: Boolean = true) {}

    /** Tutup search bar / bersihkan highlight hasil find(). */
    fun stopFind() {}
}
