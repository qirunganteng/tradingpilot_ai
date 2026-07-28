package com.tradepilot.core.common.di

import com.tradepilot.core.common.EventBus
import com.tradepilot.core.common.PendingAnalysisHolder
import org.koin.dsl.module

val coreCommonModule = module {
    single { EventBus() }
    single { PendingAnalysisHolder() }
}
