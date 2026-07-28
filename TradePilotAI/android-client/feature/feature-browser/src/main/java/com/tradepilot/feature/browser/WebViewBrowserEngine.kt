package com.tradepilot.feature.browser

import android.webkit.WebView
import com.tradepilot.domain.browser.BrowserEngine

/**
 * Implementasi Android dari [BrowserEngine] — bungkus [WebView] yang sudah
 * dibuat oleh [ExnessWebView] (lihat `onWebViewReady`). Sengaja bukan
 * dependency Koin (`single`/`factory`): satu instance WebView cuma hidup
 * selama Composable ExnessWebView ada di layar, jadi dibuat manual di
 * [BrowserScreen] saat WebView-nya siap, bukan didaftarkan sebagai
 * singleton aplikasi (beda dari [com.tradepilot.app.webview.CurrentWebViewHolder]
 * yang memang sengaja singleton untuk kebutuhan capture screenshot lintas-modul).
 */
class WebViewBrowserEngine(private val webView: WebView) : BrowserEngine {

    override val currentUrl: String
        get() = webView.url ?: ""

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun goBack() {
        if (webView.canGoBack()) webView.goBack()
    }

    override fun goForward() {
        if (webView.canGoForward()) webView.goForward()
    }

    override fun reload() {
        webView.reload()
    }

    override fun canGoBack(): Boolean = webView.canGoBack()

    override fun canGoForward(): Boolean = webView.canGoForward()
}
