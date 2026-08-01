package com.tradepilot.desktop.duplicate.browserpanel

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
 *  - `initialize()` idempoten (aman dipanggil berkali-kali dari Compose
 *    recomposition -- cuma proses pertama yang benar-benar membangun CefApp).
 *  - WAJIB panggil `shutdown()` sebelum aplikasi keluar (lihat `main()` di
 *    Main.kt) -- kalau tidak, proses native CEF child bisa tertinggal
 *    jalan di background (zombie process) setelah window ditutup.
 */
object JCEFBootstrap {

    sealed class InitResult {
        data class Success(val client: CefClient) : InitResult()
        data class Failure(val message: String, val cause: Throwable) : InitResult()
    }

    @Volatile private var cefApp: CefApp? = null
    @Volatile private var cefClient: CefClient? = null

    suspend fun initialize(
        onProgress: (String) -> Unit = {}
    ): InitResult = withContext(Dispatchers.IO) {
        cefClient?.let { return@withContext InitResult.Success(it) }

        try {
            val builder = CefAppBuilder()
            builder.setInstallDir(File(System.getProperty("user.home"), ".tradepilot/jcef-bundle"))
            builder.setProgressHandler { state, percent ->
                onProgress("JCEF: $state ${if (percent >= 0) "${percent.toInt()}%" else ""}")
            }
            // Windowed rendering (bukan OSR/offscreen) -- lebih sederhana untuk
            // diintegrasikan lewat SwingPanel di Compose Desktop.
            builder.cefSettings.windowless_rendering_enabled = false

            val app = builder.build()
            cefApp = app
            val client = app.createClient()
            cefClient = client
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

    fun shutdown() {
        cefClient?.dispose()
        cefApp?.dispose()
        cefClient = null
        cefApp = null
    }
}
