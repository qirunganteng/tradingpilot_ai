package com.tradepilot.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.application
import com.tradepilot.desktop.browser.JCEFBootstrap
import com.tradepilot.desktop.layout.Workbench
import com.tradepilot.desktop.window.AppWindow

/**
 * PENGGANTI fun main() di Main.kt lama.
 *
 * Beda dengan versi lama: window sekarang dibuka lewat [AppWindow]
 * (Window/AppWindow.kt) yang undecorated + custom title bar (Prioritas 1),
 * bukan `Window(...)` bawaan langsung.
 *
 * PERUBAHAN BARU -- dukungan multi-window untuk Browser Menu "New Window" &
 * "New Incognito Window" (dulu stub kosong `/* TODO */`): `application { }`
 * di Compose Desktop itu sendiri composable (`@Composable ApplicationScope.()
 * -> Unit`), jadi bisa punya state biasa (`remember`) -- daftar `windows` di
 * bawah adalah SEMUA window yang sedang terbuka, di-render lewat perulangan
 * `AppWindow`. Klik "New Window"/"New Incognito Window" di salah satu
 * window cuma menambah entri baru ke daftar ini; klik Close/tombol X di
 * title bar salah satu window menghapus entri itu SAJA (window lain tetap
 * terbuka). Aplikasi baru benar-benar keluar (shutdown JCEF + exitApplication)
 * begitu window TERAKHIR ditutup -- pola standar aplikasi multi-window (mis.
 * Chrome: nutup 1 window browser tidak menutup seluruh aplikasi selama masih
 * ada window lain).
 *
 * `key(spec.id)` WAJIB di sini -- tanpa itu Compose bisa salah mengasosiasikan
 * state internal (mis. WindowState posisi/ukuran) antar window kalau daftar
 * berubah urutan/panjang saat window dibuka/ditutup.
 */
fun main() = application {
    val windows = remember { mutableStateListOf(WindowSpec(id = 0, isIncognito = false)) }
    var nextWindowId by remember { mutableStateOf(1) }

    fun closeWindow(id: Int) {
        windows.removeAll { it.id == id }
        if (windows.isEmpty()) {
            JCEFBootstrap.shutdown()
            exitApplication()
        }
    }

    windows.toList().forEach { spec ->
        key(spec.id) {
            AppWindow(
                onRequestExit = { closeWindow(spec.id) },
                isIncognito = spec.isIncognito
            ) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Workbench(
                        onRequestExit = { closeWindow(spec.id) },
                        isIncognito = spec.isIncognito,
                        onOpenNewWindow = {
                            windows.add(WindowSpec(id = nextWindowId++, isIncognito = false))
                        },
                        onOpenIncognitoWindow = {
                            windows.add(WindowSpec(id = nextWindowId++, isIncognito = true))
                        }
                    )
                }
            }
        }
    }
}

private data class WindowSpec(val id: Int, val isIncognito: Boolean)
