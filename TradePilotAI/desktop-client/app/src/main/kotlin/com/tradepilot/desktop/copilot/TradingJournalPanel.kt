package com.tradepilot.desktop.copilot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.journal.JournalDirection
import com.tradepilot.desktop.journal.TradeResult
import com.tradepilot.desktop.journal.TradingJournalEntry
import com.tradepilot.desktop.journal.TradingJournalStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab "Journal" di AI Workspace (bagian dari daftar tab di master prompt:
 * Chat / Chart Analysis / Risk Calculator / ... / Trading Journal / ... /
 * Session Summary). Digabung jadi SATU panel (bukan dua tab terpisah)
 * karena Session Summary murni agregat dari data Journal yang sama --
 * pisah jadi 2 tab cuma bikin user bolak-balik tanpa data baru.
 *
 * PERSISTEN (lihat TradingJournalStore.kt) -- beda dengan History/Bookmark
 * browser biasa yang boleh hilang kalau app ditutup, catatan trade WAJIB
 * selamat lintas sesi.
 */
@Composable
fun TradingJournalPanel(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { TradingJournalStore.ensureLoaded() }

    var isFormOpen by remember { mutableStateOf(false) }
    var pair by remember { mutableStateOf("XAUUSD") }
    var direction by remember { mutableStateOf(JournalDirection.BUY) }
    var entryPrice by remember { mutableStateOf("") }
    var stopLoss by remember { mutableStateOf("") }
    var takeProfit by remember { mutableStateOf("") }
    var lotSize by remember { mutableStateOf("0.01") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val summary = remember(TradingJournalStore.entries.size, TradingJournalStore.entries) {
        TradingJournalStore.todaySummary()
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Trading Journal", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text("Catatan trade tersimpan lokal di perangkat ini, tidak hilang saat app ditutup/update.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

        Spacer(Modifier.height(10.dp))
        // Session Summary (hari ini).
        SessionSummaryCard(summary)

        Spacer(Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            onClick = { isFormOpen = !isFormOpen }
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (isFormOpen) "Tutup Form" else "Catat Trade Baru", style = MaterialTheme.typography.labelSmall)
        }

        if (isFormOpen) {
            Spacer(Modifier.height(8.dp))
            Surface(color = Color(0xFF262626), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(10.dp)) {
                    CompactField("Pair", pair) { pair = it }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        DirectionChip("BUY", direction == JournalDirection.BUY) { direction = JournalDirection.BUY }
                        Spacer(Modifier.width(6.dp))
                        DirectionChip("SELL", direction == JournalDirection.SELL) { direction = JournalDirection.SELL }
                    }
                    CompactField("Entry", entryPrice) { entryPrice = it }
                    CompactField("Stop Loss", stopLoss) { stopLoss = it }
                    CompactField("Take Profit", takeProfit) { takeProfit = it }
                    CompactField("Lot", lotSize) { lotSize = it }
                    CompactField("Catatan (opsional)", notes) { notes = it }

                    formError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE57373))
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        onClick = {
                            val entryD = entryPrice.toDoubleOrNull()
                            val slD = stopLoss.toDoubleOrNull()
                            val tpD = takeProfit.toDoubleOrNull()
                            val lotD = lotSize.toDoubleOrNull()
                            if (pair.isBlank() || entryD == null || slD == null || tpD == null || lotD == null) {
                                formError = "Isi semua angka dengan benar (Entry/SL/TP/Lot)."
                                return@Button
                            }
                            TradingJournalStore.add(
                                TradingJournalEntry(
                                    pair = pair.trim().uppercase(),
                                    direction = direction,
                                    entryPrice = entryD,
                                    stopLoss = slD,
                                    takeProfit = tpD,
                                    lotSize = lotD,
                                    notes = notes.trim()
                                )
                            )
                            formError = null
                            entryPrice = ""; stopLoss = ""; takeProfit = ""; notes = ""
                            isFormOpen = false
                        }
                    ) {
                        Text("Simpan", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF3A3A3A))
        Spacer(Modifier.height(8.dp))

        if (TradingJournalStore.entries.isEmpty()) {
            Text("Belum ada catatan trade.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        } else {
            TradingJournalStore.entries.forEach { entry ->
                JournalEntryRow(entry)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(summary: com.tradepilot.desktop.journal.DaySummary) {
    Surface(color = Color(0xFF262626), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("Ringkasan Sesi Hari Ini", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row {
                SummaryStat("Total", "${summary.totalTrades}")
                SummaryStat("Win", "${summary.wins}", Color(0xFF81C784))
                SummaryStat("Loss", "${summary.losses}", Color(0xFFE57373))
                SummaryStat("Pending", "${summary.pending}", Color(0xFFFFB74D))
            }
            Spacer(Modifier.height(4.dp))
            Text("Win rate: ${"%.1f".format(summary.winRatePercent)}%", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        }
    }
}

@Composable
private fun RowScope.SummaryStat(label: String, value: String, color: Color = Color.White) {
    Column(modifier = Modifier.weight(1f)) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
private fun DirectionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (label == "BUY") Color(0xFF81C784) else Color(0xFFE57373)
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) color.copy(alpha = 0.25f) else Color(0xFF2C2C2C),
            labelColor = if (isSelected) color else Color.Gray
        )
    )
}

@Composable
private fun JournalEntryRow(entry: TradingJournalEntry) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }
    val directionColor = if (entry.direction == JournalDirection.BUY) Color(0xFF81C784) else Color(0xFFE57373)
    val resultColor = when (entry.result) {
        TradeResult.WIN -> Color(0xFF81C784)
        TradeResult.LOSS -> Color(0xFFE57373)
        TradeResult.BREAKEVEN -> Color(0xFFFFB74D)
        TradeResult.PENDING -> Color.Gray
    }

    Surface(color = Color(0xFF262626), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.pair, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(6.dp))
                Surface(color = directionColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                    Text(entry.direction.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = directionColor)
                }
                Spacer(Modifier.weight(1f))
                Text(dateFormat.format(Date(entry.createdAtEpochMillis)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                IconButton(onClick = { TradingJournalStore.delete(entry.id) }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Entry: ${entry.entryPrice}  •  SL: ${entry.stopLoss}  •  TP: ${entry.takeProfit}  •  Lot: ${entry.lotSize}", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            if (entry.notes.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(entry.notes, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(Modifier.height(6.dp))
            Row {
                TradeResult.entries.forEach { r ->
                    val selected = entry.result == r
                    Surface(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { TradingJournalStore.updateResult(entry.id, r) },
                        color = if (selected) resultColor.copy(alpha = 0.25f) else Color(0xFF2C2C2C),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            r.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) resultColor else Color.Gray
                        )
                    }
                }
            }
        }
    }
}
