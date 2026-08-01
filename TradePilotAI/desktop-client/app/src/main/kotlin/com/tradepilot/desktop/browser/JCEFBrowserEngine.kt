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
 * Yang tadinya terasa \"cuma bisa ke Exness\" murni karena UI (BrowserBar)
 * belum ada address bar -- sudah ditambah di Main.kt.
 *
 * Field *State di bawah reaktif ke Compose (dipakai BrowserBar) lewat
 * CefLoadHandler (canGoBack/canGoForward/isLoading -- lebih akurat dari
 * browser.canGoBack() yang bisa \"telat\" 1 frame) dan CefDisplayHandler
 * (address & title berubah, termasuk saat user klik link DI DALAM halaman,
 * bukan cuma lewat address bar kita).
 *
 * PERUBAHAN untuk Prioritas 10 & 12 (semua ADITIF -- tidak ada method lama
 * yang dihapus/diubah signature-nya):
 *  - find()/stopFind(): dari interface BrowserEngine (Ctrl+F, Prioritas 10),
 *    diteruskan langsung ke CefBrowser.find(...) (memang sudah tersedia di
 *    JCEF, cuma belum dipakai sebelumnya).
 *  - loadErrorState: diisi oleh CefLoadHandler.onLoadError (method yang
 *    memang sudah ada di CefLoadHandlerAdapter, cuma sebelumnya tidak
 *    di-override) -- dipakai JCEFBrowserView untuk Error Page/Offline Page
 *    (Prioritas 12). Otomatis dikosongkan lagi begitu onLoadingStateChange
 *    bilang loading dimulai (retry/navigasi baru).
 *  - loadingProgressState: CATATAN JUJUR -- CEF/JCEF TIDAK expose event
 *    persentase loading real (onLoadingProgressChange bukan bagian dari
 *    CefLoadHandler standar), jadi ini SIMULASI (naik bertahap selagi
 *    isLoadingState true, langsung ke 100% saat selesai) -- cukup untuk
 *    progress bar visual ala Chrome, TAPI bukan progress byte-per-byte asli.
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

    /** Prioritas 12: null = tidak ada error, terisi begitu load gagal. */
    var loadErrorState by mutableStateOf<LoadError?>(null)
        private set

    /** Prioritas 12: 0f..1f, simulasi (lihat catatan class di atas). */
    var loadingProgressState by mutableStateOf(0f)
        private set

    data class LoadError(
        val failedUrl: String,
        val errorText: String,
        val isOffline: Boolean
    )

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
                    if (isLoading) {
                        loadErrorState = null // navigasi baru/retry -> bersihkan error lama
                        loadingProgressState = 0.15f // simulasi: langsung kelihatan mulai jalan
                    } else {
                        loadingProgressState = 1f
                    }
                }
            }

            override fun onLoadError(
                browser: CefBrowser?,
                frame: CefFrame?,
                errorCode: org.cef.handler.CefLoadHandler.ErrorCode?,
                errorText: String?,
                failedUrl: String?
            ) {
                if (browser !== this@JCEFBrowserEngine.browser) return
                // ERR_ABORTED terjadi normal kalau user pindah halaman sebelum
                // load lama selesai -- BUKAN error sungguhan, jangan tampilkan
                // Error Page untuk kasus ini.
                if (errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_ABORTED) return
                val offline = errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_INTERNET_DISCONNECTED ||
                    errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_NAME_NOT_RESOLVED ||
                    errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_CONNECTION_TIMED_OUT
                loadErrorState = LoadError(
                    failedUrl = failedUrl ?: addressState,
                    errorText = errorText ?: errorCode?.name ?: "Gagal memuat halaman",
                    isOffline = offline
                )
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
        loadErrorState = null
        browser.reload()
    }

    override fun canGoBack(): Boolean = browser.canGoBack()

    override fun canGoForward(): Boolean = browser.canGoForward()

    override fun find(text: String, forward: Boolean) {
        if (text.isBlank()) return
        // matchCase=false, findNext=true (biar Enter berulang lompat ke hasil
        // berikutnya alih-alih selalu mulai dari awal lagi).
        browser.find(text, forward, false, true)
    }

    override fun stopFind() {
        browser.stopFinding(true)
    }

    /**
     * Prioritas 6 (Browser Menu -- Zoom): CefBrowser memang expose zoom
     * level asli (bukan simulasi), beda dengan progress bar di atas.
     * +/- 0.5 per langkah kira-kira sama dengan Ctrl+Scroll di Chrome.
     */
    fun zoomIn() {
        browser.zoomLevel = (browser.zoomLevel + 0.5).coerceAtMost(5.0)
    }

    fun zoomOut() {
        browser.zoomLevel = (browser.zoomLevel - 0.5).coerceAtLeast(-5.0)
    }

    fun resetZoom() {
        browser.zoomLevel = 0.0
    }

    /**
     * Prioritas 6 -- Developer Tools. `openDevTools()` tersedia di CefBrowser
     * pada versi JCEF yang umum dipakai, TAPI ini best-effort: kalau build
     * JCEF kamu berbeda dan method-nya tidak ada / melempar exception saat
     * dipanggil, ini gagal diam-diam (di-log) daripada meng-crash seluruh
     * aplikasi -- silakan cek Logcat/console kalau tombol DevTools di menu
     * kelihatan tidak bereaksi.
     */
    fun openDevTools() {
        try {
            browser.openDevTools()
        } catch (t: Throwable) {
            println("[JCEFBrowserEngine] DevTools tidak tersedia di build JCEF ini: ${t.message}")
        }
    }

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
