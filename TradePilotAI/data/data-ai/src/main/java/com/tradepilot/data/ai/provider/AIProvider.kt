package com.tradepilot.data.ai.provider

import com.tradepilot.domain.model.AnalysisResult

/**
 * Kontrak provider AI. Gemini adalah implementasi DEFAULT, tapi provider
 * lain (OpenAI/Claude/DeepSeek/Qwen) bisa ditambahkan tanpa mengubah
 * AIRepositoryImpl maupun UseCase manapun — cukup implementasi baru
 * dari interface ini dan didaftarkan di ProviderFactory.
 */
interface AIProvider {
    val providerName: String

    suspend fun analyzeChart(
        imageBytes: ByteArray,
        promptText: String
    ): Result<AnalysisResult>
}
