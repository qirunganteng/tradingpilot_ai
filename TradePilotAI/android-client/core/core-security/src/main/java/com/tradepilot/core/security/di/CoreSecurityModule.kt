package com.tradepilot.core.security.di

import com.tradepilot.core.security.SecureKeyStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreSecurityModule = module {
    single { SecureKeyStore(androidContext()) }
}
