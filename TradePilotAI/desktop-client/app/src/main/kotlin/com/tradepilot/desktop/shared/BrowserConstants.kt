package com.tradepilot.domain.browser

/**
 * URL default/home yang dimuat pertama kali oleh BrowserEngine manapun
 * (android-client WebViewBrowserEngine maupun desktop-client
 * JCEFBrowserEngine). Ini CUMA starting point/quick-link, BUKAN whitelist --
 * BrowserBar.kt sudah punya address bar bebas ketik URL apa saja.
 *
 * TODO (FASE 3/4, dicatat dari sesi audit FASE 1 -- BUKAN dikerjakan
 * sekarang, di luar scope "Browser Engine" murni): permintaan eksplisit
 * user adalah tab baru/pertama kali buka HARUSNYA menampilkan New Tab Page
 * bergaya Google (search bar + grid speed-dial shortcut custom: youtube,
 * coinmarketcap, github, dst -- lihat
 * "D:\rule tradepilot ai claude\referensi menu chrome (New Tab Page + Browser Menu).txt"
 * untuk detail & screenshot acuan), BUKAN langsung memuat EXNESS_WEBTRADING_URL
 * seperti sekarang. Ini butuh New Tab Page Composable/halaman lokal baru
 * (fitur UI, bukan sekadar ganti string), jadi sengaja ditunda ke FASE yang
 * memang menjadwalkan UI (FASE 3 Browser Feature / FASE 4 UI Polish).
 */
const val EXNESS_WEBTRADING_URL = "https://my.exness.com/webtrading"
