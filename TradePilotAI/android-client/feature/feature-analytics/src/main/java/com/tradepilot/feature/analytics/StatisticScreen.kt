package com.tradepilot.feature.analytics
import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tradepilot.domain.usecase.GenerateHistoryInsightUseCase

/**
 * Statistik & rekomendasi berbasis histori (versi 6). Sebelum minimal
 * 100 trade terkumpul, tampilkan progress alih-alih insight prematur.
 */
@Composable
fun StatisticScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticViewModel = koinViewModel()
) {
    val insight by viewModel.insight.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Statistik & Rekomendasi", style = MaterialTheme.typography.titleLarge)

        if (!insight.hasEnoughData) {
            val target = GenerateHistoryInsightUseCase.MIN_TRADES_FOR_INSIGHT
            Text("Kumpulkan minimal $target trade untuk mendapat rekomendasi berbasis histori.")
            LinearProgressIndicator(
                progress = { (insight.tradesAnalyzed.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize().padding(top = 4.dp)
            )
            Text("${insight.tradesAnalyzed} / $target trade tersimpan.")
        } else {
            StatLine("Trade Dianalisa", insight.tradesAnalyzed.toString())
            StatLine("Winrate", "${insight.winRate}%")
            StatLine("Loss Rate", "${insight.lossRate}%")
            StatLine("Profit Factor", insight.profitFactor.toString())
            StatLine("Average RR", insight.averageRR.toString())
            StatLine("Jam Terbaik", insight.bestHourOfDay?.let { "$it:00" } ?: "-")
            StatLine("Pair Terbaik", insight.bestPair ?: "-")
            StatLine("Pair Terlemah", insight.worstPair ?: "-")
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Text(text = "$label: $value", fontFamily = FontFamily.Monospace)
}
