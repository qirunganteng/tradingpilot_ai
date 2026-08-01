package com.tradepilot.desktop.window

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowState

/**
 * State fullscreen "workspace" yang perlu dibaca DUA composable berbeda:
 * Workbench.kt (yang men-toggle lewat F11 / tombol fullscreen di
 * BrowserBar) DAN CustomTitleBarHost (yang perlu tahu kapan HARUS
 * menyembunyikan title bar row-nya sendiri).
 *
 * BUG YANG DIPERBAIKI (#8 -- "fullscreen mentok di bawah tombol X"):
 * sebelumnya "workspace fullscreen" cuma state lokal di dalam Workbench,
 * jadi cuma menyembunyikan ActivityBar/Sidebar/Toolbar DI DALAM Workbench --
 * CustomTitleBar (32dp, berisi tombol minimize/maximize/close) ada di LUAR
 * Workbench (dipasang dari AppWindow.kt/CustomTitleBar.kt) dan sama sekali
 * tidak tahu soal fullscreen, jadi selalu tetap tampil -- makanya konten
 * fullscreen cuma bisa mengisi sampai TEPAT DI BAWAH title bar itu, tidak
 * pernah benar-benar mentok ke ujung atas layar seperti Chrome F11.
 *
 * Fix: CompositionLocal ini dibaca AppWindow.kt untuk (1) mendorong window
 * ke WindowPlacement.Fullscreen sungguhan (edge-to-edge, bukan cuma
 * menyembunyikan konten internal) dan (2) oleh CustomTitleBarHost untuk
 * skip render CustomTitleBar sama sekali saat fullscreen aktif. Dipakai
 * CompositionLocal (bukan parameter berantai) karena AppWindow.kt (window)
 * & Workbench.kt (layout) tidak punya hubungan parent-child composable
 * langsung yang gampang buat threading parameter biasa.
 */
class AppFullscreenState {
    var isFullscreen by mutableStateOf(false)
}

val LocalAppFullscreenState = compositionLocalOf { AppFullscreenState() }

/**
 * Dipakai JCEFBrowserView.kt sebagai mitigasi bug #10 ("klik maximize,
 * halaman browser jadi putih kosong") -- SwingPanel yang membungkus
 * komponen native JCEF kadang tidak dapat notifikasi resize yang benar dari
 * OS saat window di-maximize/di-restore lewat WindowPlacement (dibanding
 * drag-resize manual, yang biasanya normal). Dengan expose WindowState di
 * sini, JCEFBrowserView bisa `LaunchedEffect(windowState.placement,
 * windowState.size)` dan memaksa revalidate/repaint komponen browser setiap
 * kali placement/size berubah.
 *
 * CATATAN JUJUR: ini best-effort mitigation untuk bug rendering native
 * (SwingPanel + AWT heavyweight component) yang tidak bisa saya uji
 * interaktif dari sini (tidak ada akses jalankan & lihat langsung tampilan
 * aplikasi Windows). Kalau setelah ini bug #10 masih muncul di sebagian
 * kasus, kabari lagi -- mungkin perlu pendekatan lain (mis. force window
 * resize 1px nudge, atau OSR/offscreen rendering mode di JCEFBootstrap).
 */
val LocalAppWindowState = compositionLocalOf<WindowState?> { null }
