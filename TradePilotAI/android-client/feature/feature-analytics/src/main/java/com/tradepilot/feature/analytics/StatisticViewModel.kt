package com.tradepilot.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.domain.usecase.GenerateHistoryInsightUseCase
import com.tradepilot.domain.usecase.HistoryInsight
import com.tradepilot.domain.usecase.ObserveTradeHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class StatisticViewModel constructor(
    observeTradeHistoryUseCase: ObserveTradeHistoryUseCase,
    private val generateHistoryInsightUseCase: GenerateHistoryInsightUseCase
) : ViewModel() {

    private val _insight = MutableStateFlow(generateHistoryInsightUseCase(emptyList()))
    val insight: StateFlow<HistoryInsight> = _insight

    init {
        observeTradeHistoryUseCase()
            .onEach { trades -> _insight.value = generateHistoryInsightUseCase(trades) }
            .launchIn(viewModelScope)
    }
}
