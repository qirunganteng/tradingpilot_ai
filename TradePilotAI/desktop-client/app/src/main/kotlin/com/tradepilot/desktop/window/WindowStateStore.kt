package com.tradepilot.desktop.window

import java.io.File
import java.util.Properties

/**
 * FASE 2 (Window Manager -- item "Window State") -- SEBELUM ini, window
 * SELALU mulai di 1280x800 posisi default OS setiap app dibuka, walau user
 * sudah menggeser/mengubah ukurannya sebelum menutup app. Ini menyimpan &
 * memulihkan posisi+ukuran+status maximize TERAKHIR per window id.
 *
 * Pola sama dengan DesktopSettingsStore.kt/SessionStore.kt (Properties,
 * folder `~/.tradepilot/`, gagal baca/tulis TIDAK BOLEH crash app -- paling
 * buruk fallback ke default 1280x800).
 *
 * Window id di sini SENGAJA memakai id yang SAMA dengan yang dipakai
 * SessionStore (lihat Main.kt -- WindowSpec.id) supaya window yang
 * dipulihkan dari sesi juga otomatis dapat balik ukuran/posisi lamanya,
 * tanpa perlu sinkronisasi id terpisah.
 */
data class SavedWindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMaximized: Boolean
)

object WindowStateStore {

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".tradepilot").apply { mkdirs() }
    }
    private val stateFile: File by lazy { File(configDir, "window-state.properties") }

    fun save(windowId: Int, bounds: SavedWindowBounds) {
        try {
            val props = loadProps()
            props.setProperty("window.$windowId.x", bounds.x.toString())
            props.setProperty("window.$windowId.y", bounds.y.toString())
            props.setProperty("window.$windowId.width", bounds.width.toString())
            props.setProperty("window.$windowId.height", bounds.height.toString())
            props.setProperty("window.$windowId.maximized", bounds.isMaximized.toString())
            // "Terakhir dipakai" -- fallback untuk window BARU (id belum
            // pernah tersimpan sebelumnya, mis. hasil klik "New Window") biar
            // ukurannya konsisten dengan window lain yang sudah di-resize
            // user, bukan selalu balik ke 1280x800 hardcoded.
            props.setProperty("lastUsed.width", bounds.width.toString())
            props.setProperty("lastUsed.height", bounds.height.toString())
            stateFile.outputStream().use {
                props.store(it, "TradePilot AI desktop-client window bounds -- lihat WindowStateStore.kt")
            }
        } catch (t: Throwable) {
            println("[WindowStateStore] Gagal simpan window state: ${t.message}")
        }
    }

    fun load(windowId: Int): SavedWindowBounds? {
        return try {
            val props = loadProps()
            val x = props.getProperty("window.$windowId.x")?.toIntOrNull()
            val y = props.getProperty("window.$windowId.y")?.toIntOrNull()
            val width = props.getProperty("window.$windowId.width")?.toIntOrNull()
            val height = props.getProperty("window.$windowId.height")?.toIntOrNull()
            if (x == null || y == null || width == null || height == null) return loadLastUsedSizeOnly()
            SavedWindowBounds(
                x = x, y = y, width = width, height = height,
                isMaximized = props.getProperty("window.$windowId.maximized", "false").toBoolean()
            )
        } catch (t: Throwable) {
            println("[WindowStateStore] Gagal baca window state: ${t.message}")
            null
        }
    }

    /** Fallback untuk window id yang belum pernah tersimpan -- posisi tetap default (biar tidak numpuk persis di titik yang sama), TAPI ukuran ikut yang terakhir dipakai. */
    private fun loadLastUsedSizeOnly(): SavedWindowBounds? {
        val props = loadProps()
        val width = props.getProperty("lastUsed.width")?.toIntOrNull() ?: return null
        val height = props.getProperty("lastUsed.height")?.toIntOrNull() ?: return null
        return SavedWindowBounds(x = -1, y = -1, width = width, height = height, isMaximized = false)
    }

    private fun loadProps(): Properties {
        val props = Properties()
        if (stateFile.exists()) {
            try {
                stateFile.inputStream().use { props.load(it) }
            } catch (t: Throwable) {
                // File korup -- mulai dari kosong, jangan crash.
            }
        }
        return props
    }
}
