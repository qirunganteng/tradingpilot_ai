package com.tradepilot.domain.repository

import com.tradepilot.domain.model.AccountInfo
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeEntry
import kotlinx.coroutines.flow.Flow

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
}
