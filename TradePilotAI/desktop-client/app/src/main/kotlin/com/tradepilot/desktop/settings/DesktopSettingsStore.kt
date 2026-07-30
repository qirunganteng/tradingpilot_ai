package com.tradepilot.desktop.settings

import com.tradepilot.domain.config.GatewayConfig
import java.io.File
import java.util.Properties

/**
 * Penyimpanan setting desktop-client yang persisten antar sesi (dulu:
 * TRADEPILOT_GATEWAY_URL/TOKEN cuma bisa diisi lewat environment variable
 * -- harus di-set ulang tiap buka terminal baru, tidak ramah non-developer).
 *
 * Disimpan sebagai file properties di folder home user, sama seperti
 * keystore.properties di android-client (baca komentar di app/build.gradle.kts
 * sana). Fase 9: token sekarang dienkripsi AES-256-GCM (DesktopCrypto.kt)
 * sebelum ditulis ke file -- lihat catatan batasan di sana (bukan
 * hardware-backed seperti Tink/Android Keystore, tapi jauh lebih baik dari
 * plain text Fase 8). URL tidak dienkripsi (bukan data sensitif).
 *
 * Env var TRADEPILOT_GATEWAY_URL/TOKEN TETAP didukung dan PRIORITAS lebih
 * tinggi dari file ini -- berguna untuk CI/scripted run tanpa perlu isi
 * Settings panel secara manual.
 */
data class DesktopSettings(
    val gatewayUrl: String = "",
    val gatewayToken: String = ""
) {
    fun toGatewayConfig(): GatewayConfig =
        GatewayConfig(baseUrl = gatewayUrl.trimEnd('/'), authToken = gatewayToken)
}

object DesktopSettingsStore {

    private const val KEY_URL = "gateway.url"
    private const val KEY_TOKEN = "gateway.token"

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".tradepilot").apply { mkdirs() }
    }
    private val configFile: File by lazy { File(configDir, "desktop-client.properties") }

    /**
     * Resolusi urutan: environment variable dulu (buat CI/dev script),
     * baru fallback ke file settings (buat pemakaian sehari-hari lewat UI).
     */
    fun resolve(): GatewayConfig {
        val envUrl = System.getenv("TRADEPILOT_GATEWAY_URL")?.trimEnd('/')
        val envToken = System.getenv("TRADEPILOT_GATEWAY_TOKEN")
        if (!envUrl.isNullOrBlank() && !envToken.isNullOrBlank()) {
            return GatewayConfig(baseUrl = envUrl, authToken = envToken)
        }
        return load().toGatewayConfig()
    }

    fun load(): DesktopSettings {
        if (!configFile.exists()) return DesktopSettings()
        return try {
            val props = Properties()
            configFile.inputStream().use { props.load(it) }
            val encryptedToken = props.getProperty(KEY_TOKEN, "")
            DesktopSettings(
                gatewayUrl = props.getProperty(KEY_URL, ""),
                gatewayToken = DesktopCrypto.decrypt(encryptedToken) ?: ""
            )
        } catch (e: Exception) {
            // File korup/tidak bisa dibaca -- jangan crash aplikasi cuma
            // gara-gara settings, anggap saja belum dikonfigurasi.
            DesktopSettings()
        }
    }

    fun save(settings: DesktopSettings) {
        val props = Properties()
        props.setProperty(KEY_URL, settings.gatewayUrl)
        props.setProperty(KEY_TOKEN, DesktopCrypto.encrypt(settings.gatewayToken))
        configFile.outputStream().use {
            props.store(it, "TradePilot AI desktop-client settings -- lihat DesktopSettingsStore.kt")
        }
    }
}
