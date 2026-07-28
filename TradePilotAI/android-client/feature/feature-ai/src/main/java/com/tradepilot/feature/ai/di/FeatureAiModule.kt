package com.tradepilot.feature.ai.di

import com.tradepilot.feature.ai.AnalysisViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureAiModule = module {
    viewModel { AnalysisViewModel(get(), get()) }
}
