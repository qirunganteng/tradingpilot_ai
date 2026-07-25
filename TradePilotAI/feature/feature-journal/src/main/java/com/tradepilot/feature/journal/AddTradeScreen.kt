package com.tradepilot.feature.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradepilot.domain.model.TradeDirection

/**
 * Form manual untuk mengisi hasil trade ke Journal. Ini BUKAN eksekusi
 * transaksi apapun — murni mencatat hasil trade yang SUDAH dilakukan user
 * sendiri secara manual di WebView Exness, sesuai prinsip "AI/aplikasi
 * tidak pernah bertransaksi" di seluruh blueprint.
 */
@Composable
fun AddTradeScreen(
    modifier: Modifier = Modifier,
    viewModel: AddTradeViewModel = hiltViewModel(),
    onSaved: () -> Unit = {}
) {
    val saved by viewModel.saved.collectAsState()

    var pair by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(TradeDirection.BUY) }
    var entry by remember { mutableStateOf("") }
    var exit by remember { mutableStateOf("") }
    var sl by remember { mutableStateOf("") }
    var tp by remember { mutableStateOf("") }
    var lot by remember { mutableStateOf("") }
    var profitLoss by remember { mutableStateOf("") }
    var rr by remember { mutableStateOf("") }
    var balanceAfter by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(saved) {
        if (saved) {
            onSaved()
            viewModel.resetSaved()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Catat Trade", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Catat hasil trade yang sudah kamu eksekusi manual — form ini tidak melakukan transaksi apapun.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Field("Pair (mis. EURUSD)", pair) { pair = it }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = direction == TradeDirection.BUY, onClick = { direction = TradeDirection.BUY }, label = { Text("BUY") })
            FilterChip(selected = direction == TradeDirection.SELL, onClick = { direction = TradeDirection.SELL }, label = { Text("SELL") })
        }

        NumField("Entry", entry) { entry = it }
        NumField("Exit", exit) { exit = it }
        NumField("Stop Loss", sl) { sl = it }
        NumField("Take Profit", tp) { tp = it }
        NumField("Lot", lot) { lot = it }
        NumField("Profit/Loss (USD, negatif jika rugi)", profitLoss) { profitLoss = it }
        NumField("Risk Reward Ratio", rr) { rr = it }
        NumField("Balance Setelah Trade", balanceAfter) { balanceAfter = it }
        Field("Catatan (opsional)", notes) { notes = it }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.saveTrade(
                    pair = pair.ifBlank { "N/A" },
                    direction = direction,
                    entry = entry.toDoubleOrNull() ?: 0.0,
                    exit = exit.toDoubleOrNull() ?: 0.0,
                    stopLoss = sl.toDoubleOrNull() ?: 0.0,
                    takeProfit = tp.toDoubleOrNull() ?: 0.0,
                    lot = lot.toDoubleOrNull() ?: 0.0,
                    profitLoss = profitLoss.toDoubleOrNull() ?: 0.0,
                    riskRewardRatio = rr.toDoubleOrNull() ?: 0.0,
                    balanceAfter = balanceAfter.toDoubleOrNull() ?: 0.0,
                    notes = notes
                )
            }
        ) { Text("Simpan ke Journal") }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
