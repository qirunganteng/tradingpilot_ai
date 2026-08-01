package com.tradepilot.desktop.fullscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Prioritas 9: Fullscreen Browser.
 *
 * Sebelumnya "isWorkspaceFullscreen" di Main.kt (lama) cuma menyembunyikan
 * ActivityBar/SideBar/CopilotPanel dari layout Row -- TAPI TabsBar +
 * BrowserBar (address bar) TETAP tampil terus, itu bug yang dilaporkan:
 * "Saat Fullscreen, URL masih terlihat."
 *
 * Fix di sini: begitu fullscreen aktif, TabsBar + BrowserBar (semua chrome
 * di atas area render browser) DISEMBUNYIKAN JUGA -- bukan cuma
 * ActivityBar/SideBar/CopilotPanel. Yang tersisa betul-betul cuma konten
 * halaman (JCEFBrowserView), persis seperti Chrome F11.
 *
 * Supaya tetap bisa mengakses toolbar tanpa keluar fullscreen dulu (mis.
 * ganti tab/ketik URL baru), toolbar muncul lagi otomatis kalau mouse
 * digerakkan ke ~4dp paling atas layar, dan hilang lagi otomatis setelah
 * beberapa detik tidak disentuh (auto-hide) -- itu perilaku
 * [FullscreenRevealHost] di bawah.
 */
@Composable
fun FullscreenRevealHost(
    isFullscreen: Boolean,
    hiddenChrome: @Composable () -> Unit,
    browserContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isFullscreen) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) { hiddenChrome() }
            Box(modifier = Modifier.fillMaxSize()) { browserContent() }
        }
        return
    }

    var isRevealed by remember { mutableStateOf(false) }

    // Auto-hide: begitu revealed, tunggu 2.5 detik tanpa mouse di area atas,
    // lalu sembunyikan lagi -- supaya konten browser dapat ruang penuh.
    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            delay(2500)
            isRevealed = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Pass = Initial supaya deteksi posisi mouse tidak "dimakan"
            // duluan oleh child (address bar dkk saat revealed).
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val y = event.changes.firstOrNull()?.position?.y ?: continue
                        if (y <= with(this) { 6.dp.toPx() }) {
                            isRevealed = true
                        }
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) { browserContent() }

        AnimatedVisibility(
            visible = isRevealed,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) { hiddenChrome() }
        }
    }
}
