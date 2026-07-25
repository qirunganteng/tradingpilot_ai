package com.tradepilot.feature.trading

import androidx.lifecycle.ViewModel
import com.tradepilot.domain.model.RiskRecommendation
import com.tradepilot.domain.usecase.CalculateRiskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Fase 4: Balance/Equity saat ini diisi MANUAL oleh user (lihat catatan
 * keterbatasan di README/blueprint — membaca DOM Exness butuh JS-bridge
 * yang fragile). Field ini disiapkan agar gampang diganti ke
 * AccountRepository.observeAccountInfo() jika nanti ada sumber data otomatis.
 */
@HiltViewModel
class MoneyManagementViewModel @Inject constructor(
    private val calculateRiskUseCase: CalculateRiskUseCase
) : ViewModel() {

    private val _result = MutableStateFlow<RiskRecommendation?>(null)
    val result: StateFlow<RiskRecommendation?> = _result

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun calculate(
        balance: Double,
        riskPercent: Double,
        entryPrice: Double,
        stopLossPrice: Double,
        takeProfitPrice: Double
    ) {
        try {
            _errorMessage.value = null
            _result.value = calculateRiskUseCase(
                balance = balance,
                riskPercent = riskPercent,
                entryPrice = entryPrice,
                stopLossPrice = stopLossPrice,
                takeProfitPrice = takeProfitPrice
            )
        } catch (e: IllegalArgumentException) {
            _errorMessage.value = e.message
        }
    }
}
