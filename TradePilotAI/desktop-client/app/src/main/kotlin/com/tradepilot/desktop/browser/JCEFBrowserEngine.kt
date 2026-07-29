package com.tradepilot.desktop.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tradepilot.domain.browser.BrowserEngine
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.Component

/**
 * Implementasi Desktop dari [BrowserEngine] — bungkus [CefBrowser] (JCEF),
 * pasangan dari `WebViewBrowserEngine` di android-client yang membungkus
 * `android.webkit.WebView`. Business logic yang sudah dipakai (BrowserToolbar
 * dkk kalau nanti di-share ke desktop) tidak perlu tahu bedanya.
 *
 * PENTING (browser umum, bukan cuma Exness/TradingView): engine ini TIDAK
 * membatasi domain apa pun -- `loadUrl` menerima URL apa saja (youtube.com,
 * github.com, shopee.co.id, facebook.com, dst) persis seperti browser biasa.
 * Yang tadinya terasa "cuma bisa ke Exness" murni karena UI (BrowserBar)
 * belum ada address bar -- sudah ditambah di Main.kt.
 *
 * Field *State di bawah reaktif ke Compose (dipakai BrowserBar) lewat
 * CefLoadHandler (canGoBack/canGoForward/isLoading -- lebih akurat dari
 * browser.canGoBack() yang bisa "telat" 1 frame) dan CefDisplayHandler
 * (address & title berubah, termasuk saat user klik link DI DALAM halaman,
 * bukan cuma lewat address bar kita).
 */
class JCEFBrowserEngine(
    cefClient: CefClient,
    startUrl: String
) : BrowserEngine {

    /** Komponen AWT untuk di-embed lewat `SwingPanel` di Compose Desktop. */
    val browser: CefBrowser = cefClient.createBrowser(startUrl, false, false)
    val uiComponent: Component get() = browser.uiComponent

    var addressState by mutableStateOf(startUrl)
        private set
    var titleState by mutableStateOf("")
        private set
    var isLoadingState by mutableStateOf(false)
        private set
    var canGoBackState by mutableStateOf(false)
        private set
    var canGoForwardState by mutableStateOf(false)
        private set

    init {
        cefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onAddressChange(browser: CefBrowser?, frame: CefFrame?, url: String?) {
                if (browser === this@JCEFBrowserEngine.browser && url != null) {
                    addressState = url
                }
            }

            override fun onTitleChange(browser: CefBrowser?, title: String?) {
                if (browser === this@JCEFBrowserEngine.browser && title != null) {
                    titleState = title
                }
            }
        })

        cefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                if (browser === this@JCEFBrowserEngine.browser) {
                    isLoadingState = isLoading
                    canGoBackState = canGoBack
                    canGoForwardState = canGoForward
                }
            }
        })
    }

    override val currentUrl: String
        get() = browser.url ?: ""

    override fun loadUrl(url: String) {
        browser.loadURL(normalizeUrl(url))
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

    companion object {
        /**
         * Address bar browser biasa menerima ketikan tanpa skema ("youtube.com",
         * "github.com/qirunganteng") -- tanpa ini JCEF akan menganggapnya query
         * pencarian aneh atau gagal load. Kalau sudah ada skema (http/https/
         * file/data/about, dst) dibiarkan apa adanya.
         */
        fun normalizeUrl(input: String): String {
            val trimmed = input.trim()
            val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed) ||
                trimmed.startsWith("about:") || trimmed.startsWith("data:")
            return if (hasScheme) trimmed else "https://$trimmed"
        }
    }
}
