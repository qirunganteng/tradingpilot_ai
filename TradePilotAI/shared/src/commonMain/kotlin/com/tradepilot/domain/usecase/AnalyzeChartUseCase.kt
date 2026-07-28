package com.tradepilot.domain.usecase

import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.repository.AIRepository
import javax.inject.Inject

/**
 * Orkestrasi analisa chart (Fase 2). ViewModel di feature-ai memanggil
 * UseCase ini saja — tidak pernah menyentuh AIRepository/AIProvider langsung.
 */
class AnalyzeChartUseCase @Inject constructor(
    private val aiRepository: AIRepository
) {
    companion object {
        val DEFAULT_METHODS = listOf(
            "ICT", "SMC", "Liquidity", "Order Block", "Fair Value Gap",
            "BOS", "CHOCH", "Session", "PDH", "PDL", "Trend", "Momentum"
        )
    }

    suspend operator fun invoke(
        imageBytes: ByteArray,
        methods: List<String> = DEFAULT_METHODS
    ): Result<AnalysisResult> = aiRepository.analyzeChart(imageBytes, methods)
}
