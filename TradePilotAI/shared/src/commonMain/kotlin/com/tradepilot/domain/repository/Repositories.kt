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

/**
 * Fase 6: Risk Engine sumber kebenaran dipindah ke Backend (lihat catatan
 * di CalculateRiskRequestBody, backend/cloudflare-worker/src/types.ts).
 * Interface ini adalah kontraknya di sisi client -- implementasi konkret
 * (HTTP call ke /api/v1/calculate-risk) HARUS platform-specific karena
 * shared/commonMain sengaja tidak boleh depend ke library HTTP apa pun
 * (lihat catatan "shared HARUS pure Kotlin" di shared/build.gradle.kts).
 *
 * - desktop-client: implementasi ada di shared/desktopMain (java.net.http,
 *   tersedia langsung dari JDK, tanpa dependency tambahan).
 * - android-client: implementasi (belum ada) sebaiknya taruh di
 *   data-ai memakai Retrofit yang sudah dipakai AIRepository di sana.
 *
 * CalculateRiskUseCase (RiskUseCases.kt) TETAP ada dan tetap dipakai --
 * bukan sebagai sumber kebenaran, tapi untuk instant local preview
 * (optimistic UI) selagi menunggu response dari gateway ini.
 */
interface RiskGatewayRepository {
    suspend fun calculateRisk(
        balance: Double,
        riskPercent: Double,
        entryPrice: Double,
        stopLossPrice: Double,
        takeProfitPrice: Double,
        deviceId: String,
        pipValuePerLotUsd: Double? = null,
        pipSize: Double? = null
    ): Result<RiskRecommendation>
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
