package com.tradepilot.desktop.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Prioritas 10 (Browser UX): kumpulan shortcut yang dipetakan ke aksi.
 * Sengaja dipisah jadi 1 objek supaya Layouts/Workbench.kt tidak berisi
 * blok if/else raksasa -- tinggal isi lambda-nya.
 *
 * Middle Click buka tab baru DIWIRE terpisah di TabBar/BrowserTabsBar.kt
 * (di tombol "+") karena itu event mouse, bukan keyboard -- tidak masuk
 * di sini.
 */
class BrowserShortcutActions(
    val newTab: () -> Unit,
    val closeTab: () -> Unit,
    val reopenClosedTab: () -> Unit,
    val nextTab: () -> Unit,
    val focusAddressBar: () -> Unit,
    val reload: () -> Unit,
    val openFind: () -> Unit,
    val toggleBookmark: () -> Unit,
    val toggleFullscreen: () -> Unit,
    val goBack: () -> Unit,
    val goForward: () -> Unit,
    // Prioritas 9: "ESC keluar fullscreen" -- terpisah dari toggleFullscreen
    // (F11) karena ESC HANYA boleh keluar dari fullscreen, tidak boleh
    // masuk ke fullscreen kalau belum aktif (beda arah dari F11 yang toggle
    // dua arah).
    val isFullscreen: () -> Boolean,
    val exitFullscreen: () -> Unit
)

/**
 * Dipasang di root Composable Workbench lewat
 * `Modifier.onPreviewKeyEvent { handleBrowserShortcuts(it, actions) }`.
 * Return true kalau event ditangani (supaya tidak diteruskan lagi, misalnya
 * ke address bar yang sedang fokus).
 */
fun handleBrowserShortcuts(event: KeyEvent, actions: BrowserShortcutActions): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    return when {
        event.key == Key.Escape && actions.isFullscreen() -> { actions.exitFullscreen(); true }
        event.isCtrlPressed && event.isShiftPressed && event.key == Key.T -> { actions.reopenClosedTab(); true }
        event.isCtrlPressed && event.key == Key.T -> { actions.newTab(); true }
        event.isCtrlPressed && event.key == Key.W -> { actions.closeTab(); true }
        event.isCtrlPressed && event.key == Key.Tab -> { actions.nextTab(); true }
        event.isCtrlPressed && event.key == Key.L -> { actions.focusAddressBar(); true }
        event.isCtrlPressed && event.key == Key.R -> { actions.reload(); true }
        event.isCtrlPressed && event.key == Key.F -> { actions.openFind(); true }
        event.isCtrlPressed && event.key == Key.D -> { actions.toggleBookmark(); true }
        event.key == Key.F5 -> { actions.reload(); true }
        event.key == Key.F11 -> { actions.toggleFullscreen(); true }
        event.isAltPressed && event.key == Key.DirectionLeft -> { actions.goBack(); true }
        event.isAltPressed && event.key == Key.DirectionRight -> { actions.goForward(); true }
        else -> false
    }
}

/** Sugar supaya pemanggilan di Workbench ringkas: `Modifier.browserShortcuts(actions)`. */
fun Modifier.browserShortcuts(actions: BrowserShortcutActions): Modifier =
    this.onPreviewKeyEvent { handleBrowserShortcuts(it, actions) }

/**
 * Varian raw Windows-VK-code dari [handleBrowserShortcuts], dipakai dari
 * [com.tradepilot.desktop.browser.JCEFBrowserEngine.onNativeKeyDown] --
 * lihat catatan panjang di JCEFBrowserEngine.kt soal kenapa varian ini
 * perlu ada sama sekali (Focus Management, fokus di komponen browser
 * native tidak pernah sampai ke Compose).
 *
 * SENGAJA dijaga 1:1 dengan `handleBrowserShortcuts` di atas (kombinasi
 * tombol yang sama, urutan pengecekan yang sama) -- kalau nambah shortcut
 * baru di salah satu, cerminkan juga ke yang satunya, supaya perilaku app
 * konsisten mau fokus lagi di Compose atau di dalam halaman web.
 * `java.awt.event.KeyEvent.VK_*` dipakai sebagai kode referensi karena
 * nilainya identik dengan Windows virtual-key code yang dikirim CEF lewat
 * `windows_key_code` untuk huruf/tombol umum -- bukan kebetulan, memang
 * dirancang selaras oleh AWT.
 */
fun handleBrowserShortcutsNative(
    windowsKeyCode: Int,
    isCtrl: Boolean,
    isShift: Boolean,
    isAlt: Boolean,
    actions: BrowserShortcutActions
): Boolean {
    return when {
        windowsKeyCode == java.awt.event.KeyEvent.VK_ESCAPE && actions.isFullscreen() -> { actions.exitFullscreen(); true }
        isCtrl && isShift && windowsKeyCode == java.awt.event.KeyEvent.VK_T -> { actions.reopenClosedTab(); true }
        isCtrl && windowsKeyCode == java.awt.event.KeyEvent.VK_T -> { actions.newTab(); true }
        isCtrl && windowsKeyCode == java.awt.event.KeyEvent.VK_W -> { actions.closeTab(); true }
        isCtrl && windowsKeyCode == java.awt.event.KeyEvent.VK_TAB -> { actions.nextTab(); true }
        isCtrl && windowsKeyCode == java.awt.event.KeyEvent.VK_L -> { actions.focusAddressBar(); true }
        isCtrl && windowsKeyCode == java.awt.event.KeyEvent.VK_R -> { actions.reload(); true }
        isCtrl && windowsKeyCode == java.awt.event.KeyEvent.VK_F -> { actions.openFind(); true }
        isCtrl && windowsKeyCode == java.awt.event.KeyEvent.VK_D -> { actions.toggleBookmark(); true }
        windowsKeyCode == java.awt.event.KeyEvent.VK_F5 -> { actions.reload(); true }
        windowsKeyCode == java.awt.event.KeyEvent.VK_F11 -> { actions.toggleFullscreen(); true }
        isAlt && windowsKeyCode == java.awt.event.KeyEvent.VK_LEFT -> { actions.goBack(); true }
        isAlt && windowsKeyCode == java.awt.event.KeyEvent.VK_RIGHT -> { actions.goForward(); true }
        else -> false
    }
}
