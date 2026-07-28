package com.tradepilot.data.ai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tradepilot.data.ai.remote.GeminiApiService
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

val dataAiGeminiModule = module {

    single {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        val okHttpClient = get<OkHttpClient>() // OkHttpClient default dari :core-network (coreNetworkModule)
        val moshi = get<Moshi>()
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}
