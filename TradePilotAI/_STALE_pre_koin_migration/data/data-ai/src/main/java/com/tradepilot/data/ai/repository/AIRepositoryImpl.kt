package com.tradepilot.data.ai.repository

import com.tradepilot.data.ai.provider.ProviderFactory
import com.tradepilot.data.ai.provider.ProviderType
import com.tradepilot.data.ai.provider.PromptBuilder
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.repository.AIRepository
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val providerFactory: ProviderFactory
) : AIRepository {

    override suspend fun analyzeChart(
        imageBytes: ByteArray,
        methods: List<String>
    ): Result<AnalysisResult> {
        val provider = providerFactory.create(ProviderType.WORKER_GATEWAY) // default: lewat Cloudflare Worker AI Gateway
        val prompt = PromptBuilder.buildChartAnalysisPrompt(methods)

        return provider.analyzeChart(imageBytes, prompt).map { result ->
            result.copy(method = methods.ifEmpty { result.method })
        }
    }
}
