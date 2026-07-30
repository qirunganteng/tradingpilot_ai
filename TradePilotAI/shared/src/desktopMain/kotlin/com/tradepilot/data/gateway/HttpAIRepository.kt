package com.tradepilot.data.gateway

import com.tradepilot.data.gateway.json.MinimalJson
import com.tradepilot.domain.config.GatewayConfig
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeDirection
import com.tradepilot.domain.repository.AIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * Implementasi desktop dari [AIRepository] -- POST /api/v1/analyze, padanan
 * persis dari WorkerProvider.kt (android-client/data/data-ai). Kontrak
 * request/response HARUS sama dengan WorkerAnalyzeRequest/Response di sana
 * (backend satu-satunya, dua client).
 *
 * deviceId desktop sengaja konstan "desktop-client" untuk fase ini (belum
 * ada SecureKeyStore/device-id generator di desktop-client seperti Android
 * punya core-security). Cukup untuk rate-limit & audit log per-platform,
 * belum per-instalasi individual -- kalau nanti perlu itu, lihat pola
 * SecureKeyStore.getOrCreateDeviceId() di android-client sebagai referensi.
 */
class HttpAIRepository(
    private val config: GatewayConfig
) : AIRepository {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override suspend fun analyzeChart(
        imageBytes: ByteArray,
        methods: List<String>
    ): Result<AnalysisResult> = withContext(Dispatchers.IO) {
        if (!config.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Gateway belum dikonfigurasi. Set environment variable " +
                        "TRADEPILOT_GATEWAY_URL dan TRADEPILOT_GATEWAY_TOKEN " +
                        "sebelum menjalankan desktop-client."
                )
            )
        }

        try {
            val base64Image = Base64.getEncoder().encodeToString(imageBytes)

            val builder = MinimalJson.ObjectBuilder()
            builder.put("imageBase64", base64Image)
            builder.put("mimeType", "image/jpeg")
            builder.putStringArray("methods", methods)
            builder.put("deviceId", "desktop-client")
            builder.put("storeImage", true)

            val request = HttpRequest.newBuilder()
                .uri(URI.create("${config.baseUrl}/api/v1/analyze"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${config.authToken}")
                // Gemini vision + upload gambar jauh lebih lambat dari
                // calculate-risk -- timeout lebih longgar (30s vs 15s).
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(builder.build()))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val json = response.body()

            if (response.statusCode() !in 200..299) {
                val errorMessage = MinimalJson.string(json, "error")
                    ?: "Gateway error (HTTP ${response.statusCode()})"
                return@withContext Result.failure(AIGatewayException(response.statusCode(), errorMessage))
            }

            val signalRaw = MinimalJson.string(json, "signal")?.uppercase() ?: "NONE"
            val signal = when (signalRaw) {
                "BUY" -> TradeDirection.BUY
                "SELL" -> TradeDirection.SELL
                else -> TradeDirection.NONE
            }

            val result = AnalysisResult(
                pair = MinimalJson.string(json, "pair") ?: "?",
                trend = MinimalJson.string(json, "trend") ?: "",
                signal = signal,
                confidence = MinimalJson.double(json, "confidence") ?: 0.0,
                entry = MinimalJson.string(json, "entry") ?: "",
                stopLoss = MinimalJson.string(json, "stopLoss") ?: "",
                takeProfit = MinimalJson.string(json, "takeProfit") ?: "",
                riskReward = MinimalJson.string(json, "riskReward") ?: "",
                reasoning = MinimalJson.string(json, "reasoning") ?: "",
                method = MinimalJson.stringArray(json, "method"),
                providerUsed = MinimalJson.string(json, "providerUsed") ?: "worker-gateway",
                timestampMillis = MinimalJson.long(json, "timestampMillis") ?: System.currentTimeMillis()
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AIGatewayException(val statusCode: Int, message: String) : Exception(message)
