package com.tradepilot.app

import android.app.Application
import com.tradepilot.app.BuildConfig
import com.tradepilot.core.logging.AppLogger
import dagger.hilt.android.HiltAndroidApp

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
    }
}
