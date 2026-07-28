package com.tradepilot.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.core.security.SecureKeyStore
import com.tradepilot.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apiKeySaved = MutableStateFlow(secureKeyStore.getGeminiApiKey() != null)
    val apiKeySaved: StateFlow<Boolean> = _apiKeySaved

    private val _workerConfigured = MutableStateFlow(secureKeyStore.getWorkerBaseUrl() != null)
    val workerConfigured: StateFlow<Boolean> = _workerConfigured

    private val _workerBaseUrl = MutableStateFlow(secureKeyStore.getWorkerBaseUrl() ?: "")
    val workerBaseUrl: StateFlow<String> = _workerBaseUrl

    private val _riskPercentDefault = MutableStateFlow(1.0)
    val riskPercentDefault: StateFlow<Double> = _riskPercentDefault

    init {
        viewModelScope.launch {
            _riskPercentDefault.value = settingsRepository.getRiskPercentDefault()
        }
    }

    fun saveWorkerConfig(baseUrl: String, gatewayToken: String) {
        if (baseUrl.isBlank() || gatewayToken.isBlank()) return
        secureKeyStore.saveWorkerConfig(baseUrl.trim(), gatewayToken.trim())
        _workerConfigured.value = true
        _workerBaseUrl.value = baseUrl.trim()
    }

    fun clearWorkerConfig() {
        secureKeyStore.clearWorkerConfig()
        _workerConfigured.value = false
        _workerBaseUrl.value = ""
    }

    fun saveApiKey(key: String) {
        if (key.isBlank()) return
        secureKeyStore.saveGeminiApiKey(key.trim())
        _apiKeySaved.value = true
    }

    fun clearApiKey() {
        secureKeyStore.clearGeminiApiKey()
        _apiKeySaved.value = false
    }

    fun saveRiskPercentDefault(value: Double) {
        viewModelScope.launch {
            settingsRepository.setRiskPercentDefault(value)
            _riskPercentDefault.value = value
        }
    }
}
