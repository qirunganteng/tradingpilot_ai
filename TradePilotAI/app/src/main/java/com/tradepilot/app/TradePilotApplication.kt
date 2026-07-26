package com.tradepilot.app

import android.app.Application
import com.tradepilot.app.crash.CrashActivity
import com.tradepilot.core.logging.AppLogger
import dagger.hilt.android.HiltAndroidApp
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Application root. Inisialisasi logging global di sini.
 * Tidak boleh menaruh logika bisnis apapun di kelas ini —
 * hanya bootstrap infrastruktur (logging, crash reporting, dsb).
 */
@HiltAndroidApp
class TradePilotApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(isDebug = BuildConfig.DEBUG)
        installCrashHandler()
    }

    /**
     * DIAGNOSTIK SEMENTARA: alih-alih app diam-diam ketutup saat crash
     * (perilaku default Android, sulit didiagnosis tanpa ADB/logcat),
     * tangkap exception apapun yang lolos, lalu tampilkan stack trace
     * lengkap di layar lewat CrashActivity. Cukup screenshot layar itu.
     *
     * TODO: hapus/nonaktifkan mekanisme ini sebelum rilis publik (cukup
     * untuk tahap debugging awal, bukan pengalaman yang pantas dilihat
     * user biasa).
     */
    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                CrashActivity.start(applicationContext, sw.toString())
            } catch (e: Exception) {
                // Kalau bahkan CrashActivity gagal diluncurkan, jangan sampai
                // handler ini sendiri yang bikin loop crash -- lanjut ke
                // default handler seperti biasa.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}