package com.tradepilot.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings Manager (Blueprint 0): API key, Risk % default, dan (nanti)
 * bahasa/tema/timeframe/notification. Fase 4+: API key & risk % dulu
 * yang difungsikan penuh.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKeySaved by viewModel.apiKeySaved.collectAsState()
    val workerConfigured by viewModel.workerConfigured.collectAsState()
    val workerBaseUrlSaved by viewModel.workerBaseUrl.collectAsState()
    val riskDefault by viewModel.riskPercentDefault.collectAsState()

    var workerUrlInput by remember(workerBaseUrlSaved) { mutableStateOf(workerBaseUrlSaved) }
    var workerTokenInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var riskSlider by remember(riskDefault) { mutableStateOf(riskDefault.toFloat()) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Pengaturan", style = MaterialTheme.typography.titleLarge)

        Text(text = "Cloudflare Worker AI Gateway (direkomendasikan)", style = MaterialTheme.typography.titleSmall)
        Text(
            text = if (workerConfigured) "Status: terkonfigurasi" else "Status: belum dikonfigurasi — analisa AI tidak akan berfungsi tanpa ini",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = workerUrlInput,
            onValueChange = { workerUrlInput = it },
            label = { Text("URL Worker (mis. https://tradepilot-ai-gateway.xxx.workers.dev)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = workerTokenInput,
            onValueChange = { workerTokenInput = it },
            label = { Text("Gateway Token") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { viewModel.saveWorkerConfig(workerUrlInput, workerTokenInput); workerTokenInput = "" }) {
            Text("Simpan Konfigurasi Worker")
        }
        if (workerConfigured) {
            OutlinedButton(onClick = { viewModel.clearWorkerConfig() }) { Text("Hapus Konfigurasi Worker") }
        }

        Divider()

        Text(text = "Gemini API Key (fallback, opsional)", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Hanya dipakai kalau kamu sengaja ganti provider ke 'Gemini langsung' di kode " +
                "(ProviderFactory). Dengan Worker Gateway aktif, field ini TIDAK diperlukan.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (apiKeySaved) "Status: tersimpan (terenkripsi)" else "Status: belum diisi",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("Masukkan API Key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { viewModel.saveApiKey(apiKeyInput); apiKeyInput = "" }) {
            Text("Simpan API Key")
        }
        if (apiKeySaved) {
            OutlinedButton(onClick = { viewModel.clearApiKey() }) { Text("Hapus API Key") }
        }

        Divider()

        Text(text = "Risk % Default", style = MaterialTheme.typography.titleSmall)
        Text(text = "${"%.1f".format(riskSlider)}%")
        Slider(
            value = riskSlider,
            onValueChange = { riskSlider = it },
            onValueChangeFinished = { viewModel.saveRiskPercentDefault(riskSlider.toDouble()) },
            valueRange = 0.1f..5f
        )

        Divider()

        Text(
            text = "Bahasa: mengikuti pengaturan bahasa perangkat (ID/EN) — pengaturan manual di app menyusul.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
