package com.tradepilot.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.application
import com.tradepilot.desktop.browser.JCEFBootstrap
import com.tradepilot.desktop.layout.Workbench
import com.tradepilot.desktop.session.SessionStore
import com.tradepilot.desktop.session.SessionTab
import com.tradepilot.desktop.session.SessionWindow
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
 *
 * FASE 2 (Browser Session/Browser Restore) -- BARU: dulu SELALU mulai dari
 * `windows = [WindowSpec(id=0, isIncognito=false)]` hardcoded. Sekarang cek
 * `SessionStore.load()` dulu -- kalau ada sesi tersimpan dari terakhir
 * ditutup (lihat SessionStore.kt), window-window ITU yang dipulihkan
 * (lengkap dengan tab-tabnya), bukan window default kosong. Window
 * INCOGNITO tidak pernah termasuk (baik disimpan maupun dipulihkan --
 * prinsip privasi, lihat SessionStore.kt).
 */
fun main() = application {
    val savedSession = remember { SessionStore.load() }

    val windows = remember {
        if (savedSession.isNotEmpty()) {
            mutableStateListOf(*savedSession.map { WindowSpec(id = it.id, isIncognito = false) }.toTypedArray())
        } else {
            mutableStateListOf(WindowSpec(id = 0, isIncognito = false))
        }
    }
    var nextWindowId by remember { mutableStateOf((savedSession.maxOfOrNull { it.id } ?: -1) + 1) }

    // Snapshot tab TERBARU per window (dilaporkan oleh Workbench.kt lewat
    // onTabsChanged, sudah didebounce 500ms di sana) -- window incognito
    // TIDAK PERNAH mengisi map ini sama sekali (Workbench.kt skip total
    // pemanggilan onTabsChanged untuk isIncognito=true). Setiap kali map ini
    // berubah, tulis ULANG seluruh session.properties (gabungan semua
    // window non-incognito yang saat ini terbuka) -- overwrite penuh, bukan
    // patch parsial, supaya window yang sudah ditutup otomatis hilang dari
    // file tanpa perlu logic hapus terpisah.
    //
    // Di-seed dari savedSession SEJAK AWAL (bukan mulai kosong menunggu
    // laporan pertama) -- kalau tidak, window yang dipulihkan lalu langsung
    // ditutup SEBELUM debounce 500ms pertama sempat lapor bisa membuat
    // window itu ke-drop dari session.properties (dianggap "belum pernah
    // lapor tab" padahal sebenarnya sudah punya tab dari hasil restore).
    val windowSessionTabs = remember {
        mutableStateMapOf<Int, List<SessionTab>>().apply {
            savedSession.forEach { put(it.id, it.tabs) }
        }
    }

    fun persistSession() {
        val snapshot = windows
            .filterNot { it.isIncognito }
            .mapNotNull { spec ->
                val tabs = windowSessionTabs[spec.id] ?: return@mapNotNull null
                if (tabs.isEmpty()) return@mapNotNull null
                SessionWindow(id = spec.id, tabs = tabs)
            }
        SessionStore.save(snapshot)
    }

    fun closeWindow(id: Int) {
        windows.removeAll { it.id == id }
        windowSessionTabs.remove(id)
        if (windows.isEmpty()) {
            // Window terakhir ditutup lewat tombol X/menu Exit (bukan crash) --
            // simpan final state-nya (kosong kalau semua window memang non-
            // incognito sudah ditutup manual) sebelum benar-benar keluar.
            persistSession()
            JCEFBootstrap.shutdown()
            exitApplication()
        } else {
            persistSession()
        }
    }

    windows.toList().forEach { spec ->
        key(spec.id) {
            AppWindow(
                onRequestExit = { closeWindow(spec.id) },
                isIncognito = spec.isIncognito,
                windowId = spec.id
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
                        },
                        initialTabs = if (spec.isIncognito) null else savedSession.find { it.id == spec.id }?.tabs,
                        onTabsChanged = { tabs ->
                            windowSessionTabs[spec.id] = tabs
                            persistSession()
                        }
                    )
                }
            }
        }
    }
}

private data class WindowSpec(val id: Int, val isIncognito: Boolean)
