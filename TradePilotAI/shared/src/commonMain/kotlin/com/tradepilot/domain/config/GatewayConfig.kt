package com.tradepilot.domain.config

/**
 * Konfigurasi koneksi ke Cloudflare Worker (AI Gateway). Data class murni
 * (tidak ada logic baca env var/BuildConfig di sini -- itu tanggung jawab
 * Platform Client masing-masing, karena caranya beda: android-client dari
 * BuildConfig/local.properties, desktop-client dari environment variable).
 *
 * baseUrl contoh: "https://tradepilot-ai-gateway.<subdomain>.workers.dev"
 * (TANPA trailing slash).
 */
data class GatewayConfig(
    val baseUrl: String,
    val authToken: String
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && authToken.isNotBlank()

    companion object {
        val NotConfigured = GatewayConfig(baseUrl = "", authToken = "")
    }
}
