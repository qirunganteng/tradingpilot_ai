package com.tradepilot.desktop.copilot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tradepilot.data.gateway.HttpAIRepository
import com.tradepilot.data.gateway.HttpRiskGatewayRepository
import com.tradepilot.desktop.browser.JCEFBrowserEngine
import com.tradepilot.domain.config.GatewayConfig
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.RiskRecommendation
import com.tradepilot.domain.model.TradeDirection
import com.tradepilot.domain.repository.AIRepository
import com.tradepilot.domain.repository.RiskGatewayRepository
import com.tradepilot.domain.usecase.CalculateRiskUseCase
import com.tradepilot.domain.usecase.DeriveCopilotSignalUseCase
import kotlinx.coroutines.launch

/**
 * Panel AI Copilot ala VS Code (dock kanan, bisa ditoggle dari ActivityBar).
 *
 * Fase 6: Risk Calculator terhubung ke Cloudflare Worker (/api/v1/calculate-risk)
 * -- server sumber kebenaran (lihat catatan di Repositories.kt), CalculateRiskUseCase
 * lokal cuma instant preview optimistic.
 *
 * Fase 7 (BARU): Analisa Chart -- screenshot komponen browser JCEF
 * (DesktopChartCapture, pakai java.awt.Robot) -> upload ke /api/v1/analyze
 * (HttpAIRepository) -> tampilkan AnalysisResult + sinyal turunan
 * (DeriveCopilotSignalUseCase). BATASAN JUJUR: Robot capture screen area
 * SUNGGUHAN di posisi window -- window aplikasi harus terlihat & tidak
 * ketutupan window lain saat tombol "Analisa Chart" ditekan, kalau tidak
 * hasil capture salah/hitam (lihat catatan lengkap di DesktopChartCapture.kt).
 *
 * Chat AI bebas teks (bukan cuma tombol analisa terstruktur) BELUM ada --
 * itu perubahan UX yang lebih besar (percakapan multi-turn), sengaja belum
 * diklaim di sini.
 */
@Composable
fun CopilotPanel(engine: JCEFBrowserEngine?, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val gatewayConfig = remember { readGatewayConfigFromEnv() }
    val gateway: RiskGatewayRepository = remember(gatewayConfig) { HttpRiskGatewayRepository(gatewayConfig) }
    val aiRepository: AIRepository = remember(gatewayConfig) { HttpAIRepository(gatewayConfig) }
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

    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CandlestickChart, contentDescription = null, tint = Color(0xFFBA68C8), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Analisa Chart (Fase 7)", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Screenshot browser saat ini -> kirim ke AI Gateway (Gemini vision) " +
                "-> hasil analisa ICT/SMC. Pastikan window ini terlihat penuh & " +
                "tidak ketutupan window lain saat menekan tombol -- capture-nya " +
                "screen area sungguhan, bukan render langsung dari browser.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing && engine != null,
            onClick = {
                val component = engine?.uiComponent ?: return@Button
                analysisError = null
                isAnalyzing = true

                val captureResult = DesktopChartCapture.captureCompressed(component)
                captureResult.onFailure {
                    analysisError = it.message ?: "Gagal ambil screenshot."
                    isAnalyzing = false
                }

                captureResult.onSuccess { imageBytes ->
                    scope.launch {
                        aiRepository.analyzeChart(imageBytes, methods = emptyList())
                            .onSuccess {
                                analysisResult = it
                                isAnalyzing = false
                            }
                            .onFailure {
                                analysisError = it.message ?: "Gagal menghubungi AI Gateway."
                                isAnalyzing = false
                            }
                    }
                }
            }
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                when {
                    engine == null -> "Browser belum siap"
                    isAnalyzing -> "Menganalisa chart..."
                    else -> "Analisa Chart Sekarang"
                }
            )
        }

        analysisError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        analysisResult?.let { result ->
            Spacer(Modifier.height(12.dp))
            AnalysisResultCard(result)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Chat AI bebas teks (percakapan multi-turn) belum ada -- ini tombol " +
                "analisa terstruktur satu-arah dulu (screenshot -> hasil).",
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
private fun AnalysisResultCard(result: AnalysisResult) {
    val signalColor = when (result.signal) {
        TradeDirection.BUY -> Color(0xFF81C784)
        TradeDirection.SELL -> Color(0xFFE57373)
        TradeDirection.NONE -> Color.Gray
    }
    Surface(color = Color(0xFF262626), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(result.pair, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Surface(color = signalColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                    Text(
                        result.signal.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = signalColor
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Trend: ${result.trend} • Confidence: ${result.confidence}%", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text("Entry: ${result.entry}", style = MaterialTheme.typography.bodySmall)
            Text("SL: ${result.stopLoss}  •  TP: ${result.takeProfit}", style = MaterialTheme.typography.bodySmall)
            Text("RR: ${result.riskReward}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(result.reasoning, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)

            val signals = DeriveCopilotSignalUseCase.invoke(result)
            if (signals.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF3A3A3A))
                Spacer(Modifier.height(6.dp))
                signals.forEach { (category, message) ->
                    Text("• [$category] $message", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4FC3F7))
                }
            }
        }
    }
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
