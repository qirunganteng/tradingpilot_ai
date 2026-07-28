package com.tradepilot.domain.repository

/**
 * Abstraksi pengambilan screenshot chart saat ini. Implementasi nyata
 * (memakai WebView + ScreenCapture) disediakan di app module supaya
 * feature-notification TIDAK perlu bergantung langsung ke feature-browser
 * atau feature-screenshot (menjaga independensi modul, Blueprint bagian 0).
 */
interface ChartSnapshotProvider {
    /** null jika WebView belum siap / belum ada chart untuk di-capture. */
    suspend fun captureCurrentChart(): ByteArray?
}
