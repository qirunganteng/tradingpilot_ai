package com.tradepilot.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * kita gambar sendiri title bar custom (lihat CustomTitleBar.kt).
 *
 * BUG #8 FIX ("fullscreen mentok di bawah tombol X"): dulu toggle fullscreen
 * cuma menyembunyikan konten INTERNAL Workbench, window OS-nya sendiri tetap
 * di WindowPlacement.Floating/Maximized biasa dan CustomTitleBar (32dp)
 * selalu tetap tampil di atasnya. Sekarang AppWindow mengamati
 * [AppFullscreenState] (lihat AppFullscreenState.kt, di-toggle dari
 * Workbench lewat F11/tombol fullscreen) dan mendorong window BENAR-BENAR
 * ke WindowPlacement.Fullscreen (edge-to-edge, menutup seluruh layar
 * termasuk area taskbar) begitu aktif -- dikombinasikan dengan
 * CustomTitleBarHost yang skip render title bar row saat fullscreen (lihat
 * CustomTitleBar.kt), hasilnya benar-benar mentok ke ujung atas layar
 * persis seperti Chrome F11, bukan cuma berhenti di bawah title bar lagi.
 * Placement sebelum fullscreen disimpan supaya balik ke situ (bukan selalu
 * ke Floating) saat keluar fullscreen.
 *
 * isIncognito: dipakai buat menu "New Incognito Window" (Browser Menu) --
 * cuma mengubah judul window supaya kelihatan jelas ini window incognito,
 * isolasi cookie/cache sesungguhnya ada di level JCEFBrowserEngine (lihat
 * catatan jujur di sana soal batasannya).
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
    var placementBeforeFullscreen by remember { mutableStateOf(WindowPlacement.Floating) }

    LaunchedEffect(fullscreenState.isFullscreen) {
        if (fullscreenState.isFullscreen) {
            if (windowState.placement != WindowPlacement.Fullscreen) {
                placementBeforeFullscreen = windowState.placement
            }
            windowState.placement = WindowPlacement.Fullscreen
        } else if (windowState.placement == WindowPlacement.Fullscreen) {
            windowState.placement = placementBeforeFullscreen
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
