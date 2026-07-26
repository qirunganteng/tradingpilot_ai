package com.tradepilot.app.webview

import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentWebViewHolder @Inject constructor() {
    @Volatile
    var webView: WebView? = null
}
