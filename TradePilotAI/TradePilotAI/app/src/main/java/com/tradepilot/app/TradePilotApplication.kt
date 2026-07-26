package com.tradepilot.app

import android.app.Application
import com.tradepilot.app.BuildConfig
import com.tradepilot.core.logging.AppLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TradePilotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(isDebug = BuildConfig.DEBUG)
    }
}
