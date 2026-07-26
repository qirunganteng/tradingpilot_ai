package com.tradepilot.core.network

import javax.inject.Qualifier

/**
 * Beberapa @Provides di project ini mengembalikan tipe okhttp3.Interceptor
 * yang sama secara literal (NetworkModule punya 1, WorkerModule di data-ai
 * punya 2 lagi) -- tanpa qualifier, Dagger menganggapnya binding duplikat
 * untuk tipe yang sama persis ([Dagger/DuplicateBindings]). Qualifier di
 * bawah ini memberi identitas unik ke tiap Interceptor supaya Dagger tahu
 * mana yang harus disuntikkan ke OkHttpClient yang mana.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiKeyInterceptorQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrlInterceptorQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GatewayTokenInterceptorQualifier
