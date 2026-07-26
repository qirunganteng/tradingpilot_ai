package com.tradepilot.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PendingAnalysisHolder @Inject constructor() {
    private val _pendingImage = MutableStateFlow<ByteArray?>(null)
    val pendingImage: StateFlow<ByteArray?> = _pendingImage.asStateFlow()

    fun submit(byteArray: ByteArray) {
        _pendingImage.value = byteArray
    }

    fun clear() {
        _pendingImage.value = null
    }
}
