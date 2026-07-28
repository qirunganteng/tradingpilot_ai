package com.tradepilot.desktop.browser

import com.tradepilot.domain.browser.BrowserEngine
import org.cef.CefClient
import org.cef.browser.CefBrowser
import java.awt.Component

/**
 * Implementasi Desktop dari [BrowserEngine] — bungkus [CefBrowser] (JCEF),
 * pasangan dari `WebViewBrowserEngine` di android-client yang membungkus
 * `android.webkit.WebView`. Business logic yang sudah dipakai (BrowserToolbar
 * dkk kalau nanti di-share ke desktop) tidak perlu tahu bedanya.
 */
class JCEFBrowserEngine(
    cefClient: CefClient,
    startUrl: String
) : BrowserEngine {

    /** Komponen AWT untuk di-embed lewat `SwingPanel` di Compose Desktop. */
    val browser: CefBrowser = cefClient.createBrowser(startUrl, false, false)
    val uiComponent: Component get() = browser.uiComponent

    override val currentUrl: String
        get() = browser.url ?: ""

    override fun loadUrl(url: String) {
        browser.loadURL(url)
    }

    override fun goBack() {
        if (browser.canGoBack()) browser.goBack()
    }

    override fun goForward() {
        if (browser.canGoForward()) browser.goForward()
    }

    override fun reload() {
        browser.reload()
    }

    override fun canGoBack(): Boolean = browser.canGoBack()

    override fun canGoForward(): Boolean = browser.canGoForward()

    /** Panggil saat Composable-nya dibuang dari komposisi (lihat Main.kt). */
    fun dispose() {
        browser.close(true)
    }
}
