package com.tradepilot.data.ai.provider

import android.util.Base64
import com.tradepilot.data.ai.remote.GeminiApiService
import com.tradepilot.data.ai.remote.GeminiContent
import com.tradepilot.data.ai.remote.GeminiInlineData
import com.tradepilot.data.ai.remote.GeminiPart
import com.tradepilot.data.ai.remote.GeminiRequest
import com.tradepilot.domain.model.AnalysisResult
import javax.inject.Inject

/**
 * Implementasi nyata AIProvider memakai Google Gemini Free API.
 * Kirim gambar (base64) + prompt terstruktur, terima teks JSON,
 * di-parse oleh AnalysisResponseMapper.
 *
 * API key TIDAK di sini — dikirim otomatis lewat header oleh
 * ApiKeyInterceptor di core-network/NetworkModule (dibaca dari SecureKeyStore).
 */
class GeminiProvider @Inject constructor(
    private val service: GeminiApiService,
    private val responseMapper: AnalysisResponseMapper
) : AIProvider {

    override val providerName: String = "gemini"

    override suspend fun analyzeChart(
        imageBytes: ByteArray,
        promptText: String
    ): Result<AnalysisResult> {
        return try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = promptText),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = "image/jpeg",
                                    data = base64Image
                                )
                            )
                        )
                    )
                )
            )

            val response = service.generateContent(request = request)
            val rawText = response.candidates
                .firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: return Result.failure(IllegalStateException("Gemini tidak mengembalikan teks analisa"))

            responseMapper.map(rawText, providerName, methods = emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
