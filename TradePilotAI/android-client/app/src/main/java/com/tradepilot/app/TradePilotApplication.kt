package com.tradepilot.app

import android.app.Application
import com.tradepilot.app.crash.CrashActivity
import com.tradepilot.app.di.appModule
import com.tradepilot.app.di.useCaseModule
import com.tradepilot.core.common.di.coreCommonModule
import com.tradepilot.core.database.coreDatabaseModule
import com.tradepilot.core.logging.AppLogger
import com.tradepilot.core.network.coreNetworkModule
import com.tradepilot.core.security.di.coreSecurityModule
import com.tradepilot.data.ai.dataAiGeminiModule
import com.tradepilot.data.ai.dataAiProviderModule
import com.tradepilot.data.ai.dataAiWorkerModule
import com.tradepilot.data.trading.dataTradingModule
import com.tradepilot.data.user.dataUserModule
import com.tradepilot.feature.ai.di.featureAiModule
import com.tradepilot.feature.analytics.di.featureAnalyticsModule
import com.tradepilot.feature.browser.di.featureBrowserModule
import com.tradepilot.feature.drawing.di.featureDrawingModule
import com.tradepilot.feature.journal.di.featureJournalModule
import com.tradepilot.feature.mentor.di.featureMentorModule
import com.tradepilot.feature.notification.di.featureNotificationModule
import com.tradepilot.feature.screenshot.di.featureScreenshotModule
import com.tradepilot.feature.settings.di.featureSettingsModule
import com.tradepilot.feature.trading.di.featureTradingModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Application root. Inisialisasi logging global & Koin (DI) di sini.
 * Tidak boleh menaruh logika bisnis apapun di kelas ini —
 * hanya bootstrap infrastruktur (logging, crash reporting, DI, dsb).
 *
 * Fase 2 (migrasi Hilt -> Koin): semua modul Koin dari tiap gradle module
 * didaftarkan di SATU tempat ini (composition root), karena :app adalah
 * satu-satunya module yang punya dependency ke semua module lain.
 */
class TradePilotApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(isDebug = BuildConfig.DEBUG)
        installCrashHandler()

        startKoin {
            androidLogger()
            androidContext(this@TradePilotApplication)
            modules(
                // core
                coreCommonModule,
                coreSecurityModule,
                coreNetworkModule,
                coreDatabaseModule,
                // data
                dataUserModule,
                dataTradingModule,
                dataAiGeminiModule,
                dataAiWorkerModule,
                dataAiProviderModule,
                // domain use case (lihat catatan di UseCaseModule.kt)
                useCaseModule,
                // feature
                featureScreenshotModule,
                featureDrawingModule,
                featureTradingModule,
                featureAnalyticsModule,
                featureSettingsModule,
                featureMentorModule,
                featureBrowserModule,
                featureAiModule,
                featureNotificationModule,
                featureJournalModule,
                // app (composition root sendiri)
                appModule,
            )
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = StringWriter().also { sw ->
                    throwable.printStackTrace(PrintWriter(sw))
                }.toString()
                CrashActivity.start(this, stackTrace)
            } catch (e: Exception) {
                // Jangan sampai crash handler sendiri crash -- fallback ke default.
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
