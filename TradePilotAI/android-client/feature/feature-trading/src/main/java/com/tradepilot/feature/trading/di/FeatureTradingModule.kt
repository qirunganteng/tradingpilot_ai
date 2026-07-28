package com.tradepilot.feature.trading.di

import com.tradepilot.feature.trading.MoneyManagementViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureTradingModule = module {
    viewModel { MoneyManagementViewModel(get()) }
}
