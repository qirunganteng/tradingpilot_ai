package com.tradepilot.feature.journal
import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Trading Journal otomatis (versi 3): daftar trade + statistik
 * (winrate, profit factor, average RR, total profit).
 */
@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = koinViewModel(),
    onAddTradeClick: () -> Unit = {}
) {
    val trades by viewModel.trades.collectAsState()
    val stats by viewModel.statistics.collectAsState()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTradeClick) {
                Icon(Icons.Default.Add, contentDescription = "Catat Trade Baru")
            }
        }
    ) { padding ->
    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
        Text(text = "Jurnal Trading", style = MaterialTheme.typography.titleLarge)

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StatLine("Total Trade", stats.totalTrades.toString())
            StatLine("Winrate", "${stats.winRate}%")
            StatLine("Profit Factor", stats.profitFactor.toString())
            StatLine("Average RR", stats.averageRR.toString())
            StatLine("Total Profit", stats.totalProfit.toString())
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (trades.isEmpty()) {
            Text("Belum ada trade tersimpan.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(trades) { trade ->
                    Column {
                        Text(
                            text = "${trade.pair} - ${trade.direction}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Entry ${trade.entry} | Exit ${trade.exit} | PnL ${trade.profitLoss} | RR ${trade.riskRewardRatio}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Divider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Text(text = "$label: $value", fontFamily = FontFamily.Monospace)
}
