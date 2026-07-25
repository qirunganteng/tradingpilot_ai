package com.tradepilot.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.domain.model.TradeDirection
import com.tradepilot.domain.model.TradeEntry
import com.tradepilot.domain.usecase.SaveTradeEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mengisi celah fungsional: sebelumnya tidak ada cara mengisi Trading
 * Journal sama sekali (SaveTradeEntryUseCase ada di domain tapi tidak
 * dipanggil dari mana pun). Form manual ini jadi cara utama sampai nanti
 * ada integrasi otomatis dari histori Exness (di luar scope saat ini).
 */
@HiltViewModel
class AddTradeViewModel @Inject constructor(
    private val saveTradeEntryUseCase: SaveTradeEntryUseCase
) : ViewModel() {

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun saveTrade(
        pair: String,
        direction: TradeDirection,
        entry: Double,
        exit: Double,
        stopLoss: Double,
        takeProfit: Double,
        lot: Double,
        profitLoss: Double,
        riskRewardRatio: Double,
        balanceAfter: Double,
        notes: String
    ) {
        viewModelScope.launch {
            saveTradeEntryUseCase(
                TradeEntry(
                    pair = pair,
                    direction = direction,
                    entry = entry,
                    exit = exit,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit,
                    lot = lot,
                    profitLoss = profitLoss,
                    riskRewardRatio = riskRewardRatio,
                    balanceAfter = balanceAfter,
                    timestampMillis = System.currentTimeMillis(),
                    notes = notes
                )
            )
            _saved.value = true
        }
    }

    fun resetSaved() {
        _saved.value = false
    }
}
