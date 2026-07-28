package com.tradepilot.app.di

import com.tradepilot.domain.usecase.AnalyzeChartUseCase
import com.tradepilot.domain.usecase.CalculateJournalStatisticsUseCase
import com.tradepilot.domain.usecase.CalculateRiskUseCase
import com.tradepilot.domain.usecase.GenerateHistoryInsightUseCase
import com.tradepilot.domain.usecase.GenerateMentorFeedbackUseCase
import com.tradepilot.domain.usecase.ObserveTradeHistoryUseCase
import com.tradepilot.domain.usecase.SaveTradeEntryUseCase
import org.koin.dsl.module

/**
 * Semua use case di :shared TIDAK boleh bergantung ke Koin (Konstitusi:
 * shared harus pure Kotlin, tanpa framework DI apa pun). Karena itu
 * registrasinya dipusatkan di sini (:app, composition root) -- bukan di
 * masing-masing gradle module data/feature yang memakainya -- supaya
 * tidak ada risiko dua module berbeda mendaftarkan use case yang sama
 * dua kali ("already registered" saat startKoin).
 *
 * `DeriveCopilotSignalUseCase` sengaja tidak didaftarkan di sini karena
 * itu `object` (bukan class) -- dipanggil langsung tanpa Koin.
 */
val useCaseModule = module {
    factory { CalculateRiskUseCase() }
    factory { AnalyzeChartUseCase(get()) }
    factory { SaveTradeEntryUseCase(get()) }
    factory { ObserveTradeHistoryUseCase(get()) }
    factory { CalculateJournalStatisticsUseCase() }
    factory { GenerateHistoryInsightUseCase() }
    factory { GenerateMentorFeedbackUseCase() }
}
