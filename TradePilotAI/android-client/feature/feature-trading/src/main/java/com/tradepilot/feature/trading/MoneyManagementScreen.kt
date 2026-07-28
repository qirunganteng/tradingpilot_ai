package com.tradepilot.feature.trading
import org.koin.androidx.compose.koinViewModel

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Money Management (versi 2). KEPUTUSAN TERKUNCI: FLAG_SECURE hanya aktif
 * di screen ini karena menampilkan data finansial (Blueprint bagian 18).
 */
@Composable
fun MoneyManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: MoneyManagementViewModel = koinViewModel()
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    var balance by remember { mutableStateOf("") }
    var riskPercent by remember { mutableStateOf("1.0") }
    var entry by remember { mutableStateOf("") }
    var sl by remember { mutableStateOf("") }
    var tp by remember { mutableStateOf("") }

    val result by viewModel.result.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Manajemen Risiko", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Isi manual — balance/equity belum bisa dibaca otomatis dari Exness.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        NumberField("Balance (USD)", balance) { balance = it }
        NumberField("Risk % per trade", riskPercent) { riskPercent = it }
        NumberField("Entry Price", entry) { entry = it }
        NumberField("Stop Loss Price", sl) { sl = it }
        NumberField("Take Profit Price", tp) { tp = it }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.calculate(
                    balance = balance.toDoubleOrNull() ?: 0.0,
                    riskPercent = riskPercent.toDoubleOrNull() ?: 0.0,
                    entryPrice = entry.toDoubleOrNull() ?: 0.0,
                    stopLossPrice = sl.toDoubleOrNull() ?: 0.0,
                    takeProfitPrice = tp.toDoubleOrNull() ?: 0.0
                )
            }
        ) { Text("Hitung Rekomendasi") }

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        result?.let { r ->
            Divider()
            Text(text = "Risk %: ${r.riskPercent}", fontFamily = FontFamily.Monospace)
            Text(text = "Lot: ${r.lot}", fontFamily = FontFamily.Monospace)
            Text(text = "SL: ${r.stopLoss}", fontFamily = FontFamily.Monospace)
            Text(text = "TP: ${r.takeProfit}", fontFamily = FontFamily.Monospace)
            Text(text = "RR: ${r.riskRewardRatio}", fontFamily = FontFamily.Monospace)
            Text(text = "Max Daily Loss: ${r.maxDailyLoss}", fontFamily = FontFamily.Monospace)
            Text(text = "Max Trade/hari: ${r.maxTrade}", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
