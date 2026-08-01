package com.tradepilot.desktop.explorer

import androidx.compose.runtime.mutableStateListOf

/**
 * Penyimpanan Bookmark (Prioritas 3), IN-MEMORY saja -- lihat catatan sama
 * di HistoryStore.kt soal persistence.
 */
object BookmarkStore {
    private val _entries = mutableStateListOf<BookmarkEntry>()
    val entries: List<BookmarkEntry> get() = _entries

    fun isBookmarked(url: String): Boolean = _entries.any { it.url == url }

    fun toggle(url: String, title: String) {
        if (url.isBlank()) return
        val existingIndex = _entries.indexOfFirst { it.url == url }
        if (existingIndex >= 0) {
            _entries.removeAt(existingIndex)
        } else {
            _entries.add(0, BookmarkEntry(url = url, title = title.ifBlank { url }))
        }
    }

    fun remove(url: String) {
        _entries.removeAll { it.url == url }
    }
}
