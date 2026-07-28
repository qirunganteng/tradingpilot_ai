package com.tradepilot.data.ai.provider

import android.util.Base64
import com.tradepilot.core.security.SecureKeyStore
import com.tradepilot.data.ai.remote.WorkerAnalyzeRequest
import com.tradepilot.data.ai.remote.WorkerApiService
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeDirection

/**
 * AIProvider yang memanggil Cloudflare Worker AI Gateway alih-alih Gemini
 * secara langsung. Ini provider DEFAULT yang direkomendasikan mulai fase ini:
 *  - API key Gemini tinggal di server (Worker secret), tidak pernah di device.
 *  - Screenshot chart otomatis diarsipkan ke R2 lewat Worker.
 *  - Histori panggilan analisa tercatat di D1 (audit, BUKAN pengganti Journal lokal).
 *
 * GeminiProvider (panggil Gemini langsung dari device) tetap ada sebagai
 * fallback opsional di ProviderFactory bagi yang belum/tidak mau setup Worker.
 */
class WorkerProvider(
    private val service: WorkerApiService,
    private val secureKeyStore: SecureKeyStore
) : AIProvider {

    override val providerName: String = "worker-gateway"

    override suspend fun analyzeChart(
        imageBytes: ByteArray,
        promptText: String
    ): Result<AnalysisResult> {
        // promptText diabaikan sengaja -- Worker menyusun prompt sendiri (PromptBuilder
        // versi server) supaya prompt terpusat & bisa di-update tanpa rilis app baru.
        return try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val deviceId = secureKeyStore.getOrCreateDeviceId()

            val response = service.analyze(
                WorkerAnalyzeRequest(
                    imageBase64 = base64Image,
                    deviceId = deviceId
                )
            )

            val signal = when (response.signal.uppercase()) {
                "BUY" -> TradeDirection.BUY
                "SELL" -> TradeDirection.SELL
                else -> TradeDirection.NONE
            }

            Result.success(
                AnalysisResult(
                    pair = response.pair,
                    trend = response.trend,
                    signal = signal,
                    confidence = response.confidence,
                    entry = response.entry,
                    stopLoss = response.stopLoss,
                    takeProfit = response.takeProfit,
                    riskReward = response.riskReward,
                    reasoning = response.reasoning,
                    method = response.method,
                    providerUsed = response.providerUsed,
                    timestampMillis = response.timestampMillis
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
