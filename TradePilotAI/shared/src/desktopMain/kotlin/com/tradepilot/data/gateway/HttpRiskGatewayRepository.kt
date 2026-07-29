package com.tradepilot.data.gateway

import com.tradepilot.domain.config.GatewayConfig
import com.tradepilot.domain.model.RiskRecommendation
import com.tradepilot.domain.repository.RiskGatewayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Implementasi desktop dari [RiskGatewayRepository] -- panggil
 * POST /api/v1/calculate-risk di Cloudflare Worker (lihat
 * backend/cloudflare-worker/src/riskEngine.ts untuk logic servernya).
 *
 * Sengaja pakai java.net.http.HttpClient (bawaan JDK 11+, sesuai
 * jvmToolchain(11) di desktop-client/app) alih-alih menambah dependency
 * HTTP baru (OkHttp/Ktor) -- modul ini kecil dan cuma butuh 1 endpoint
 * JSON, belum perlu library sebesar itu. Kalau kebutuhan network desktop
 * bertambah (Fase 7: analyze/ocr dengan upload gambar), pertimbangkan
 * pindah ke Ktor Client biar konsisten dengan kemungkinan kebutuhan
 * multiplatform HTTP client di commonMain nanti.
 *
 * Parsing JSON di sini SENGAJA manual (bukan kotlinx.serialization/Moshi)
 * karena field response-nya flat & sudah diketahui persis (lihat
 * RiskRecommendation). Kalau field bertambah/berubah, pertimbangkan ganti
 * ke library JSON yang proper -- jangan tumpuk lebih banyak manual parsing
 * di atas ini.
 */
class HttpRiskGatewayRepository(
    private val config: GatewayConfig
) : RiskGatewayRepository {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override suspend fun calculateRisk(
        balance: Double,
        riskPercent: Double,
        entryPrice: Double,
        stopLossPrice: Double,
        takeProfitPrice: Double,
        deviceId: String,
        pipValuePerLotUsd: Double?,
        pipSize: Double?
    ): Result<RiskRecommendation> = withContext(Dispatchers.IO) {
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
            val bodyJson = buildJsonObject {
                put("balance", balance)
                put("riskPercent", riskPercent)
                put("entryPrice", entryPrice)
                put("stopLossPrice", stopLossPrice)
                put("takeProfitPrice", takeProfitPrice)
                put("deviceId", deviceId)
                if (pipValuePerLotUsd != null) put("pipValuePerLotUsd", pipValuePerLotUsd)
                if (pipSize != null) put("pipSize", pipSize)
            }

            val request = HttpRequest.newBuilder()
                .uri(URI.create("${config.baseUrl}/api/v1/calculate-risk"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${config.authToken}")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                val errorMessage = extractStringField(response.body(), "error")
                    ?: "Gateway error (HTTP ${response.statusCode()})"
                return@withContext Result.failure(RiskGatewayException(response.statusCode(), errorMessage))
            }

            val json = response.body()
            val result = RiskRecommendation(
                riskPercent = requireDoubleField(json, "riskPercent"),
                lot = requireDoubleField(json, "lot"),
                stopLoss = requireDoubleField(json, "stopLoss"),
                takeProfit = requireDoubleField(json, "takeProfit"),
                riskRewardRatio = requireDoubleField(json, "riskRewardRatio"),
                maxDailyLoss = requireDoubleField(json, "maxDailyLoss"),
                maxTrade = requireDoubleField(json, "maxTrade").toInt()
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Manual JSON encode/decode kecil-kecilan, lihat catatan di kelas ---

    private fun buildJsonObject(build: JsonObjectBuilder.() -> Unit): String {
        val builder = JsonObjectBuilder()
        builder.build()
        return builder.toJsonString()
    }

    private class JsonObjectBuilder {
        private val fields = mutableListOf<Pair<String, String>>()

        fun put(key: String, value: Double) {
            fields += key to value.toString()
        }

        fun put(key: String, value: String) {
            val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
            fields += key to "\"$escaped\""
        }

        fun toJsonString(): String =
            fields.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":$v" }
    }

    private fun requireDoubleField(json: String, field: String): Double {
        val regex = Regex("\"$field\"\\s*:\\s*(-?[0-9]+(\\.[0-9]+)?)")
        val match = regex.find(json)
            ?: throw IllegalStateException("Field '$field' tidak ditemukan di response gateway: $json")
        return match.groupValues[1].toDouble()
    }

    private fun extractStringField(json: String, field: String): String? {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)
    }
}

class RiskGatewayException(val statusCode: Int, message: String) : Exception(message)
