package com.tradepilot.domain.browser

/**
 * URL default/home yang dimuat pertama kali oleh BrowserEngine manapun
 * (android-client WebViewBrowserEngine maupun desktop-client
 * JCEFBrowserEngine). Ini CUMA starting point/quick-link, BUKAN whitelist --
 * BrowserBar.kt sudah punya address bar bebas ketik URL apa saja.
 */
const val EXNESS_WEBTRADING_URL = "https://my.exness.com/webtrading"
