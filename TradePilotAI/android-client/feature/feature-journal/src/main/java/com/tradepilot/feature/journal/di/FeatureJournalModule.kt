package com.tradepilot.feature.journal.di

import com.tradepilot.feature.journal.AddTradeViewModel
import com.tradepilot.feature.journal.JournalViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureJournalModule = module {
    viewModel { JournalViewModel(get(), get()) }
    viewModel { AddTradeViewModel(get()) }
}
