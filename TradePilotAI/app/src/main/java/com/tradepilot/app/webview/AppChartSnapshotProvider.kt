package com.tradepilot.app.webview

import com.tradepilot.domain.repository.ChartSnapshotProvider
import com.tradepilot.feature.screenshot.ScreenCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppChartSnapshotProvider @Inject constructor(
    private val webViewHolder: CurrentWebViewHolder,
    private val screenCapture: ScreenCapture
) : ChartSnapshotProvider {

    override suspend fun captureCurrentChart(): ByteArray? {
        val webView = webViewHolder.webView ?: return null
        return withContext(Dispatchers.Main) {
            // View.draw() harus di main thread
            screenCapture.captureCompressed(webView)
        }
    }
}
