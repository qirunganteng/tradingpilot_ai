package com.tradepilot.feature.ai

import com.tradepilot.domain.model.AnalysisResult

sealed class AnalysisUiState {
    data object Idle : AnalysisUiState()
    data object Loading : AnalysisUiState()
    data class Success(val result: AnalysisResult) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}
