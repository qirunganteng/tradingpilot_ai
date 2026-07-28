package com.tradepilot.feature.browser

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * State minimal untuk BrowserScreen. Sengaja tipis — logika AI/screenshot
 * ada di feature-ai & feature-screenshot, dipanggil lewat callback
 * onAnalyzeRequested dari NavHost (lihat app/MainActivity di Fase 1 lanjutan).
 */
@HiltViewModel
class BrowserViewModel @Inject constructor() : ViewModel() {

    fun toggleFullscreen() {
        // TODO: expose event ke Activity untuk menyembunyikan status bar & Activity Bar
        // saat fullscreen. Disiapkan sebagai hook, belum wajib difungsikan di Fase 1.
    }
}
