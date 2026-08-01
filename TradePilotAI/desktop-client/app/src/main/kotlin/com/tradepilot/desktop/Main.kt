package com.tradepilot.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.window.application
import com.tradepilot.desktop.browser.JCEFBootstrap
import com.tradepilot.desktop.layout.Workbench
import com.tradepilot.desktop.window.AppWindow

/**
 * PENGGANTI fun main() di Main.kt lama.
 *
 * Beda dengan versi lama: window sekarang dibuka lewat [AppWindow]
 * (Window/AppWindow.kt) yang undecorated + custom title bar (Prioritas 1),
 * bukan `Window(...)` bawaan langsung. Baris
 * `JCEFBootstrap.shutdown(); exitApplication()` TETAP SAMA PERSIS seperti
 * versi lama -- ini bagian lifecycle JCEF yang READMEnya bilang "paling
 * rawan", jadi sengaja tidak diapa-apakan selain dipindah ke dalam lambda
 * onRequestExit yang sekarang dipakai bersama oleh: tombol Close di title
 * bar custom, DAN menu item "Exit" yang baru (Prioritas 6).
 */
fun main() = application {
    val onRequestExit: () -> Unit = {
        JCEFBootstrap.shutdown()
        exitApplication()
    }

    AppWindow(onRequestExit = onRequestExit) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Workbench(onRequestExit = onRequestExit)
        }
    }
}
