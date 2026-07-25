package com.tradepilot.data.ai.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface ke Cloudflare Worker AI Gateway (bukan langsung ke
 * Gemini). Worker yang memegang GEMINI_API_KEY di sisi server — app
 * Android TIDAK PERNAH menyimpan API key Gemini lagi (lihat WorkerProvider).
 */
interface WorkerApiService {
    @POST("api/v1/analyze")
    suspend fun analyze(@Body request: WorkerAnalyzeRequest): WorkerAnalyzeResponse

    @GET("api/v1/analyses")
    suspend fun history(@Query("deviceId") deviceId: String, @Query("limit") limit: Int = 50): WorkerHistoryResponse
}

@JsonClass(generateAdapter = true)
data class WorkerAnalyzeRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
    val methods: List<String> = emptyList(),
    val deviceId: String,
    val storeImage: Boolean = true
)

@JsonClass(generateAdapter = true)
data class WorkerAnalyzeResponse(
    val id: String,
    val pair: String,
    val trend: String,
    val signal: String,
    val confidence: Double,
    val entry: String,
    val stopLoss: String,
    val takeProfit: String,
    val riskReward: String,
    val reasoning: String,
    val method: List<String>,
    val providerUsed: String,
    val timestampMillis: Long,
    val imageKey: String?
)

@JsonClass(generateAdapter = true)
data class WorkerHistoryResponse(
    val analyses: List<Map<String, Any?>> = emptyList()
)
