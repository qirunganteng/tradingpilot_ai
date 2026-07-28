package com.tradepilot.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.domain.usecase.CalculateJournalStatisticsUseCase
import com.tradepilot.domain.usecase.JournalStatistics
import com.tradepilot.domain.usecase.ObserveTradeHistoryUseCase
import com.tradepilot.domain.model.TradeEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JournalViewModel constructor(
    observeTradeHistoryUseCase: ObserveTradeHistoryUseCase,
    private val calculateJournalStatisticsUseCase: CalculateJournalStatisticsUseCase
) : ViewModel() {

    private val _trades = MutableStateFlow<List<TradeEntry>>(emptyList())
    val trades: StateFlow<List<TradeEntry>> = _trades

    private val _statistics = MutableStateFlow(CalculateJournalStatisticsUseCase().invoke(emptyList()))
    val statistics: StateFlow<JournalStatistics> = _statistics

    init {
        observeTradeHistoryUseCase()
            .onEach { list ->
                _trades.value = list
                _statistics.value = calculateJournalStatisticsUseCase(list)
            }
            .launchIn(viewModelScope)
    }
}
