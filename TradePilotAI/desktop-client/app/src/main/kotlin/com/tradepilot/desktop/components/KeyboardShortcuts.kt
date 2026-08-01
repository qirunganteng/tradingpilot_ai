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
    /** Prioritas 9: "ESC keluar fullscreen" -- dipisah dari toggleFullscreen
     *  supaya Esc TIDAK MASUK fullscreen kalau ditekan saat bukan fullscreen
     *  (beda dengan F11 yang memang toggle 2 arah). */
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
        event.key == Key.Escape && actions.isFullscreen() -> { actions.exitFullscreen(); true }
        event.isAltPressed && event.key == Key.DirectionLeft -> { actions.goBack(); true }
        event.isAltPressed && event.key == Key.DirectionRight -> { actions.goForward(); true }
        else -> false
    }
}

/** Sugar supaya pemanggilan di Workbench ringkas: `Modifier.browserShortcuts(actions)`. */
fun Modifier.browserShortcuts(actions: BrowserShortcutActions): Modifier =
    this.onPreviewKeyEvent { handleBrowserShortcuts(it, actions) }
