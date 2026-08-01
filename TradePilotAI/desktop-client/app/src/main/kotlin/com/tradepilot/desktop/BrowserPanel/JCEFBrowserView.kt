package com.tradepilot.desktop.duplicate.browserpanel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.tradepilot.desktop.theme.AppColors
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
 *
 * Prioritas 12 (Browser Loading) -- tambahan dibanding versi lama:
 *  - Loading Progress: LinearProgressIndicator tipis di paling atas
 *    (di atas SwingPanel), muncul saat engine.loadingProgressState < 1f.
 *  - Error Page / Offline Page: begitu engine.loadErrorState terisi,
 *    SwingPanel disembunyikan & diganti halaman error dengan tombol
 *    "Coba lagi" yang reload ke URL yang gagal.
 *  - Spinner & status JCEF bootstrap AWAL (sebelum engine ada sama sekali)
 *    -- SAMA seperti versi lama, tidak diubah.
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
                val pageError = currentEngine.loadErrorState
                if (pageError != null) {
                    BrowserErrorPage(
                        error = pageError,
                        onRetry = { currentEngine.loadUrl(pageError.failedUrl) }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val progress by animateFloatAsState(currentEngine.loadingProgressState)
                        if (progress < 1f) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().padding(0.dp),
                                color = AppColors.Accent,
                                trackColor = AppColors.Base
                            )
                        }
                        SwingPanel(
                            modifier = Modifier.fillMaxSize(),
                            factory = { currentEngine.uiComponent }
                        )
                    }
                }
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

/** Prioritas 12: Error Page (generik) / Offline Page (kalau isOffline). */
@Composable
private fun BrowserErrorPage(error: JCEFBrowserEngine.LoadError, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (error.isOffline) Icons.Default.CloudOff else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = if (error.isOffline) "Tidak ada koneksi internet" else "Halaman tidak bisa dimuat",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${error.failedUrl}\n${error.errorText}",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Coba lagi")
            }
        }
    }
}
