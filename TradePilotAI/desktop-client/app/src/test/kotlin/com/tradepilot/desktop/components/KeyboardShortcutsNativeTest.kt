package com.tradepilot.desktop.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.event.KeyEvent.VK_D
import java.awt.event.KeyEvent.VK_ESCAPE
import java.awt.event.KeyEvent.VK_F
import java.awt.event.KeyEvent.VK_F5
import java.awt.event.KeyEvent.VK_F11
import java.awt.event.KeyEvent.VK_L
import java.awt.event.KeyEvent.VK_LEFT
import java.awt.event.KeyEvent.VK_R
import java.awt.event.KeyEvent.VK_RIGHT
import java.awt.event.KeyEvent.VK_T
import java.awt.event.KeyEvent.VK_TAB
import java.awt.event.KeyEvent.VK_W

/**
 * Browser Testing (item terakhir FASE 1) -- menguji
 * [handleBrowserShortcutsNative], jalur Focus Management yang dipasang ke
 * [com.tradepilot.desktop.browser.JCEFBrowserEngine.onNativeKeyDown] (lihat
 * catatan panjang di sana). Ini fungsi PURE (Int/Boolean masuk, Boolean
 * keluar, efek samping cuma manggil lambda di [BrowserShortcutActions]) --
 * TIDAK butuh Compose runtime/AWT window/JCEF native sama sekali, aman
 * dijalankan di JVM headless biasa.
 *
 * `handleBrowserShortcuts` (varian Compose, dipasang lewat
 * `Modifier.onPreviewKeyEvent`) SENGAJA TIDAK diuji terpisah di sini --
 * butuh membangun objek `androidx.compose.ui.input.key.KeyEvent` sungguhan,
 * yang berisiko butuh native Skia ter-load di JVM headless test (di luar
 * scope "unit test murni tanpa native" yang dijanjikan di docs/TESTING.md).
 * Keduanya SUDAH dijaga 1:1 lewat komentar di KeyboardShortcuts.kt --
 * kalau menambah shortcut baru, tetap perlu diperbarui manual di dua
 * tempat & jangan lupa cerminkan ke sini juga.
 */
class KeyboardShortcutsNativeTest {

    /** Mock [BrowserShortcutActions] yang mencatat aksi mana yang terpanggil, tanpa Compose/JCEF sungguhan. */
    private class RecordingActions(private var fullscreen: Boolean = false) {
        val calls = mutableListOf<String>()

        val actions = BrowserShortcutActions(
            newTab = { calls += "newTab" },
            closeTab = { calls += "closeTab" },
            reopenClosedTab = { calls += "reopenClosedTab" },
            nextTab = { calls += "nextTab" },
            focusAddressBar = { calls += "focusAddressBar" },
            reload = { calls += "reload" },
            openFind = { calls += "openFind" },
            toggleBookmark = { calls += "toggleBookmark" },
            toggleFullscreen = { calls += "toggleFullscreen" },
            goBack = { calls += "goBack" },
            goForward = { calls += "goForward" },
            isFullscreen = { fullscreen },
            exitFullscreen = { calls += "exitFullscreen" }
        )

        fun setFullscreen(value: Boolean) { fullscreen = value }
    }

    private lateinit var recorder: RecordingActions

    @Before
    fun setUp() {
        recorder = RecordingActions()
    }

    private fun press(
        keyCode: Int,
        ctrl: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false
    ): Boolean = handleBrowserShortcutsNative(keyCode, ctrl, shift, alt, recorder.actions)

    @Test
    fun `Ctrl T membuka tab baru`() {
        val handled = press(VK_T, ctrl = true)
        assertTrue(handled)
        assertEquals(listOf("newTab"), recorder.calls)
    }

    @Test
    fun `Ctrl Shift T membuka kembali tab yang ditutup, BUKAN newTab`() {
        val handled = press(VK_T, ctrl = true, shift = true)
        assertTrue(handled)
        assertEquals(listOf("reopenClosedTab"), recorder.calls)
    }

    @Test
    fun `Ctrl W menutup tab aktif`() {
        assertTrue(press(VK_W, ctrl = true))
        assertEquals(listOf("closeTab"), recorder.calls)
    }

    @Test
    fun `Ctrl Tab pindah ke tab berikutnya`() {
        assertTrue(press(VK_TAB, ctrl = true))
        assertEquals(listOf("nextTab"), recorder.calls)
    }

    @Test
    fun `Ctrl L fokus ke address bar`() {
        assertTrue(press(VK_L, ctrl = true))
        assertEquals(listOf("focusAddressBar"), recorder.calls)
    }

    @Test
    fun `Ctrl R reload`() {
        assertTrue(press(VK_R, ctrl = true))
        assertEquals(listOf("reload"), recorder.calls)
    }

    @Test
    fun `F5 reload juga (tanpa Ctrl)`() {
        assertTrue(press(VK_F5))
        assertEquals(listOf("reload"), recorder.calls)
    }

    @Test
    fun `Ctrl F membuka find bar`() {
        assertTrue(press(VK_F, ctrl = true))
        assertEquals(listOf("openFind"), recorder.calls)
    }

    @Test
    fun `Ctrl D toggle bookmark`() {
        assertTrue(press(VK_D, ctrl = true))
        assertEquals(listOf("toggleBookmark"), recorder.calls)
    }

    @Test
    fun `F11 toggle fullscreen aplikasi`() {
        assertTrue(press(VK_F11))
        assertEquals(listOf("toggleFullscreen"), recorder.calls)
    }

    @Test
    fun `Alt Left navigasi mundur`() {
        assertTrue(press(VK_LEFT, alt = true))
        assertEquals(listOf("goBack"), recorder.calls)
    }

    @Test
    fun `Alt Right navigasi maju`() {
        assertTrue(press(VK_RIGHT, alt = true))
        assertEquals(listOf("goForward"), recorder.calls)
    }

    // -- ESC: HANYA boleh keluar dari fullscreen, tidak boleh dipakai untuk
    // apa pun kalau aplikasi TIDAK sedang fullscreen (beda arah dari F11
    // yang dua arah) -- ini juga mekanisme di balik permintaan eksplisit
    // "ESC keluar dari fullscreen browser DAN aplikasi sekaligus" (lihat
    // JCEFBrowserEngine.exitPageFullscreen() & Workbench.kt).

    @Test
    fun `ESC keluar fullscreen kalau sedang fullscreen`() {
        recorder.setFullscreen(true)
        assertTrue(press(VK_ESCAPE))
        assertEquals(listOf("exitFullscreen"), recorder.calls)
    }

    @Test
    fun `ESC tidak melakukan apa pun kalau TIDAK sedang fullscreen`() {
        recorder.setFullscreen(false)
        assertFalse(press(VK_ESCAPE))
        assertTrue(recorder.calls.isEmpty())
    }

    // -- Kombinasi tombol yang tidak dikenal -> tidak boleh salah nyantol
    // ke shortcut lain (regresi kalau urutan `when` di atas berubah).

    @Test
    fun `tombol tanpa modifier yang tidak dipetakan tidak melakukan apa pun`() {
        assertFalse(press(VK_T)) // T tanpa Ctrl -- bukan shortcut, harusnya diketik biasa ke halaman
        assertTrue(recorder.calls.isEmpty())
    }

    @Test
    fun `Ctrl tanpa key yang cocok tidak melakukan apa pun`() {
        assertFalse(press(java.awt.event.KeyEvent.VK_Z, ctrl = true))
        assertTrue(recorder.calls.isEmpty())
    }
}
