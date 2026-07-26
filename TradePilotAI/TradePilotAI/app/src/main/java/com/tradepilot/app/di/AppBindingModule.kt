package com.tradepilot.app.di

import com.tradepilot.app.webview.AppChartSnapshotProvider
import com.tradepilot.domain.repository.ChartSnapshotProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingModule {
    @Binds
    @Singleton
    abstract fun bindChartSnapshotProvider(impl: AppChartSnapshotProvider): ChartSnapshotProvider
}
