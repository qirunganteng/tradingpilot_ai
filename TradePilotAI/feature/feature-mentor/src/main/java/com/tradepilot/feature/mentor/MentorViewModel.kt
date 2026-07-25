package com.tradepilot.feature.mentor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.domain.model.MentorFeedback
import com.tradepilot.domain.model.TradeEntry
import com.tradepilot.domain.usecase.GenerateMentorFeedbackUseCase
import com.tradepilot.domain.usecase.ObserveTradeHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MentorViewModel @Inject constructor(
    observeTradeHistoryUseCase: ObserveTradeHistoryUseCase,
    private val generateMentorFeedbackUseCase: GenerateMentorFeedbackUseCase
) : ViewModel() {

    data class TradeWithFeedback(val trade: TradeEntry, val feedback: MentorFeedback)

    private val _items = MutableStateFlow<List<TradeWithFeedback>>(emptyList())
    val items: StateFlow<List<TradeWithFeedback>> = _items

    init {
        observeTradeHistoryUseCase()
            .onEach { trades ->
                _items.value = trades.map { TradeWithFeedback(it, generateMentorFeedbackUseCase(it)) }
            }
            .launchIn(viewModelScope)
    }
}
