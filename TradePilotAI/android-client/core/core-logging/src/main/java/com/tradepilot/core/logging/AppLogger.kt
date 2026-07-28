package com.tradepilot.core.logging

import timber.log.Timber

/**
 * Satu titik masuk logging untuk seluruh app (dipanggil dari
 * TradePilotApplication.onCreate). Memisahkan tree Debug vs Release
 * sesuai Blueprint bagian 15: Logging.
 */
object AppLogger {
    fun init(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}

/**
 * Release tree: saat ini hanya no-op untuk log verbose/debug.
 * Titik ekstensi untuk mengirim crash/log penting ke backend
 * (opsional, lihat Blueprint 16: Deployment Strategy) tanpa
 * mengubah pemanggil Timber.d/.e di seluruh app.
 */
private class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // TODO: kirim ke crash/log backend jika kebijakan privasi mengizinkan.
        // Untuk sekarang: sengaja tidak melakukan apa-apa di release build.
    }
}
