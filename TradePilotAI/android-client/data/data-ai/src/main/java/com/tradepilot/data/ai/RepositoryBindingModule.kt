package com.tradepilot.data.ai

import com.tradepilot.data.ai.provider.AnalysisResponseMapper
import com.tradepilot.data.ai.provider.GeminiProvider
import com.tradepilot.data.ai.provider.ProviderFactory
import com.tradepilot.data.ai.provider.WorkerProvider
import com.tradepilot.data.ai.remote.WorkerApiService
import com.tradepilot.data.ai.repository.AIRepositoryImpl
import com.tradepilot.domain.repository.AIRepository
import org.koin.dsl.module

/**
 * Registrasi provider AI + ProviderFactory + binding AIRepository.
 * Digabung satu file (dulu terpisah GeminiModule/WorkerModule/RepositoryBindingModule
 * di sisi @Provides/@Binds) supaya jelas urutan dependency-nya untuk Koin:
 * provider butuh service dari GeminiModule/WorkerModule, ProviderFactory
 * butuh kedua provider, AIRepositoryImpl butuh ProviderFactory.
 */
val dataAiProviderModule = module {

    single { AnalysisResponseMapper(get()) }

    single { GeminiProvider(service = get(), responseMapper = get()) }

    single {
        WorkerProvider(
            service = get<WorkerApiService>(WorkerRetrofitQualifier),
            secureKeyStore = get()
        )
    }

    single { ProviderFactory(workerProvider = get(), geminiProvider = get()) }

    single<AIRepository> { AIRepositoryImpl(get()) }
}
