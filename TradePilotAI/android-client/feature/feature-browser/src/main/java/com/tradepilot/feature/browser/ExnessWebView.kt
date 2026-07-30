package com.tradepilot.feature.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tradepilot.domain.browser.EXNESS_WEBTRADING_URL

/**
 * WebView browser umum (dulu: "ExnessWebView", cuma bisa ke Exness -- nama
 * class dipertahankan supaya tidak ganti di banyak tempat, tapi sekarang
 * genuinely generic, lihat startUrl & BrowserToolbar yang sudah punya
 * address bar).
 *
 * Hardening keamanan (Blueprint 13) TETAP dipertahankan:
 *  - File access & universal access from file URLs DIMATIKAN.
 *  - TIDAK ADA addJavascriptInterface yang mengekspos kontrol trading.
 *
 * FIX BUG "halaman blank begitu ngetik sandi" (dilaporkan user, terjadi di
 * form password Exness webtrading) -- 2 penyebab paling umum untuk gejala
 * PERSIS ini di WebView, keduanya sekarang di-fix:
 *
 * 1. Third-party cookies DIMATIKAN secara default oleh Android WebView.
 *    Login/SSO modern (termasuk kemungkinan besar Exness) sering redirect
 *    lewat subdomain auth terpisah yang butuh set cookie cross-domain saat
 *    proses submit password -- kalau ditolak, request auth gagal SENYAP
 *    (tidak ada error jelas ke user) dan halaman berakhir blank/stuck.
 * 2. User-Agent default WebView berisi penanda "; wv)" yang menandakan
 *    "ini WebView tertanam, bukan browser asli". BANYAK platform finansial
 *    (Google sendiri melakukan ini untuk OAuth) SENGAJA mendeteksi penanda
 *    ini lalu memblokir/mengosongkan konten sensitif seperti form password,
 *    demi mencegah pencurian kredensial lewat WebView tersembunyi/tidak
 *    dipercaya. Fix: buang penanda "; wv" dari UA supaya terlihat seperti
 *    Chrome Mobile biasa (bukan menyamar jadi browser lain, cuma
 *    menghilangkan flag yang secara spesifik menandai "WebView tertanam").
 *
 * Kalau setelah fix ini masalahnya MASIH terjadi, onReceivedError/
 * onConsoleMessage di bawah sekarang di-log ke Logcat (tag "TradePilotWebView")
 * -- sebelumnya WebViewClient default diam saja kalau ada error, jadi kalau
 * masih blank, sambungkan device ke `adb logcat` atau chrome://inspect
 * dan cari tag itu untuk lihat error sebenarnya, alih-alih nebak lagi.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExnessWebView(
    modifier: Modifier = Modifier,
    startUrl: String = EXNESS_WEBTRADING_URL,
    onWebViewReady: (WebView) -> Unit = {},
    onPageLoading: (Boolean) -> Unit = {},
    onUrlChanged: (String) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true // dibutuhkan Exness webtrading app
                settings.databaseEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.allowFileAccessFromFileURLs = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // Fix #2 (lihat catatan kelas): buang penanda "; wv)" dari UA.
                val defaultUserAgent: String = settings.userAgentString
                settings.userAgentString = defaultUserAgent.replace("; wv", "")

                // Fix #1 (lihat catatan kelas): izinkan third-party cookies
                // KHUSUS untuk WebView ini (bukan global/semua WebView di app).
                // `webView` di sini eksplisit merujuk WebView yang lagi dibuat,
                // supaya tidak ambigu dengan `apply` milik CookieManager di baris
                // berikutnya (kalau pakai this@apply tanpa label, ketertukar ke
                // scope apply terdekat -- CookieManager, bukan WebView -- itu bug
                // yang sempat kejadian di draf pertama fix ini).
                val webView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, true)
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        onPageLoading(true)
                        if (url != null) onUrlChanged(url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageLoading(false)
                        if (url != null) onUrlChanged(url)
                        // Persist cookie session (mis. token login) supaya tidak
                        // hilang kalau proses app di-kill OS di background.
                        CookieManager.getInstance().flush()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            android.util.Log.e(
                                "TradePilotWebView",
                                "onReceivedError (main frame): ${error?.description} @ ${request.url}"
                            )
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: android.webkit.SslErrorHandler?,
                        error: android.net.http.SslError?
                    ) {
                        // SENGAJA tetap cancel() (perilaku default/aman) --
                        // TIDAK proceed() begitu saja walau lagi debugging bug
                        // ini, supaya tidak buka celah MITM. Cuma ditambah log
                        // supaya kalau INI penyebabnya, kelihatan di Logcat.
                        android.util.Log.e("TradePilotWebView", "onReceivedSslError: $error @ ${error?.url}")
                        handler?.cancel()
                    }
                }

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                        if (message != null) {
                            android.util.Log.d(
                                "TradePilotWebView",
                                "console: ${message.message()} (${message.sourceId()}:${message.lineNumber()})"
                            )
                        }
                        return true
                    }
                }

                loadUrl(startUrl)
                onWebViewReady(this)
            }
        }
    )
}
