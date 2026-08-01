package com.tradepilot.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.ApplicationScope

/**
 * Prioritas 1: hapus title bar putih bawaan Windows/JVM.
 *
 * Sebelumnya (Main.kt lama):
 *   Window(onCloseRequest = ..., title = "TradePilot AI") { ... }
 * -- ini pakai title bar NATIVE OS (putih di Windows), makanya area paling
 * atas aplikasi kelihatan putih dan bertuliskan "TradePilot AI" dengan style
 * Windows, bukan menyatu dengan tema gelap aplikasi.
 *
 * Fix: `undecorated = true` membuang title bar + border native OS sepenuhnya
 * (window jadi kotak polos tanpa dekorasi apa pun dari OS). Sebagai gantinya
 * kita gambar sendiri title bar custom (lihat CustomTitleBar.kt) yang:
 *  - background-nya AppColors.Base (#1E1E1E) -- tidak ada lagi area putih.
 *  - punya area drag sendiri (WindowDraggableArea, API bawaan Compose Desktop
 *    -- BUKAN reimplementasi manual dari nol) supaya window tetap bisa
 *    digeser walau tanpa title bar native.
 *  - punya 3 tombol sendiri: minimize / maximize-restore / close, yang
 *    memanggil langsung ke `window` (java.awt.Frame) yang disediakan
 *    FrameWindowScope.
 *
 * PENTING: `undecorated = true` di sebagian OS/window manager Linux bisa
 * kehilangan snap-to-edge / Aero Snap bawaan OS (Windows biasanya tetap oke
 * untuk drag-to-maximize karena itu ditangani di CustomTitleBar lewat
 * double-click, bukan lewat OS). Kalau target utamanya Windows (sesuai
 * prompt asli), ini aman.
 */
@Composable
fun ApplicationScope.AppWindow(
    onRequestExit: () -> Unit,
    content: @Composable () -> Unit
) {
    val windowState: WindowState = rememberWindowState(
        placement = WindowPlacement.Floating,
        size = DpSize(1280.dp, 800.dp)
    )

    Window(
        onCloseRequest = onRequestExit,
        state = windowState,
        title = "TradePilot AI",
        // Prioritas 1: inti dari fix-nya.
        undecorated = true,
        // Transparent = false: kita TIDAK butuh window transparan (itu beda
        // masalah dengan "title bar putih"), cukup undecorated + kita gambar
        // background custom sendiri di root Composable (lihat Main.kt Workbench).
        transparent = false
    ) {
        CustomTitleBarHost(windowState = windowState, onRequestExit = onRequestExit) {
            content()
        }
    }
}
