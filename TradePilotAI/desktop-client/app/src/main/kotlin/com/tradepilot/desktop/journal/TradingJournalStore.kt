package com.tradepilot.desktop.journal

import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.util.UUID

enum class JournalDirection { BUY, SELL }
enum class TradeResult { PENDING, WIN, LOSS, BREAKEVEN }

data class TradingJournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val pair: String,
    val direction: JournalDirection,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val lotSize: Double,
    val result: TradeResult = TradeResult.PENDING,
    val notes: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

data class DaySummary(
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val pending: Int,
    val winRatePercent: Double
)

/**
 * Trading Journal -- PERSISTEN antar sesi (BEDA dengan HistoryStore.kt yang
 * sengaja in-memory-saja untuk riwayat browsing biasa). Catatan trade itu
 * data penting yang tidak boleh hilang cuma karena app di-restart/update,
 * jadi disimpan sebagai file di ~/.tradepilot/ (folder yang sama dengan
 * DesktopSettingsStore.kt & UpdatePreferences.kt).
 *
 * Format penyimpanan: JSON Lines (satu baris = satu JSON object flat) --
 * BUKAN satu array JSON besar. Alasan: parser JSON manual di codebase ini
 * (lihat UpdateManifest.kt, MinimalJson.kt) cuma menangani object flat
 * per-baris dengan regex per-field; mem-parsing array-of-object bersarang
 * butuh state-machine JSON parser sungguhan yang lebih rawan bug daripada
 * nilainya untuk kasus sesederhana ini. JSON Lines juga punya keuntungan
 * praktis: menambah entri baru = append satu baris (tidak perlu baca+tulis
 * ulang seluruh file), lebih murah untuk file yang terus bertambah.
 */
object TradingJournalStore {
    private val _entries = mutableStateListOf<TradingJournalEntry>()
    val entries: List<TradingJournalEntry> get() = _entries

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".tradepilot").apply { mkdirs() }
    }
    private val journalFile: File by lazy { File(configDir, "trading-journal.jsonl") }

    private var isLoaded = false

    fun ensureLoaded() {
        if (isLoaded) return
        isLoaded = true
        if (!journalFile.exists()) return
        try {
            journalFile.readLines().forEach { line ->
                if (line.isBlank()) return@forEach
                parseEntry(line)?.let { _entries.add(it) }
            }
        } catch (e: Exception) {
            // File korup/tidak bisa dibaca -- jangan crash aplikasi, mulai
            // dengan jurnal kosong daripada bikin app tidak bisa dibuka.
        }
    }

    fun add(entry: TradingJournalEntry) {
        ensureLoaded()
        _entries.add(0, entry)
        appendLine(entry)
    }

    fun updateResult(id: String, result: TradeResult) {
        val index = _entries.indexOfFirst { it.id == id }
        if (index < 0) return
        _entries[index] = _entries[index].copy(result = result)
        rewriteAll()
    }

    fun delete(id: String) {
        _entries.removeAll { it.id == id }
        rewriteAll()
    }

    fun todaySummary(): DaySummary {
        val today = java.time.LocalDate.now()
        val todayEntries = _entries.filter {
            val entryDate = java.time.Instant.ofEpochMilli(it.createdAtEpochMillis)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            entryDate == today
        }
        val wins = todayEntries.count { it.result == TradeResult.WIN }
        val losses = todayEntries.count { it.result == TradeResult.LOSS }
        val pending = todayEntries.count { it.result == TradeResult.PENDING }
        val closed = wins + losses
        val winRate = if (closed > 0) (wins.toDouble() / closed.toDouble()) * 100 else 0.0
        return DaySummary(totalTrades = todayEntries.size, wins = wins, losses = losses, pending = pending, winRatePercent = winRate)
    }

    private fun appendLine(entry: TradingJournalEntry) {
        try {
            journalFile.appendText(serialize(entry) + "\n")
        } catch (e: Exception) {
            // Gagal tulis ke disk -- entry TETAP ada di memori untuk sesi
            // ini, cuma tidak akan persist ke sesi berikutnya. Tidak crash.
        }
    }

    private fun rewriteAll() {
        try {
            val body = _entries.reversed().joinToString("\n") { serialize(it) }
            journalFile.writeText(if (_entries.isNotEmpty()) "$body\n" else "")
        } catch (e: Exception) {
            // Sama seperti appendLine -- gagal tulis tidak boleh crash app.
        }
    }

    private fun serialize(e: TradingJournalEntry): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return "{" +
            "\"id\":\"${esc(e.id)}\"," +
            "\"pair\":\"${esc(e.pair)}\"," +
            "\"direction\":\"${e.direction.name}\"," +
            "\"entryPrice\":${e.entryPrice}," +
            "\"stopLoss\":${e.stopLoss}," +
            "\"takeProfit\":${e.takeProfit}," +
            "\"lotSize\":${e.lotSize}," +
            "\"result\":\"${e.result.name}\"," +
            "\"notes\":\"${esc(e.notes)}\"," +
            "\"createdAtEpochMillis\":${e.createdAtEpochMillis}" +
            "}"
    }

    private fun parseEntry(json: String): TradingJournalEntry? {
        val id = stringField(json, "id") ?: return null
        val pair = stringField(json, "pair") ?: return null
        val direction = try { JournalDirection.valueOf(stringField(json, "direction") ?: "BUY") } catch (e: Exception) { JournalDirection.BUY }
        val entryPrice = numberField(json, "entryPrice") ?: return null
        val stopLoss = numberField(json, "stopLoss") ?: return null
        val takeProfit = numberField(json, "takeProfit") ?: return null
        val lotSize = numberField(json, "lotSize") ?: 0.0
        val result = try { TradeResult.valueOf(stringField(json, "result") ?: "PENDING") } catch (e: Exception) { TradeResult.PENDING }
        val notes = stringField(json, "notes") ?: ""
        val createdAt = numberField(json, "createdAtEpochMillis")?.toLong() ?: System.currentTimeMillis()
        return TradingJournalEntry(
            id = id, pair = pair, direction = direction, entryPrice = entryPrice,
            stopLoss = stopLoss, takeProfit = takeProfit, lotSize = lotSize,
            result = result, notes = notes, createdAtEpochMillis = createdAt
        )
    }

    private fun stringField(json: String, name: String): String? {
        val regex = Regex("\"$name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val raw = regex.find(json)?.groupValues?.get(1) ?: return null
        return raw.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\")
    }

    private fun numberField(json: String, name: String): Double? {
        val regex = Regex("\"$name\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)")
        return regex.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
