package com.tradepilot.feature.browser

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
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Screen utama app (Blueprint versi 1). WebView terarah HANYA ke terminal
 * web Exness. Tombol ANALISA memicu capture screenshot -> AI (Fase 2),
 * TIDAK PERNAH memicu aksi BUY/SELL apapun di WebView.
 */
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = hiltViewModel(),
    onAnalyzeRequested: (WebView) -> Unit = {},
    onWebViewReady: (WebView) -> Unit = {}
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier,
        topBar = {
            BrowserToolbar(
                onBack = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                onForward = { webViewRef?.let { if (it.canGoForward()) it.goForward() } },
                onRefresh = { webViewRef?.reload() },
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
                    onWebViewReady = { webViewRef = it; onWebViewReady(it) },
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
