package com.tradepilot.app.webview

import android.webkit.WebView
/**
 * Jembatan tipis antar-modul di level app: BrowserScreen mendaftarkan
 * WebView aktif ke sini (lewat onWebViewReady), lalu AppChartSnapshotProvider
 * membacanya untuk capture. Ini sengaja diletakkan di app module (bukan
 * feature-browser maupun feature-notification) supaya kedua feature
 * tetap independen sesuai aturan modul di Blueprint bagian 0.
 */
class CurrentWebViewHolder constructor() {
    @Volatile
    var webView: WebView? = null
}
