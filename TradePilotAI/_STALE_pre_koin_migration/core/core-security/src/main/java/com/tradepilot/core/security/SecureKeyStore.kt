package com.tradepilot.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Penyimpanan terenkripsi untuk data sensitif — TERUTAMA Gemini API key.
 *
 * Aturan keras (lihat Blueprint bagian 13: Security Design):
 *  - API key TIDAK PERNAH ditulis di source code / BuildConfig.
 *  - User meng-input API key sendiri lewat Settings, disimpan di sini.
 *  - Tidak ada modul lain yang boleh mengakses SharedPreferences biasa
 *    untuk data ini.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "tradepilot_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API, apiKey).apply()
    }

    fun getGeminiApiKey(): String? = prefs.getString(KEY_GEMINI_API, null)

    fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_API).apply()
    }

    /**
     * Konfigurasi Cloudflare Worker AI Gateway (arsitektur baru, direkomendasikan).
     * Lewat pola ini, API key Gemini TIDAK PERNAH disimpan di device — hanya
     * baseUrl Worker + token gateway sederhana yang dikirim di header.
     */
    fun saveWorkerConfig(baseUrl: String, gatewayToken: String) {
        prefs.edit()
            .putString(KEY_WORKER_BASE_URL, baseUrl.trimEnd('/') + "/")
            .putString(KEY_WORKER_GATEWAY_TOKEN, gatewayToken)
            .apply()
    }

    fun getWorkerBaseUrl(): String? = prefs.getString(KEY_WORKER_BASE_URL, null)

    fun getWorkerGatewayToken(): String? = prefs.getString(KEY_WORKER_GATEWAY_TOKEN, null)

    fun clearWorkerConfig() {
        prefs.edit().remove(KEY_WORKER_BASE_URL).remove(KEY_WORKER_GATEWAY_TOKEN).apply()
    }

    /** ID anonim per-install (BUKAN identitas pribadi) untuk rate-limit & audit log di Worker. */
    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val generated = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_WORKER_BASE_URL = "worker_base_url"
        private const val KEY_WORKER_GATEWAY_TOKEN = "worker_gateway_token"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
