package com.tradepilot.desktop.updater

import java.io.File
import java.util.Properties

/**
 * State auto-updater yang persisten antar sesi -- disimpan di folder yang
 * sama dengan DesktopSettingsStore (~/.tradepilot/), pola identik (lihat
 * settings/DesktopSettingsStore.kt).
 *
 * Cuma menyimpan satu hal: SHA versi yang user pilih "Lewati versi ini",
 * supaya tidak terus-menerus ditawari update yang sama tiap app dibuka.
 */
object UpdatePreferences {
    private const val KEY_SKIPPED_SHA = "update.skippedSha"

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".tradepilot").apply { mkdirs() }
    }
    private val configFile: File by lazy { File(configDir, "update-prefs.properties") }

    fun getSkippedSha(): String? {
        if (!configFile.exists()) return null
        return try {
            val props = Properties()
            configFile.inputStream().use { props.load(it) }
            props.getProperty(KEY_SKIPPED_SHA)?.ifBlank { null }
        } catch (e: Exception) {
            // File korup/tidak bisa dibaca -- anggap saja belum ada yang di-skip.
            null
        }
    }

    fun setSkippedSha(sha: String) {
        try {
            val props = Properties()
            props.setProperty(KEY_SKIPPED_SHA, sha)
            configFile.outputStream().use {
                props.store(it, "TradePilot AI update preferences -- lihat UpdatePreferences.kt")
            }
        } catch (e: Exception) {
            // Gagal simpan preference bukan alasan buat crash -- paling
            // banter user ditanya lagi update yang sama di sesi berikutnya.
        }
    }
}
