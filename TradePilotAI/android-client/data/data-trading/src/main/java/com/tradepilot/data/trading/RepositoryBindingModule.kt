package com.tradepilot.data.trading

import com.tradepilot.domain.repository.TradeJournalRepository
import org.koin.dsl.module

val dataTradingModule = module {
    single<TradeJournalRepository> { TradeJournalRepositoryImpl(get()) }
}
