package com.tradepilot.domain.repository

import com.tradepilot.domain.model.AccountInfo
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeEntry
import kotlinx.coroutines.flow.Flow

/**
 * Interface repository di domain layer — implementasi konkret ada di
 * modul data-* (Dependency Inversion). feature-* hanya bergantung ke
 * interface ini, tidak pernah ke implementasi langsung.
 */

interface AIRepository {
    suspend fun analyzeChart(imageBytes: ByteArray, methods: List<String>): Result<AnalysisResult>
}

interface AccountRepository {
    fun observeAccountInfo(): Flow<AccountInfo>
}

interface TradeJournalRepository {
    suspend fun save(entry: TradeEntry)
    fun observeHistory(): Flow<List<TradeEntry>>
}

interface SettingsRepository {
    suspend fun getRiskPercentDefault(): Double
    suspend fun setRiskPercentDefault(value: Double)
    // TODO: bahasa (ID/EN), tema, AI provider aktif, timeframe, dst.
}
