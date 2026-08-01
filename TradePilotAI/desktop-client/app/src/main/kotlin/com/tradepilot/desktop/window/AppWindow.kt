package com.tradepilot.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.ApplicationScope
import java.awt.GraphicsEnvironment

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
 * kita gambar sendiri title bar custom (lihat CustomTitleBar.kt).
 *
 * BUG #8 FIX ASLI ("fullscreen mentok di bawah tombol X") + FIX REGRESI
 * BARU ("fullscreen -> klik menu/Settings -> semua tombol freeze + bunyi
 * beep"): perbaikan #8 sebelumnya pakai `WindowPlacement.Fullscreen` (mode
 * OS EXCLUSIVE fullscreen sungguhan). Itu benar menyelesaikan #8, TAPI
 * begitu BrowserMenu.kt & SettingsDialog.kt diperbaiki (oleh proses lain,
 * pindah dari DropdownMenu/Dialog ke [DialogWindow] -- window OS terpisah
 * sungguhan, lihat catatan di file-file itu), muncul REGRESI baru: window
 * OS baru yang di-spawn SAAT window utama sedang exclusive-fullscreen bikin
 * Windows bingung soal fokus input (window fullscreen exclusive biasanya
 * "mengunci" fokus ke dirinya sendiri) -- hasilnya klik di DialogWindow
 * baru tidak diterima sama sekali, dan OS berbunyi beep tiap kali input
 * "nyasar" ke window yang tidak bisa menerimanya.
 *
 * Fix: JANGAN pakai `WindowPlacement.Fullscreen` (exclusive) sama sekali --
 * cukup resize window yang MASIH `WindowPlacement.Floating` biasa supaya
 * posisi & ukurannya PERSIS menutupi seluruh layar (pakai
 * `GraphicsEnvironment` buat tahu bounds layar). Secara visual hasilnya
 * SAMA PERSIS (edge-to-edge, dikombinasikan dengan CustomTitleBarHost yang
 * skip render title bar row saat fullscreen), TAPI window tetap window
 * "biasa" di mata OS -- tidak ada mode exclusive yang bisa bentrok dengan
 * DialogWindow lain.
 */
@Composable
fun ApplicationScope.AppWindow(
    onRequestExit: () -> Unit,
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val windowState: WindowState = rememberWindowState(
        placement = WindowPlacement.Floating,
        size = DpSize(1280.dp, 800.dp)
    )
    val fullscreenState = remember { AppFullscreenState() }
    val density = LocalDensity.current

    var savedPlacement by remember { mutableStateOf(WindowPlacement.Floating) }
    var savedPosition by remember { mutableStateOf<WindowPosition?>(null) }
    var savedSize by remember { mutableStateOf<DpSize?>(null) }

    LaunchedEffect(fullscreenState.isFullscreen) {
        if (fullscreenState.isFullscreen) {
            savedPlacement = windowState.placement
            savedPosition = windowState.position
            savedSize = windowState.size

            val screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            windowState.placement = WindowPlacement.Floating
            with(density) {
                windowState.position = WindowPosition(screenBounds.x.toDp(), screenBounds.y.toDp())
                windowState.size = DpSize(screenBounds.width.toDp(), screenBounds.height.toDp())
            }
        } else {
            savedSize?.let { windowState.size = it }
            savedPosition?.let { windowState.position = it }
            windowState.placement = savedPlacement
        }
    }

    Window(
        onCloseRequest = onRequestExit,
        state = windowState,
        title = if (isIncognito) "TradePilot AI — Incognito" else "TradePilot AI",
        // Prioritas 1: inti dari fix-nya.
        undecorated = true,
        // Transparent = false: kita TIDAK butuh window transparan (itu beda
        // masalah dengan "title bar putih"), cukup undecorated + kita gambar
        // background custom sendiri di root Composable (lihat Main.kt Workbench).
        transparent = false
    ) {
        CompositionLocalProvider(
            LocalAppFullscreenState provides fullscreenState,
            LocalAppWindowState provides windowState
        ) {
            CustomTitleBarHost(windowState = windowState, isIncognito = isIncognito, onRequestExit = onRequestExit) {
                content()
            }
        }
    }
}
