package com.tradepilot.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Jembatan ringan antar screen (Browser -> Analysis): BrowserScreen
 * menaruh hasil capture JPEG di sini saat tombol ANALISA ditekan,
 * AnalysisScreen membacanya sekali via consume() lalu otomatis kosong.
 * Diletakkan di core-common (bukan app) supaya feature-ai bisa
 * menggunakannya tanpa bergantung ke modul app.
 */
@Singleton
class PendingAnalysisHolder @Inject constructor() {
    val pendingImage = MutableStateFlow<ByteArray?>(null)

    fun submit(bytes: ByteArray) {
        pendingImage.value = bytes
    }

    fun consume(): ByteArray? {
        val value = pendingImage.value
        pendingImage.value = null
        return value
    }
}
