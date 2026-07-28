package com.tradepilot.feature.browser
import org.koin.androidx.compose.koinViewModel

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tradepilot.domain.browser.BrowserEngine

/**
 * Screen utama app (Blueprint versi 1). WebView terarah HANYA ke terminal
 * web Exness. Tombol ANALISA memicu capture screenshot -> AI (Fase 2),
 * TIDAK PERNAH memicu aksi BUY/SELL apapun di WebView.
 *
 * Fase 3: navigasi (back/forward/reload) sekarang lewat [BrowserEngine]
 * ([WebViewBrowserEngine]), bukan manipulasi [WebView] mentah langsung --
 * ini yang nanti dipertukarkan dengan JCEFBrowserEngine di desktop-client
 * tanpa BrowserToolbar perlu tahu bedanya. `onAnalyzeRequested`/`onWebViewReady`
 * TETAP menerima WebView mentah (bukan BrowserEngine) karena keduanya dipakai
 * app module untuk ChartSnapshotProvider (screenshot), yang sudah punya
 * abstraksinya sendiri dan sengaja tidak digabung ke sini (lihat catatan
 * di BrowserEngine.kt).
 */
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = koinViewModel(),
    onAnalyzeRequested: (WebView) -> Unit = {},
    onWebViewReady: (WebView) -> Unit = {}
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var browserEngine by remember { mutableStateOf<BrowserEngine?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier,
        topBar = {
            BrowserToolbar(
                onBack = { browserEngine?.goBack() },
                onForward = { browserEngine?.goForward() },
                onRefresh = { browserEngine?.reload() },
                onToggleFullscreen = { viewModel.toggleFullscreen() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "ANALISA") }, // TODO: ganti dengan stringResource(R.string.btn_analyze) saat resource dipindah ke modul ini
                icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                onClick = { webViewRef?.let(onAnalyzeRequested) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ExnessWebView(
                    modifier = Modifier.fillMaxSize(),
                    onWebViewReady = { webView ->
                        webViewRef = webView
                        browserEngine = WebViewBrowserEngine(webView)
                        onWebViewReady(webView)
                    },
                    onPageLoading = { isLoading = it }
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
