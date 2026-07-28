package com.tradepilot.data.user

import com.tradepilot.domain.repository.SettingsRepository

/**
 * Fase 0: default value sederhana. Fase feature-settings akan mengganti
 * dengan penyimpanan persisten (DataStore/Room `settings` table sesuai
 * skema di Blueprint bagian 5).
 */
class SettingsRepositoryImpl constructor() : SettingsRepository {

    private var riskPercentDefault: Double = 1.0 // default aman: 1% per trade

    override suspend fun getRiskPercentDefault(): Double = riskPercentDefault

    override suspend fun setRiskPercentDefault(value: Double) {
        riskPercentDefault = value
    }
}
