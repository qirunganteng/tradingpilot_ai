package com.tradepilot.desktop.explorer

import androidx.compose.runtime.mutableStateListOf

/**
 * Penyimpanan History (Prioritas 3), IN-MEMORY saja (hilang setiap restart
 * aplikasi) -- tidak ada database/file persistence di paket file browser
 * ini (README asli sengaja tidak menyertakan layer data/DB). Kalau butuh
 * history yang persisten antar sesi, ini tempat paling gampang untuk
 * disambungkan ke penyimpanan sungguhan (mis. Room/SQLDelight yang sudah
 * dipakai modul lain di monorepo) -- cukup ganti mutableStateListOf di
 * bawah dengan yang baca/tulis dari situ, API publiknya (record/all/clear)
 * tidak perlu berubah.
 */
object HistoryStore {
    private val _entries = mutableStateListOf<HistoryEntry>()
    val entries: List<HistoryEntry> get() = _entries

    private const val MAX_ENTRIES = 500

    fun record(url: String, title: String) {
        if (url.isBlank()) return
        // Hindari duplikat berturut-turut (mis. title berubah dikit tapi URL
        // sama karena SPA/JS navigation) -- update entry terakhir saja.
        val last = _entries.lastOrNull()
        if (last != null && last.url == url) {
            _entries[_entries.lastIndex] = last.copy(title = title.ifBlank { last.title })
            return
        }
        _entries.add(HistoryEntry(url = url, title = title.ifBlank { url }, visitedAtEpochMillis = System.currentTimeMillis()))
        if (_entries.size > MAX_ENTRIES) _entries.removeAt(0)
    }

    fun clear() {
        _entries.clear()
    }
}
