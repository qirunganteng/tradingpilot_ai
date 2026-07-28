package com.tradepilot.feature.ai
import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeDirection

/**
 * Panel hasil analisa AI (versi 1, poin 7): "tampil dalam panel modern,
 * tidak boleh popup browser biasa". Data numerik pakai monospace agar
 * konsisten dengan gaya "editor" di core-ui/Type.kt.
 *
 * Sengaja TIDAK ada tombol aksi apapun yang mengeksekusi trade —
 * hanya menampilkan rekomendasi.
 */
@Composable
fun AnalysisScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.consumePendingImageIfAny()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (val s = state) {
            is AnalysisUiState.Idle -> Text("Tekan tombol ANALISA di layar Browser untuk memulai.")
            is AnalysisUiState.Loading -> LoadingState()
            is AnalysisUiState.Success -> AnalysisResultPanel(s.result)
            is AnalysisUiState.Error -> Text(
                text = "Analisa gagal: ${s.message}",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator()
        Text("Mengirim chart ke AI untuk dianalisa...")
    }
}

@Composable
private fun AnalysisResultPanel(result: AnalysisResult) {
    val signalColor = when (result.signal) {
        TradeDirection.BUY -> MaterialTheme.colorScheme.primary
        TradeDirection.SELL -> MaterialTheme.colorScheme.error
        TradeDirection.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(text = result.pair, style = MaterialTheme.typography.titleLarge)
    Text(text = "Trend: ${result.trend}")
    Text(
        text = "Signal: ${result.signal}",
        color = signalColor,
        style = MaterialTheme.typography.titleMedium
    )
    Text(text = "Confidence: ${result.confidence}%")

    Divider()

    DataRow("Entry", result.entry)
    DataRow("Stop Loss", result.stopLoss)
    DataRow("Take Profit", result.takeProfit)
    DataRow("Risk Reward", result.riskReward)

    Divider()

    Text(text = "Alasan Analisa:", style = MaterialTheme.typography.labelLarge)
    Text(text = result.reasoning)

    Text(
        text = "Metode: ${result.method.joinToString(", ")}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
        text = "AI tidak melakukan transaksi. Keputusan BUY/SELL sepenuhnya milik Anda.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DataRow(label: String, value: String) {
    Text(text = "$label: $value", fontFamily = FontFamily.Monospace)
}
