package com.tradepilot.data.ai.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface untuk Gemini Free API (endpoint generateContent).
 * Base URL diatur di GeminiModule: https://generativelanguage.googleapis.com/v1beta/
 * API key dikirim lewat header x-goog-api-key (lihat core-network/NetworkModule).
 */
interface GeminiApiService {

    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String = "gemini-1.5-flash",
        @Body request: GeminiRequest
    ): GeminiResponse
}
