package com.tradepilot.core.database

import android.content.Context
import androidx.room.Room
import com.tradepilot.core.database.dao.TradeHistoryDao
import com.tradepilot.core.database.dao.NotificationLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TradePilotDatabase =
        Room.databaseBuilder(context, TradePilotDatabase::class.java, "tradepilot.db")
            .fallbackToDestructiveMigration() // Fase 0-5: skema masih berubah cepat; ganti ke migration eksplisit sebelum rilis
            .build()

    @Provides
    @Singleton
    fun provideTradeHistoryDao(db: TradePilotDatabase): TradeHistoryDao = db.tradeHistoryDao()

    @Provides
    @Singleton
    fun provideNotificationLogDao(db: TradePilotDatabase): NotificationLogDao = db.notificationLogDao()
}
