package com.tradepilot.feature.screenshot.di

import com.tradepilot.feature.screenshot.ScreenCapture
import org.koin.dsl.module

val featureScreenshotModule = module {
    single { ScreenCapture() }
}
