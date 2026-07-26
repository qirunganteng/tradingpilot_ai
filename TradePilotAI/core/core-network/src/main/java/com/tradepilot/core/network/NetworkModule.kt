package com.tradepilot.core.network

import com.tradepilot.core.security.SecureKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Setup OkHttp/Retrofit dasar.
 *
 * Certificate pinning untuk domain Gemini & Exness akan ditambahkan
 * di sini pada Fase 9 (Security Hardening) — struktur sudah disiapkan
 * lewat `OkHttpClient.Builder` agar tinggal chain `.certificatePinner(...)`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @ApiKeyInterceptorQualifier
    fun provideApiKeyInterceptor(secureKeyStore: SecureKeyStore): Interceptor =
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

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApiKeyInterceptorQualifier apiKeyInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // jangan BODY di release: bisa bocorkan API key
        }
        return OkHttpClient.Builder()
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
