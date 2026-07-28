package com.tradepilot.feature.browser

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tradepilot.domain.browser.EXNESS_WEBTRADING_URL

/**
 * WebView khusus terminal web Exness — di-hardening sesuai Blueprint 13 (Security Design):
 *  - File access & universal access from file URLs DIMATIKAN.
 *  - TIDAK ADA addJavascriptInterface yang mengekspos kontrol trading (klik BUY/SELL).
 *  - Hanya domain Exness yang boleh dinavigasi; percobaan navigasi keluar domain
 *    tetap di-load di WebView yang sama (bukan redirect ke browser luar) tapi
 *    tidak diberi hak istimewa tambahan apapun.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExnessWebView(
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {},
    onPageLoading: (Boolean) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true // dibutuhkan Exness webtrading app
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.allowFileAccessFromFileURLs = false

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        onPageLoading(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageLoading(false)
                    }
                }

                loadUrl(EXNESS_WEBTRADING_URL)
                onWebViewReady(this)
            }
        }
    )
}
