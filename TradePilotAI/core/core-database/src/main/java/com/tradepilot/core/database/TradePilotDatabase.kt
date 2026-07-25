package com.tradepilot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tradepilot.core.database.entity.AIAnalysisEntity
import com.tradepilot.core.database.entity.JournalEntryEntity
import com.tradepilot.core.database.entity.NotificationLogEntity
import com.tradepilot.core.database.entity.RiskHistoryEntity
import com.tradepilot.core.database.entity.ScreenshotEntity
import com.tradepilot.core.database.entity.SettingsEntity
import com.tradepilot.core.database.entity.TradeHistoryEntity
import com.tradepilot.core.database.dao.TradeHistoryDao
import com.tradepilot.core.database.dao.NotificationLogDao

/**
 * Room database tunggal. DAO ditambahkan bertahap per fase feature
 * (lihat Blueprint bagian 14: Roadmap) supaya class ini tidak
 * membengkak sebelum waktunya.
 */
@Database(
    entities = [
        TradeHistoryEntity::class,
        AIAnalysisEntity::class,
        JournalEntryEntity::class,
        ScreenshotEntity::class,
        RiskHistoryEntity::class,
        SettingsEntity::class,
        NotificationLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TradePilotDatabase : RoomDatabase() {
    abstract fun tradeHistoryDao(): TradeHistoryDao
    abstract fun notificationLogDao(): NotificationLogDao
    // TODO fase berikutnya: abstract fun aiAnalysisDao(), journalEntryDao(), dst. saat dibutuhkan.
}
