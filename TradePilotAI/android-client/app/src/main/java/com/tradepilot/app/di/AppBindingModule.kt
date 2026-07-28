package com.tradepilot.app.di

import com.tradepilot.app.webview.AppChartSnapshotProvider
import com.tradepilot.app.webview.AppRootViewModel
import com.tradepilot.app.webview.CurrentWebViewHolder
import com.tradepilot.domain.repository.ChartSnapshotProvider
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CurrentWebViewHolder() }
    single<ChartSnapshotProvider> { AppChartSnapshotProvider(webViewHolder = get(), screenCapture = get()) }
    viewModel { AppRootViewModel(screenCapture = get(), pendingAnalysisHolder = get(), currentWebViewHolder = get()) }
}
