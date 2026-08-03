package com.tradepilot.desktop.session

import java.io.File
import java.util.Properties

/**
 * FASE 2 (Browser Session / Browser Restore) -- SEBELUM ini, app SELALU
 * mulai dari 1 window + 1 tab Exness setiap dibuka, tidak peduli window/tab
 * apa saja yang terbuka saat terakhir ditutup. Ini menyimpan & memulihkan
 * itu, mirip "Continue where you left off" di Chrome.
 *
 * Format: `java.util.Properties` (pola sama dengan DesktopSettingsStore.kt
 * -- TIDAK menambah dependency JSON baru, dan Properties sudah handle
 * escaping karakter aneh di title/url dengan benar secara bawaan, jadi
 * aman untuk title halaman apa pun). Key diberi index eksplisit
 * (`window.0.tab.1.url`, dst) supaya gampang dibaca manual di file kalau
 * perlu debug.
 *
 * Window INCOGNITO SENGAJA tidak pernah disimpan sama sekali (baik sebagai
 * window listing maupun tab-nya) -- ini bukan bug, ini prinsip privasi
 * dasar incognito yang sama di semua browser: sesi incognito tidak pernah
 * dipulihkan setelah ditutup/restart, titik.
 */
data class SessionTab(
    val url: String,
    val title: String,
    val isPinned: Boolean
)

data class SessionWindow(
    val id: Int,
    val tabs: List<SessionTab>
)

object SessionStore {

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".tradepilot").apply { mkdirs() }
    }
    private val sessionFile: File by lazy { File(configDir, "session.properties") }

    /** Dipanggil terus-menerus (debounced di sisi pemanggil, lihat Main.kt) -- SELALU overwrite seluruh file dengan snapshot window NON-incognito saat ini. */
    fun save(windows: List<SessionWindow>) {
        try {
            val props = Properties()
            props.setProperty("window.count", windows.size.toString())
            windows.forEachIndexed { windowIndex, window ->
                props.setProperty("window.$windowIndex.id", window.id.toString())
                props.setProperty("window.$windowIndex.tab.count", window.tabs.size.toString())
                window.tabs.forEachIndexed { tabIndex, tab ->
                    props.setProperty("window.$windowIndex.tab.$tabIndex.url", tab.url)
                    props.setProperty("window.$windowIndex.tab.$tabIndex.title", tab.title)
                    props.setProperty("window.$windowIndex.tab.$tabIndex.pinned", tab.isPinned.toString())
                }
            }
            sessionFile.outputStream().use {
                props.store(it, "TradePilot AI desktop-client session -- lihat SessionStore.kt. JANGAN edit manual saat app berjalan (akan ketimpa).")
            }
        } catch (t: Throwable) {
            // Gagal simpan session TIDAK BOLEH meng-crash aplikasi -- paling
            // buruk cuma kembali ke default (1 window + 1 tab Exness) di
            // restart berikutnya, lihat load().
            println("[SessionStore] Gagal simpan session: ${t.message}")
        }
    }

    /** Kosong (bukan null/exception) kalau belum pernah ada session tersimpan, atau file korup -- pemanggil (Main.kt) fallback ke default kalau ini kosong. */
    fun load(): List<SessionWindow> {
        if (!sessionFile.exists()) return emptyList()
        return try {
            val props = Properties()
            sessionFile.inputStream().use { props.load(it) }
            val windowCount = props.getProperty("window.count", "0").toIntOrNull() ?: 0
            (0 until windowCount).mapNotNull { windowIndex ->
                val id = props.getProperty("window.$windowIndex.id")?.toIntOrNull() ?: return@mapNotNull null
                val tabCount = props.getProperty("window.$windowIndex.tab.count", "0").toIntOrNull() ?: 0
                val tabs = (0 until tabCount).mapNotNull { tabIndex ->
                    val url = props.getProperty("window.$windowIndex.tab.$tabIndex.url") ?: return@mapNotNull null
                    if (url.isBlank()) return@mapNotNull null
                    SessionTab(
                        url = url,
                        title = props.getProperty("window.$windowIndex.tab.$tabIndex.title", url),
                        isPinned = props.getProperty("window.$windowIndex.tab.$tabIndex.pinned", "false").toBoolean()
                    )
                }
                if (tabs.isEmpty()) return@mapNotNull null // window tanpa tab valid -- jangan pulihkan window kosong.
                SessionWindow(id = id, tabs = tabs)
            }
        } catch (t: Throwable) {
            // File korup/format berubah dari versi lama -- anggap saja tidak
            // ada session tersimpan, JANGAN crash startup app cuma gara-gara
            // ini (prinsip sama seperti DesktopSettingsStore.load()).
            println("[SessionStore] Gagal baca session, fallback ke default: ${t.message}")
            emptyList()
        }
    }

    /** Tersedia untuk pemakaian manual/debug (mis. tombol "Reset semua data" di Settings kalau ditambahkan nanti) -- SENGAJA TIDAK disambungkan ke "Clear Browsing Data" di menu browser: itu soal cookie/cache, beda konsep dari daftar tab yang sedang terbuka, dan autosave 500ms di Workbench.kt akan langsung menulis ulang file ini selama tab masih ada. */
    fun clear() {
        try {
            if (sessionFile.exists()) sessionFile.delete()
        } catch (t: Throwable) {
            println("[SessionStore] Gagal hapus file session: ${t.message}")
        }
    }
}
