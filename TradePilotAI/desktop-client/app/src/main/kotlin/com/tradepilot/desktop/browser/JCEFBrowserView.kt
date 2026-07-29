package com.tradepilot.desktop.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.tradepilot.domain.browser.EXNESS_WEBTRADING_URL

/**
 * Padanan desktop dari `ExnessWebView` (android-client). Membungkus siklus
 * hidup JCEF ([JCEFBootstrap]) + [JCEFBrowserEngine], dan menampilkannya
 * lewat [SwingPanel] karena JCEF berbasis AWT/Swing (CEF belum punya
 * renderer native Compose Desktop).
 *
 * onEngineReady dipanggil sekali saat browser siap, supaya Workbench di
 * Main.kt bisa memasang tombol back/forward/reload (BrowserEngine sama
 * persis interface-nya dengan yang dipakai android-client).
 */
@Composable
fun JCEFBrowserView(
    modifier: Modifier = Modifier,
    // Default halaman awal tetap Exness (aplikasi trading), TAPI ini cuma
    // starting point -- engine di bawah tidak membatasi navigasi ke domain
    // manapun sesudahnya (lihat catatan di JCEFBrowserEngine).
    startUrl: String = EXNESS_WEBTRADING_URL,
    onEngineReady: (JCEFBrowserEngine) -> Unit = {}
) {
    var statusMessage by remember { mutableStateOf("Menyiapkan JCEF...") }
    var engine by remember { mutableStateOf<JCEFBrowserEngine?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        when (val result = JCEFBootstrap.initialize(onProgress = { statusMessage = it })) {
            is JCEFBootstrap.InitResult.Success -> {
                val newEngine = JCEFBrowserEngine(result.client, startUrl)
                engine = newEngine
                onEngineReady(newEngine)
            }
            is JCEFBootstrap.InitResult.Failure -> {
                errorMessage = result.message
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            engine?.dispose()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val currentEngine = engine
        val currentError = errorMessage

        when {
            currentEngine != null -> {
                SwingPanel(
                    modifier = Modifier.fillMaxSize(),
                    factory = { currentEngine.uiComponent }
                )
            }
            currentError != null -> {
                Text(
                    text = currentError,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    Text(
                        text = statusMessage,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    )
                }
            }
        }
    }
}
