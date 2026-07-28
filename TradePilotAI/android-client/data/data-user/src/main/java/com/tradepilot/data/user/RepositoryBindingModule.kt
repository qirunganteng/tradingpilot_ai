package com.tradepilot.data.user

import com.tradepilot.domain.repository.SettingsRepository
import org.koin.dsl.module

val dataUserModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl() }
}
