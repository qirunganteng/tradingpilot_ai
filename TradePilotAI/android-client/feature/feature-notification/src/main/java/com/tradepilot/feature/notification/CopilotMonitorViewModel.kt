package com.tradepilot.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradepilot.core.common.AppEvent
import com.tradepilot.core.common.EventBus
import com.tradepilot.core.database.dao.NotificationLogDao
import com.tradepilot.core.database.entity.NotificationLogEntity
import com.tradepilot.domain.repository.ChartSnapshotProvider
import com.tradepilot.domain.usecase.AnalyzeChartUseCase
import com.tradepilot.domain.usecase.DeriveCopilotSignalUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AI Copilot (versi 4). KEPUTUSAN TERKUNCI: hanya aktif SELAMA app dibuka
 * (bukan background service) — polling dihentikan otomatis saat
 * ViewModel di-clear (mis. user pindah dari screen/app di-kill).
 */
class CopilotMonitorViewModel constructor(
    private val chartSnapshotProvider: ChartSnapshotProvider,
    private val analyzeChartUseCase: AnalyzeChartUseCase,
    private val eventBus: EventBus,
    private val notificationLogDao: NotificationLogDao
) : ViewModel() {

    private var monitorJob: Job? = null

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    val logs: StateFlow<List<NotificationLogEntity>> =
        notificationLogDao.observeRecent()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun startMonitoring(intervalMillis: Long = 60_000L) {
        if (_isMonitoring.value) return
        _isMonitoring.value = true
        monitorJob = viewModelScope.launch {
            while (true) {
                runCatching {
                    val bytes = chartSnapshotProvider.captureCurrentChart() ?: return@runCatching
                    val result = analyzeChartUseCase(bytes).getOrNull() ?: return@runCatching
                    DeriveCopilotSignalUseCase.invoke(result).forEach { (category, message) ->
                        eventBus.publish(AppEvent.MarketSignalDetected(result.pair, category, message))
                        notificationLogDao.insert(
                            NotificationLogEntity(
                                message = message,
                                category = category,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                delay(intervalMillis)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _isMonitoring.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring() // jaminan tambahan: tidak ada polling yang lanjut di background
    }
}
