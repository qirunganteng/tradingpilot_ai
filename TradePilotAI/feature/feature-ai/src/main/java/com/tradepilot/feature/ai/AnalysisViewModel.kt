package com.tradepilot.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.core.common.PendingAnalysisHolder
import com.tradepilot.domain.usecase.AnalyzeChartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analyzeChartUseCase: AnalyzeChartUseCase,
    private val pendingAnalysisHolder: PendingAnalysisHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    /** Dipanggil sekali saat AnalysisScreen muncul, untuk konsumsi hasil capture dari BrowserScreen. */
    fun consumePendingImageIfAny() {
        val bytes = pendingAnalysisHolder.consume() ?: return
        analyze(bytes)
    }

    fun analyze(imageBytes: ByteArray) {
        _uiState.value = AnalysisUiState.Loading
        viewModelScope.launch {
            analyzeChartUseCase(imageBytes)
                .onSuccess { _uiState.value = AnalysisUiState.Success(it) }
                .onFailure { _uiState.value = AnalysisUiState.Error(it.message ?: "Analisa gagal") }
        }
    }

    fun reset() {
        _uiState.value = AnalysisUiState.Idle
    }
}
