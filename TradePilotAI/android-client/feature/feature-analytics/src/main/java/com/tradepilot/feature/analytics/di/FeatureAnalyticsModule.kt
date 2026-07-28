package com.tradepilot.feature.analytics.di

import com.tradepilot.feature.analytics.StatisticViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureAnalyticsModule = module {
    viewModel { StatisticViewModel(get(), get()) }
}
