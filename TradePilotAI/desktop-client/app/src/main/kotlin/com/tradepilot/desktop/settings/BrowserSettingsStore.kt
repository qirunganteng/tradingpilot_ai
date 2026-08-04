package com.tradepilot.desktop.settings

import java.io.File
import java.util.Properties

/**
 * Pengaturan BROWSER (beda dari [DesktopSettingsStore] yang isinya Gateway/
 * API key -- lihat catatan bug #3 di BrowserSettingsDialog.kt untuk kenapa
 * dua hal ini dulu tercampur jadi satu dialog).
 *
 * Pola sama seperti store lain (Properties, folder `~/.tradepilot/`, gagal
 * baca/tulis TIDAK BOLEH crash app).
 */
enum class StartupMode { NEW_TAB, CONTINUE_SESSION, SPECIFIC_PAGES }

data class BrowserSettings(
    val startupMode: StartupMode = StartupMode.CONTINUE_SESSION,
    /** Dipakai kalau startupMode == SPECIFIC_PAGES -- satu URL per baris di UI, disimpan comma-separated di sini. */
    val specificStartupUrls: List<String> = emptyList()
)

object BrowserSettingsStore {

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".tradepilot").apply { mkdirs() }
    }
    private val file: File by lazy { File(configDir, "browser-settings.properties") }

    fun load(): BrowserSettings {
        if (!file.exists()) return BrowserSettings()
        return try {
            val props = Properties()
            file.inputStream().use { props.load(it) }
            val mode = try {
                StartupMode.valueOf(props.getProperty("startupMode", StartupMode.CONTINUE_SESSION.name))
            } catch (t: Throwable) {
                StartupMode.CONTINUE_SESSION
            }
            val urls = props.getProperty("specificStartupUrls", "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            BrowserSettings(startupMode = mode, specificStartupUrls = urls)
        } catch (t: Throwable) {
            println("[BrowserSettingsStore] Gagal baca, fallback ke default: ${t.message}")
            BrowserSettings()
        }
    }

    fun save(settings: BrowserSettings) {
        try {
            val props = Properties()
            props.setProperty("startupMode", settings.startupMode.name)
            props.setProperty("specificStartupUrls", settings.specificStartupUrls.joinToString(","))
            file.outputStream().use { props.store(it, "TradePilot AI desktop-client browser settings -- lihat BrowserSettingsStore.kt") }
        } catch (t: Throwable) {
            println("[BrowserSettingsStore] Gagal simpan: ${t.message}")
        }
    }
}
