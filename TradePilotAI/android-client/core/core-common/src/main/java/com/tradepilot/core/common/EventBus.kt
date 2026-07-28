package com.tradepilot.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class EventBus() {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events

    suspend fun publish(event: AppEvent) {
        _events.emit(event)
    }
}
