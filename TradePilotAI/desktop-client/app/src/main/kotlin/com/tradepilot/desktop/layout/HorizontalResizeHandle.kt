package com.tradepilot.desktop.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors
import java.awt.Cursor

/**
 * Bug #3 laporan terbaru ("resize window di edge bawah tidak jalan, atas
 * jalan, kolom AI & Explorer jalan"):
 *
 * Window utama `undecorated = true` (lihat AppWindow.kt, Prioritas 1), jadi
 * Compose Desktop yang mengurus deteksi "user lagi drag edge window buat
 * resize" sendiri di layer Compose/Skia. Itu sebabnya resize JALAN kalau
 * kursor ada di atas title bar custom (murni Compose) atau di sepanjang
 * ExplorerPanel/CopilotPanel (murni Compose juga) -- tapi TIDAK jalan di
 * bagian yang ditutupi JCEFBrowserView, karena itu di-embed lewat
 * [androidx.compose.ui.awt.SwingPanel] (komponen AWT/Swing HEAVYWEIGHT asli)
 * yang menutupi & menyerap event mouse SEBELUM sempat dideteksi sebagai
 * "resize edge" oleh Compose -- workspace/browser makan hampir semua lebar
 * window sampai ke ujung bawah, jadi edge bawah praktis selalu "punya"
 * SwingPanel.
 *
 * Fix: reserve strip 6dp murni Compose di paling bawah Workbench (lihat
 * Row-nya di-`padding(bottom = ...)` di Workbench.kt supaya SwingPanel
 * BENAR-BENAR tidak menutupi strip ini) + drag manual ubah windowState.size
 * sendiri, BUKAN mengandalkan deteksi resize bawaan Compose Desktop lagi.
 */
private val HIT_AREA_HEIGHT_DP = 6.dp
private val RESIZE_CURSOR = PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR))

@Composable
fun HorizontalResizeHandle(
    onDragDeltaPx: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HIT_AREA_HEIGHT_DP)
            .pointerHoverIcon(RESIZE_CURSOR)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isHovered = true },
                    onDragEnd = { isHovered = false },
                    onDragCancel = { isHovered = false }
                ) { change, dragAmount ->
                    change.consume()
                    onDragDeltaPx(dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isHovered) AppColors.TextSecondary else AppColors.Border)
        )
    }
}
