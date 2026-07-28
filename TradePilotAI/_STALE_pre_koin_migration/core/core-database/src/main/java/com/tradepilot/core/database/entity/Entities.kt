package com.tradepilot.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity Room mengikuti skema di Blueprint bagian 5 (Database Schema).
 * Fase 0: definisi struktur tabel saja, DAO/relasi lengkap menyusul
 * di fase feature masing-masing (Journal, AI Analysis, dst).
 */

@Entity(
    tableName = "trade_history",
    indices = [Index(value = ["timestamp"])] // OPTIMASI: dipakai ORDER BY timestamp DESC di observeAll()
)
data class TradeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pair: String,
    val direction: String, // "BUY" | "SELL"
    val entry: Double,
    val exit: Double,
    val sl: Double,
    val tp: Double,
    val lot: Double,
    val profitLoss: Double,
    val rr: Double,
    val balanceAfter: Double,
    val timestamp: Long,
    val notes: String = "",
    val mentorFeedback: String = ""
)

@Entity(tableName = "ai_analysis")
data class AIAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeHistoryId: Long?,
    val method: String, // "ICT" | "SMC" | ...
    val trend: String,
    val signal: String, // "BUY" | "SELL" | "NONE"
    val confidence: Double,
    val entry: String,
    val sl: String,
    val tp: String,
    val rr: String,
    val reasoning: String,
    val providerUsed: String,
    val timestamp: Long
)

@Entity(tableName = "journal_entry")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeHistoryId: Long,
    val whatWentRight: String,
    val whatWentWrong: String,
    val improvement: String,
    val timestamp: Long
)

@Entity(tableName = "screenshot")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeHistoryId: Long?,
    val filePathOriginal: String,
    val filePathAnnotated: String?,
    val timestamp: Long
)

@Entity(tableName = "risk_history")
data class RiskHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val balanceAtTime: Double,
    val riskPercent: Double,
    val lotSizeCalculated: Double,
    val maxDailyLoss: Double,
    val timestamp: Long
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(
    tableName = "notification_log",
    indices = [Index(value = ["timestamp"])] // OPTIMASI: dipakai ORDER BY timestamp DESC di observeRecent()
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val category: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
