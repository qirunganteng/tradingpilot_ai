package com.tradepilot.desktop.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.CefClient
import java.io.File

/**
 * Lifecycle CefApp/CefClient (JCEF). HANYA boleh ada SATU CefApp per proses
 * JVM -- ini batasan JCEF sendiri, bukan pilihan desain kita -- karena itu
 * object singleton, bukan class biasa.
 *
 * BACA SEBELUM MENJALANKAN (ini Fase paling berisiko di roadmap kamu):
 *  - Saat PERTAMA KALI dijalankan di satu mesin, jcefmaven akan MENGUNDUH
 *    native binary Chromium/CEF (~100-200MB tergantung OS) ke folder
 *    `~/.tradepilot/jcef-bundle`. Butuh koneksi internet & bisa makan waktu
 *    beberapa menit. Run berikutnya pakai cache lokal, jadi cepat.
 *  - `initialize()` idempoten dari sisi CefApp (aman dipanggil berkali-kali
 *    dari Compose recomposition -- cuma proses pertama yang benar-benar
 *    membangun CefApp), TAPI setiap pemanggilan yang berhasil mengembalikan
 *    [CefClient] BARU (lihat catatan "BUG DITEMUKAN" di bawah -- ini bukan
 *    lupa cache, tapi perbaikan yang disengaja).
 *  - WAJIB panggil `shutdown()` sebelum aplikasi keluar (lihat `main()` di
 *    Main.kt) -- kalau tidak, proses native CEF child bisa tertinggal
 *    jalan di background (zombie process) setelah window ditutup.
 *
 * BUG DITEMUKAN & DIPERBAIKI (audit FASE 1, referensi: cef-master +
 * dokumentasi JCEF resmi -- org.cef.CefClient.addDisplayHandler):
 *   Versi lama object ini MENYIMPAN SATU CefClient di field statis dan
 *   membagikannya ke SELURUH window/engine yang pernah dibuat ("New Window"
 *   / "New Incognito Window" dari BrowserMenu ikut memanggil initialize()
 *   ini dan mendapat client yang SAMA). Masalahnya: `CefClient.addDisplayHandler()`
 *   / `addLoadHandler()` / `addRequestHandler()` di JCEF **HANYA menyimpan
 *   handler PERTAMA yang didaftarkan lalu mengabaikan diam-diam semua
 *   pemanggilan berikutnya** (bukan daftar/list seperti kelihatannya dari
 *   nama method "add"). Akibatnya: browser di window KEDUA dan seterusnya
 *   tetap bisa memuat halaman, TAPI address bar/title/status loading/tombol
 *   back-forward/error page-nya tidak akan pernah ter-update, karena handler
 *   milik JCEFBrowserEngine window itu tidak pernah benar-benar terpasang
 *   di CefClient yang sudah "penuh" duluan oleh window pertama.
 *
 *   Perbaikan: setiap panggilan initialize() yang sukses sekarang membangun
 *   [CefClient] BARU dari [CefApp] yang sama (CefApp WAJIB tetap singleton,
 *   tapi CefClient TIDAK -- ini pola standar CEF/JCEF untuk banyak window;
 *   lihat dokumentasi resmi JCEF: "the same [CefClient] instance can be
 *   shared among multiple browsers... it is up to the developer to use a
 *   shared or per-browser instance, depending on the handlers' logic").
 *   Setiap [com.tradepilot.desktop.browser.JCEFBrowserEngine] jadi pemilik
 *   tunggal client-nya sendiri dan WAJIB men-dispose client itu lewat
 *   `disposeClient()` di bawah saat engine-nya di-dispose (lihat
 *   JCEFBrowserEngine.dispose()). `shutdown()` di bawah tetap jadi jaring
 *   pengaman terakhir untuk client mana pun yang mungkin lolos tidak
 *   ter-dispose (mis. crash sebelum sempat membersihkan diri).
 */
object JCEFBootstrap {

    sealed class InitResult {
        data class Success(val client: CefClient) : InitResult()
        data class Failure(val message: String, val cause: Throwable) : InitResult()
    }

    @Volatile private var cefApp: CefApp? = null

    // Jaring pengaman untuk shutdown() -- lihat catatan kelas di atas.
    // synchronizedSet karena initialize()/disposeClient() bisa dipanggil dari
    // beberapa window (coroutine berbeda) hampir bersamaan.
    private val liveClients: MutableSet<CefClient> =
        java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(java.util.IdentityHashMap()))

    suspend fun initialize(
        onProgress: (String) -> Unit = {}
    ): InitResult = withContext(Dispatchers.IO) {
        try {
            val app = cefApp ?: run {
                val builder = CefAppBuilder()
                builder.setInstallDir(File(System.getProperty("user.home"), ".tradepilot/jcef-bundle"))
                builder.setProgressHandler { state, percent ->
                    onProgress("JCEF: $state ${if (percent >= 0) "${percent.toInt()}%" else ""}")
                }
                // Windowed rendering (bukan OSR/offscreen) -- lebih sederhana untuk
                // diintegrasikan lewat SwingPanel di Compose Desktop.
                builder.cefSettings.windowless_rendering_enabled = false
                // Cache Manager (item FASE 1 yang sebelumnya implisit -- cache_path
                // TIDAK di-set sama sekali, jadi lokasinya bergantung ke default
                // internal JCEF yang tidak eksplisit/predictable). Disamakan polanya
                // dengan install dir jcef-bundle di atas supaya SEMUA data JCEF app
                // ini hidup di satu tempat yang jelas (`~/.tradepilot/`), termasuk
                // untuk kebutuhan dukungan/debug ("folder mana yang harus dihapus
                // kalau mau reset total").
                //
                // CATATAN JUJUR: JCEF versi ini (146.0.10, sudah diverifikasi lewat
                // `jar tf` -- tidak ada satupun class "Cache" di jar API-nya) TIDAK
                // expose method untuk clear HTTP cache secara terprogram (beda
                // dengan cookie yang punya CefCookieManager). Jadi `cache_path` di
                // sini BARU menyelesaikan "lokasinya jelas & persisten", BELUM
                // menyelesaikan "ada tombol buat mengosongkannya" -- lihat catatan
                // di JCEFBrowserEngine.clearBrowsingData() untuk detail gap-nya.
                val cacheDir = File(System.getProperty("user.home"), ".tradepilot/jcef-cache")
                cacheDir.mkdirs()
                builder.cefSettings.cache_path = cacheDir.absolutePath
                val built = builder.build()
                cefApp = built
                built
            }
            // Client BARU per panggilan -- lihat "BUG DITEMUKAN" di atas.
            val client = app.createClient()
            liveClients.add(client)
            InitResult.Success(client)
        } catch (e: Exception) {
            InitResult.Failure(
                message = "Gagal inisialisasi JCEF: ${e.message ?: e::class.simpleName}. " +
                    "Pastikan koneksi internet aktif untuk unduhan pertama kali, dan cek " +
                    "apakah JDK yang dipakai punya dukungan AWT/Swing penuh (bukan headless).",
                cause = e
            )
        }
    }

    /**
     * Dipanggil oleh [com.tradepilot.desktop.browser.JCEFBrowserEngine.dispose]
     * saat sebuah window/tab ditutup, supaya client itu tidak menunggu sampai
     * seluruh aplikasi keluar untuk dibersihkan (mencegah client menumpuk
     * kalau user buka-tutup banyak window dalam satu sesi).
     */
    fun disposeClient(client: CefClient) {
        if (liveClients.remove(client)) {
            client.dispose()
        }
    }

    fun shutdown() {
        synchronized(liveClients) {
            liveClients.forEach { it.dispose() }
            liveClients.clear()
        }
        cefApp?.dispose()
        cefApp = null
    }
}
