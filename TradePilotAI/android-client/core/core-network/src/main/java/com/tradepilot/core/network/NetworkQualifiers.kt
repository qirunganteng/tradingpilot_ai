package com.tradepilot.core.network

import org.koin.core.qualifier.named

/**
 * Beberapa binding Koin di project ini mengembalikan tipe okhttp3.Interceptor
 * yang sama secara literal (NetworkModule punya 1, WorkerModule di data-ai
 * punya 2 lagi) -- tanpa qualifier, Koin tidak tahu instance mana yang harus
 * di-inject ke OkHttpClient yang mana kalau dua-duanya bertipe sama persis.
 * Qualifier `named(...)` di bawah ini memberi identitas unik ke tiap
 * Interceptor (dulu: annotation class custom ala Dagger @Qualifier).
 */
val ApiKeyInterceptorQualifier = named("ApiKeyInterceptor")
val BaseUrlInterceptorQualifier = named("BaseUrlInterceptor")
val GatewayTokenInterceptorQualifier = named("GatewayTokenInterceptor")
