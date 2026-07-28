package com.tradepilot.data.trading

import com.tradepilot.domain.repository.TradeJournalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {

    @Binds
    @Singleton
    abstract fun bindTradeJournalRepository(impl: TradeJournalRepositoryImpl): TradeJournalRepository
}
