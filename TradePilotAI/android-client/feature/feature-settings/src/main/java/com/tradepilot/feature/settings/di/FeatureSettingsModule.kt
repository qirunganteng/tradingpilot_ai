package com.tradepilot.feature.settings.di

import com.tradepilot.feature.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureSettingsModule = module {
    viewModel { SettingsViewModel(get(), get()) }
}
