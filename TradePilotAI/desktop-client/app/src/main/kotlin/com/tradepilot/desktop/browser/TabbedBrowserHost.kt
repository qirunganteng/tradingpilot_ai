package com.tradepilot.desktop.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FASE 2 -- Multi Tab SUNGGUHAN. Sebelum ini (FASE 1), "tab" cuma metadata
 * (BrowserTab: id/title/url) -- SATU JCEFBrowserEngine dipakai bergantian
 * lewat `loadUrl()` tiap ganti tab (lihat komentar "CATATAN JUJUR: karena
 * satu engine dipakai bergantian" yang DIHAPUS dari Workbench.kt versi
 * lama). Konsekuensinya: pindah tab = reload paksa, scroll position/form
 * yang sedang diisi/state JS semuanya hilang -- BUKAN perilaku tab
 * sungguhan.
 *
 * Sekarang setiap tab di [tabs] dapat [JCEFBrowserEngine] SENDIRI (artinya
 * juga CefBrowser + CefClient sendiri-sendiri -- lihat JCEFBootstrap.kt:
 * `initialize()` sudah menghasilkan CefClient BARU tiap dipanggil sejak
 * audit FASE 1, jadi memberi tiap tab client sendiri di sini OTOMATIS
 * konsisten dengan fix bug "handler cuma nyantol ke instance pertama" --
 * TIDAK ada satu pun perubahan diperlukan di JCEFBootstrap/JCEFBrowserEngine
 * untuk multi-tab ini).
 *
 * SEMUA tab yang terbuka tetap "hidup" penuh di background (proses render,
 * state JS, scroll position, dst -- semua tetap jalan), persis perilaku
 * default Chrome (BELUM ada tab discarding/suspend untuk hemat memori --
 * itu optimisasi performa, di luar checklist FASE 2, kandidat kerjaan
 * lanjutan). `key(tab.id)` di bawah krusial: itu yang menjaga identitas
 * slot Compose tab tetap stabil lintas switch/reorder -- engine-nya HANYA
 * benar-benar di-dispose (lewat DisposableEffect bawaan JCEFBrowserView)
 * saat tab itu betul-betul hilang dari [tabs] (ditutup), BUKAN saat cuma
 * berpindah jadi tidak aktif.
 *
 * Tab tidak aktif di-collapse ke `Modifier.size(0.dp)` (bukan dihapus dari
 * composition) supaya tetap invisible & tidak menangkap input, sementara
 * tab aktif `fillMaxSize()`. CATATAN JUJUR: belum sempat diuji interaktif
 * (tidak ada akses jalankan app Windows dari sini) -- kalau ternyata CEF
 * windowed rendering versi ini bermasalah dengan native panel ber-ukuran
 * persis 0x0 (jarang, tapi mungkin di kombinasi OS/driver tertentu),
 * gantilah ke ukuran minimal 1.dp alih-alih 0.dp sebagai mitigasi cepat.
 */
@Composable
fun TabbedBrowserHost(
    tabs: List<BrowserTab>,
    activeTabId: String,
    isIncognito: Boolean,
    onEngineForTab: (tabId: String, engine: JCEFBrowserEngine) -> Unit,
    onTabNavigated: (tabId: String, url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        for (tab in tabs) {
            key(tab.id) {
                val isActive = tab.id == activeTabId
                Box(modifier = if (isActive) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
                    var engine by remember { mutableStateOf<JCEFBrowserEngine?>(null) }

                    JCEFBrowserView(
                        modifier = Modifier.fillMaxSize(),
                        startUrl = tab.url,
                        isIncognito = isIncognito,
                        onEngineReady = { newEngine ->
                            engine = newEngine
                            onEngineForTab(tab.id, newEngine)
                        }
                    )

                    // Sinkron url/title tab INI dari engine MILIKNYA SENDIRI --
                    // bukan cuma tab aktif. Tab background yang redirect lewat
                    // JS (mis. halaman auth yang auto-refresh) tetap harus
                    // update title/url tab-nya sendiri, sama seperti Chrome
                    // sungguhan menampilkan perubahan title tab background.
                    val currentEngine = engine
                    if (currentEngine != null) {
                        LaunchedEffect(currentEngine.addressState, currentEngine.titleState) {
                            onTabNavigated(tab.id, currentEngine.addressState, currentEngine.titleState)
                        }
                    }
                }
            }
        }
    }
}
