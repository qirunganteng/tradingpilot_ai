package com.tradepilot.desktop.fullscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 *
 * BUG KRITIS DITEMUKAN & DIPERBAIKI ("layar putih saat fullscreen, ESC/
 * kembali serasa tidak berfungsi" -- laporan user): versi sebelumnya
 * memanggil `browserContent()` dari DUA LOKASI KODE BERBEDA (early-return
 * `if (!isFullscreen) { Column{...}; return }` vs cabang fullscreen
 * `Box{...}`), dengan struktur container yang SAMA SEKALI beda (Column vs
 * Box). Compose slot table mengidentifikasi composable berdasarkan POSISI
 * di source/struktur eksekusi -- dua call site berbeda struktur = dianggap
 * composable BERBEDA. Akibatnya setiap kali `isFullscreen` toggle, SELURUH
 * subtree `browserContent()` (yaitu TabbedBrowserHost -> SEMUA
 * JCEFBrowserEngine, BUKAN cuma tab aktif) di-DISPOSE lalu DIBUAT ULANG
 * DARI NOL (CefBrowser baru, JCEF bootstrap baru) -- itu yang kelihatan
 * sebagai layar putih (browser baru lagi boot), dan kenapa "tidak bisa
 * kembali" terasa nyata (state/history halaman lama sudah lenyap duluan
 * sebelum sempat kelihatan lagi).
 *
 * Fix: [movableContentOf] -- API Compose RESMI untuk kasus "pindahkan
 * composable ke posisi layout lain TANPA kehilangan identitas/state-nya"
 * (dipakai persis untuk skenario "video player pindah antara mode
 * fullscreen & normal" di dokumentasi Compose -- kasus kita identik).
 * `browserContent`/`hiddenChrome` SEKARANG dibungkus sekali lewat
 * `remember { movableContentOf(...) }`, lalu WRAPPER-nya (bukan lambda
 * asli) yang dipanggil di kedua cabang -- Compose tahu ini konten yang
 * SAMA, jadi node-nya (termasuk JCEFBrowserEngine & CefBrowser di
 * dalamnya) di-PINDAH ke posisi baru, bukan dibuat ulang.
 */
@Composable
fun FullscreenRevealHost(
    isFullscreen: Boolean,
    hiddenChrome: @Composable () -> Unit,
    browserContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // remember(Unit) SENGAJA -- wrapper ini HARUS dibuat SEKALI seumur hidup
    // composable ini & TIDAK PERNAH diganti lagi walau parameter lambda yang
    // dikirim dari Workbench.kt berubah identitas objeknya tiap recomposition
    // (lambda Kotlin biasa MEMANG selalu instance baru tiap recomposition --
    // itu normal, BUKAN berarti "konten"-nya beda). Aman: closure di dalam
    // browserContent/hiddenChrome membaca State/SnapshotStateList (nilai
    // LIVE), bukan primitif ter-snapshot, jadi tetap benar walau instance
    // lambda yang dibungkus adalah yang "pertama" ditangkap.
    val stableBrowserContent = remember { movableContentOf(browserContent) }
    val stableHiddenChrome = remember { movableContentOf(hiddenChrome) }

    if (!isFullscreen) {
        // FIX BUG: sebelumnya di sini pakai Box + fillMaxSize/fillMaxWidth untuk
        // KEDUA child (hiddenChrome & browserContent) sekaligus -- itu bikin
        // keduanya TUMPANG TINDIH di posisi yang sama (Box menggambar child
        // berurutan, yang belakangan "di atas"). Karena browserContent berisi
        // JCEFBrowserView yang di-render lewat SwingPanel (komponen native/
        // heavyweight AWT), SwingPanel SELALU digambar di atas konten Compose
        // apa pun yang tumpang tindih koordinatnya, terlepas dari urutan kode
        // -- jadi TabsBar + BrowserBar (address bar) + BrowserMenu yang ada di
        // hiddenChrome jadi TERTUTUP TOTAL oleh browser, cuma keliatan
        // "terminal Exness doang" walau kodenya sebenarnya sudah benar/lengkap.
        //
        // FIX: pakai Column, bukan Box -- hiddenChrome (TabsBar+BrowserBar)
        // ambil tinggi natural-nya di ATAS, browserContent ambil SISA ruang di
        // BAWAHNYA (weight(1f)) -- keduanya jadi bersebelahan vertikal, TIDAK
        // tumpang tindih sama sekali, persis layout Chrome/VSCode yang benar.
        Column(modifier = modifier.fillMaxSize()) {
            stableHiddenChrome()
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) { stableBrowserContent() }
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
        Box(modifier = Modifier.fillMaxSize()) { stableBrowserContent() }

        AnimatedVisibility(
            visible = isRevealed,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) { stableHiddenChrome() }
        }
    }
}
