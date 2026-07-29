package com.tradepilot.desktop.copilot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tradepilot.data.gateway.HttpRiskGatewayRepository
import com.tradepilot.domain.config.GatewayConfig
import com.tradepilot.domain.model.RiskRecommendation
import com.tradepilot.domain.repository.RiskGatewayRepository
import com.tradepilot.domain.usecase.CalculateRiskUseCase
import kotlinx.coroutines.launch

/**
 * Panel AI Copilot ala VS Code (dock kanan, bisa ditoggle dari ActivityBar).
 *
 * Fase 6 (sekarang): Risk Calculator yang benar-benar terhubung ke Cloudflare
 * Worker (/api/v1/calculate-risk) -- bukti pipeline desktop-client -> shared
 * -> Worker jalan end-to-end. Lihat catatan di Repositories.kt: server yang
 * jadi sumber kebenaran (Fase 6), CalculateRiskUseCase lokal cuma untuk
 * instant preview optimistic sebelum response gateway datang.
 *
 * Fase 7 (BELUM dikerjakan, jangan dikira sudah ada): chat AI Copilot bebas
 * teks + analisis chart otomatis dari screenshot browser. Itu butuh
 * ChartSnapshotProvider desktop (capture JCEF ke bitmap) dulu, yang belum
 * diimplementasikan -- makanya belum saya klaim "AI chat" di sini.
 */
@Composable
fun CopilotPanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val gatewayConfig = remember { readGatewayConfigFromEnv() }
    val gateway: RiskGatewayRepository = remember(gatewayConfig) { HttpRiskGatewayRepository(gatewayConfig) }
    val localUseCase = remember { CalculateRiskUseCase() }

    var balance by remember { mutableStateOf("1000") }
    var riskPercent by remember { mutableStateOf("1") }
    var entryPrice by remember { mutableStateOf("1.1000") }
    var stopLossPrice by remember { mutableStateOf("1.0950") }
    var takeProfitPrice by remember { mutableStateOf("1.1100") }

    var localResult by remember { mutableStateOf<RiskRecommendation?>(null) }
    var gatewayResult by remember { mutableStateOf<RiskRecommendation?>(null) }
    var gatewayError by remember { mutableStateOf<String?>(null) }
    var isCalling by remember { mutableStateOf(false) }

    fun parsedInputsOrNull(): RiskInputs? = try {
        RiskInputs(
            balance = balance.toDouble(),
            riskPercent = riskPercent.toDouble(),
            entry = entryPrice.toDouble(),
            sl = stopLossPrice.toDouble(),
            tp = takeProfitPrice.toDouble()
        )
    } catch (e: NumberFormatException) {
        null
    }

    Column(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(Color(0xFF1F1F1F))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF4FC3F7))
            Spacer(Modifier.width(8.dp))
            Text("AI Copilot", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Risk Calculator — terhubung ke AI Gateway",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        RiskField("Balance (USD)", balance) { balance = it }
        RiskField("Risk %", riskPercent) { riskPercent = it }
        RiskField("Entry", entryPrice) { entryPrice = it }
        RiskField("Stop Loss", stopLossPrice) { stopLossPrice = it }
        RiskField("Take Profit", takeProfitPrice) { takeProfitPrice = it }

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCalling,
            onClick = {
                val inputs = parsedInputsOrNull()
                if (inputs == null) {
                    gatewayError = "Input tidak valid — pastikan semua angka terisi benar."
                    return@Button
                }

                // Instant local preview (optimistic UI) -- lihat catatan kelas.
                localResult = localUseCase.invoke(
                    balance = inputs.balance,
                    riskPercent = inputs.riskPercent,
                    entryPrice = inputs.entry,
                    stopLossPrice = inputs.sl,
                    takeProfitPrice = inputs.tp
                )
                gatewayResult = null
                gatewayError = null
                isCalling = true

                scope.launch {
                    gateway.calculateRisk(
                        balance = inputs.balance,
                        riskPercent = inputs.riskPercent,
                        entryPrice = inputs.entry,
                        stopLossPrice = inputs.sl,
                        takeProfitPrice = inputs.tp,
                        deviceId = "desktop-client"
                    ).onSuccess {
                        gatewayResult = it
                        isCalling = false
                    }.onFailure {
                        gatewayError = it.message ?: "Gagal menghubungi gateway."
                        isCalling = false
                    }
                }
            }
        ) {
            if (isCalling) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isCalling) "Menghitung via gateway..." else "Hitung Risk")
        }

        if (!gatewayConfig.isConfigured) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Gateway belum dikonfigurasi. Set env var TRADEPILOT_GATEWAY_URL " +
                    "dan TRADEPILOT_GATEWAY_TOKEN, lalu jalankan ulang. Sementara itu " +
                    "hasil lokal tetap tampil di bawah.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFB74D)
            )
        }

        localResult?.let {
            Spacer(Modifier.height(16.dp))
            ResultCard(title = "Hasil lokal (instant preview)", result = it, accent = Color(0xFF81C784))
        }

        gatewayResult?.let {
            Spacer(Modifier.height(12.dp))
            ResultCard(title = "Hasil gateway (sumber kebenaran)", result = it, accent = Color(0xFF4FC3F7))
        }

        gatewayError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Color(0xFF3A3A3A))
        Spacer(Modifier.height(12.dp))
        Text(
            "Chat AI Copilot + analisis chart otomatis: Fase 7 (belum dikerjakan). " +
                "Butuh ChartSnapshotProvider desktop (capture JCEF -> gambar) dulu " +
                "sebelum bisa panggil /api/v1/analyze.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

private data class RiskInputs(
    val balance: Double,
    val riskPercent: Double,
    val entry: Double,
    val sl: Double,
    val tp: Double
)

@Composable
private fun RiskField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun ResultCard(title: String, result: RiskRecommendation, accent: Color) {
    Surface(
        color = Color(0xFF262626),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = accent)
            Spacer(Modifier.height(6.dp))
            Text("Lot: ${result.lot}", style = MaterialTheme.typography.bodySmall)
            Text("Risk/Reward: 1:${result.riskRewardRatio}", style = MaterialTheme.typography.bodySmall)
            Text("Max daily loss: ${result.maxDailyLoss}", style = MaterialTheme.typography.bodySmall)
            Text("Max trade/hari: ${result.maxTrade}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Baca konfigurasi gateway dari environment variable. TIDAK ADA default URL
 * yang di-hardcode di sini secara sengaja -- kalau belum di-set, panel tetap
 * jalan (hasil lokal saja) dan kasih pesan jelas alih-alih diam-diam gagal
 * atau nembak ke URL tebakan.
 */
private fun readGatewayConfigFromEnv(): GatewayConfig {
    val url = System.getenv("TRADEPILOT_GATEWAY_URL")?.trimEnd('/') ?: ""
    val token = System.getenv("TRADEPILOT_GATEWAY_TOKEN") ?: ""
    return GatewayConfig(baseUrl = url, authToken = token)
}
