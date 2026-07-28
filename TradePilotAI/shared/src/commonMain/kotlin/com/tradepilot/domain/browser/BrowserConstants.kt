package com.tradepilot.domain.browser

/**
 * Satu-satunya URL yang boleh dibuka BrowserEngine manapun (android-client
 * WebViewBrowserEngine maupun desktop-client JCEFBrowserEngine). Dipusatkan
 * di sini (dulu: duplikat const val di ExnessWebView.kt milik android-client
 * saja) supaya kalau URL berubah, cukup 1 tempat, dan desktop-client tidak
 * perlu menebak/duplikasi nilainya.
 */
const val EXNESS_WEBTRADING_URL = "https://my.exness.com/webtrading"
