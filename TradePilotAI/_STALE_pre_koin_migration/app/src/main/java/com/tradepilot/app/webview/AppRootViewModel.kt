package com.tradepilot.app.webview

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.core.common.PendingAnalysisHolder
import com.tradepilot.feature.screenshot.ScreenCapture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val screenCapture: ScreenCapture,
    private val pendingAnalysisHolder: PendingAnalysisHolder,
    private val currentWebViewHolder: CurrentWebViewHolder
) : ViewModel() {

    fun registerWebView(webView: WebView) {
        currentWebViewHolder.webView = webView
    }

    fun onAnalyzeRequested(webView: WebView) {
        viewModelScope.launch {
            val jpeg = screenCapture.captureCompressed(webView)
            jpeg?.let {
                pendingAnalysisHolder.submit(it)
            }
        }
    }
}
