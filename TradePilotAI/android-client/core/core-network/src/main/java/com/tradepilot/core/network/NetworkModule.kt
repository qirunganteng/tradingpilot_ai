package com.tradepilot.core.network

import com.tradepilot.core.security.SecureKeyStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/**
 * Setup OkHttp/Retrofit dasar.
 *
 * Certificate pinning untuk domain Gemini & Exness akan ditambahkan
 * di sini pada Fase 9 (Security Hardening) — struktur sudah disiapkan
 * lewat `OkHttpClient.Builder` agar tinggal chain `.certificatePinner(...)`.
 */
val coreNetworkModule = module {

    single(ApiKeyInterceptorQualifier) {
        val secureKeyStore = get<SecureKeyStore>()
        Interceptor { chain ->
            val apiKey = secureKeyStore.getGeminiApiKey()
            val request = if (apiKey != null) {
                chain.request().newBuilder()
                    .addHeader("x-goog-api-key", apiKey)
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
    }

    single {
        val apiKeyInterceptor = get<Interceptor>(ApiKeyInterceptorQualifier)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // jangan BODY di release: bisa bocorkan API key
        }
        OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(logging)
            // OPTIMASI PERFORMA: default OkHttp timeout (10s) terlalu pendek untuk
            // panggilan AI multimodal (upload gambar + inference Gemini bisa >10s,
            // terutama di koneksi seluler lambat). connectTimeout tetap singkat
            // supaya gagal cepat kalau memang tidak ada jaringan.
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // .certificatePinner(...) // TODO Fase 9
            .build()
    }
}
