package com.tradepilot.feature.browser.di

import com.tradepilot.feature.browser.BrowserViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureBrowserModule = module {
    viewModel { BrowserViewModel() }
}
