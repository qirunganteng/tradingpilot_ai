package com.tradepilot.feature.mentor
import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** AI Mentor (versi 7): penjelasan per trade agar user makin baik. */
@Composable
fun MentorScreen(
    modifier: Modifier = Modifier,
    viewModel: MentorViewModel = koinViewModel()
) {
    val items by viewModel.items.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "AI Mentor", style = MaterialTheme.typography.titleLarge)

        if (items.isEmpty()) {
            Text("Belum ada trade untuk dievaluasi.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { item ->
                    Column {
                        Text(
                            text = "${item.trade.pair} - ${item.trade.direction} (PnL ${item.trade.profitLoss})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        item.feedback.whyGood?.let { Text("✓ $it") }
                        item.feedback.whyBad?.let { Text("✗ $it") }
                        item.feedback.slTooTight?.let { Text("⚠ $it") }
                        item.feedback.tpTooFar?.let { Text("⚠ $it") }
                        item.feedback.betterEntrySuggestion?.let { Text("💡 $it") }
                        Divider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}
