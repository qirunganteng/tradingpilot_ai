package com.tradepilot.data.ai

import com.squareup.moshi.Moshi
import com.tradepilot.core.security.SecureKeyStore
import com.tradepilot.data.ai.remote.WorkerApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Placeholder host — DIGANTI oleh BaseUrlInterceptor sebelum request benar-benar dikirim. */
private const val PLACEHOLDER_BASE_URL = "https://tradepilot-worker.invalid/"

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WorkerRetrofit

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    /**
     * Retrofit butuh baseUrl valid saat dibuat (sekali, saat aplikasi start),
     * padahal user bisa mengubah URL Worker kapan saja di Settings. Solusinya:
     * pakai baseUrl placeholder yang TIDAK PERNAH benar-benar dihubungi —
     * interceptor ini menulis ulang scheme/host/port setiap request memakai
     * nilai TERBARU dari SecureKeyStore sebelum dikirim.
     */
    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(secureKeyStore: SecureKeyStore): Interceptor = Interceptor { chain ->
        val configuredBaseUrl = secureKeyStore.getWorkerBaseUrl()?.toHttpUrlOrNull()
        val original = chain.request()

        if (configuredBaseUrl == null) {
            // Belum dikonfigurasi -> biarkan gagal dengan error jelas, jangan diam-diam ke placeholder.
            throw WorkerNotConfiguredException()
        }

        val newUrl = original.url.newBuilder()
            .scheme(configuredBaseUrl.scheme)
            .host(configuredBaseUrl.host)
            .port(configuredBaseUrl.port)
            .build()

        chain.proceed(original.newBuilder().url(newUrl).build())
    }

    @Provides
    @Singleton
    fun provideGatewayTokenInterceptor(secureKeyStore: SecureKeyStore): Interceptor = Interceptor { chain ->
        val token = secureKeyStore.getWorkerGatewayToken()
        val request = if (token != null) {
            chain.request().newBuilder().addHeader("x-gateway-token", token).build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    @Provides
    @Singleton
    @WorkerRetrofit
    fun provideWorkerOkHttpClient(
        baseUrlInterceptor: Interceptor,
        gatewayTokenInterceptor: Interceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(gatewayTokenInterceptor)
            .addInterceptor(baseUrlInterceptor) // urutan: set header dulu, baru rewrite URL (urutan tidak saling bergantung sebenarnya, tapi konsisten)
            .addInterceptor(logging)
            // OPTIMASI PERFORMA: upload gambar (setelah downscale ~100-300KB) + Worker
            // memanggil Gemini + tulis D1/R2 di sisi server bisa makan beberapa detik.
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkerApiService(@WorkerRetrofit client: OkHttpClient, moshi: Moshi): WorkerApiService {
        return Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WorkerApiService::class.java)
    }
}

class WorkerNotConfiguredException :
    IllegalStateException("Worker AI Gateway belum dikonfigurasi. Isi URL Worker di Pengaturan.")
