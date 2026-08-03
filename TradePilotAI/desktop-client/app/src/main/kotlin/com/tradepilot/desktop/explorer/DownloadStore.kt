package com.tradepilot.desktop.explorer

import androidx.compose.runtime.mutableStateListOf

/**
 * FASE 3 (Download Manager) -- penyimpanan IN-MEMORY (sama seperti
 * HistoryStore.kt, hilang tiap restart app -- tidak masalah untuk downloads
 * karena file-nya sendiri sudah permanen di disk, cuma daftar UI-nya yang
 * reset). Diisi oleh CefDownloadHandler di JCEFBrowserEngine.kt.
 *
 * Dikunci per-tab TIDAK dilakukan (downloads global lintas semua tab/window
 * yang berbagi proses app yang sama) -- ini perilaku wajar Download Manager
 * di browser sungguhan (satu daftar downloads untuk seluruh browser, bukan
 * per-tab).
 */
object DownloadStore {
    private val _entries = mutableStateListOf<DownloadEntry>()
    val entries: List<DownloadEntry> get() = _entries

    /** Dipanggil dari `onBeforeDownload` (CefDownloadHandler) begitu unduhan baru dimulai. */
    fun start(id: Int, fileName: String, url: String, fullPath: String) {
        if (_entries.any { it.id == id }) return // guard duplikat kalau onBeforeDownload sempat terpanggil 2x untuk id sama
        _entries.add(0, DownloadEntry(id = id, fileName = fileName, url = url, fullPath = fullPath, progressPercent = 0, isComplete = false))
    }

    /** Dipanggil dari `onDownloadUpdated` (CefDownloadHandler) berulang kali selama unduhan berjalan. */
    fun update(id: Int, progressPercent: Int, isComplete: Boolean, isCanceled: Boolean) {
        val index = _entries.indexOfFirst { it.id == id }
        if (index < 0) return
        _entries[index] = _entries[index].copy(
            progressPercent = progressPercent,
            isComplete = isComplete,
            isCanceled = isCanceled
        )
    }

    fun clear() {
        _entries.clear()
    }
}
