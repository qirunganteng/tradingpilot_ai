package com.tradepilot.feature.notification.di

import com.tradepilot.feature.notification.CopilotMonitorViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureNotificationModule = module {
    viewModel { CopilotMonitorViewModel(get(), get(), get(), get()) }
}
