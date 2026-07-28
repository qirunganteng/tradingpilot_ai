package com.tradepilot.core.database

import androidx.room.Room
import com.tradepilot.core.database.dao.TradeHistoryDao
import com.tradepilot.core.database.dao.NotificationLogDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreDatabaseModule = module {

    single {
        Room.databaseBuilder(androidContext(), TradePilotDatabase::class.java, "tradepilot.db")
            .fallbackToDestructiveMigration() // Fase 0-5: skema masih berubah cepat; ganti ke migration eksplisit sebelum rilis
            .build()
    }

    single<TradeHistoryDao> { get<TradePilotDatabase>().tradeHistoryDao() }

    single<NotificationLogDao> { get<TradePilotDatabase>().notificationLogDao() }
}
