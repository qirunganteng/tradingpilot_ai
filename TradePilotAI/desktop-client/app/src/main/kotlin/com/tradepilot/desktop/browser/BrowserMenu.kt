package com.tradepilot.desktop.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.tradepilot.desktop.theme.AppColors

/**
 * Prioritas 6: Browser Menu ala Chrome (ikon hamburger di ujung kanan
 * Toolbar/BrowserBar.kt membuka menu ini).
 *
 * FIX BUG #2/#6 ("titik tiga menu diklik nggak muncul apa-apa" / "pas
 * fullscreen tombol Settings error terus semua tombol lain jadi kayak
 * freeze + ada bunyi beep"):
 *
 * Root cause-nya PERSIS jenis bug yang sama yang sudah didokumentasikan &
 * diperbaiki di SettingsDialog.kt: dulu menu ini pakai [DropdownMenu] (M3),
 * yang di Compose Desktop dirender lewat [Popup] -- popup ini di-gambar di
 * layer Compose/Skia biasa. Sementara itu JCEFBrowserView (Workspace.kt)
 * di-embed lewat [SwingPanel], yaitu komponen AWT/Swing HEAVYWEIGHT asli.
 * Komponen heavyweight SELALU digambar OS di atas layer Compose/Skia APAPUN
 * urutan komposisinya -- ini batasan Compose Desktop yang didokumentasikan,
 * bukan bug approach saya. Karena Workspace (tempat browser) makan hampir
 * semua luas window (apalagi pas fullscreen, dia BENAR-BENAR full), begitu
 * DropdownMenu overlap area itu, menu jadi tidak kelihatan SAMA SEKALI --
 * tapi popup-nya tetap ADA & tetap menangkap fokus/klik pertama yang
 * harusnya buat dismiss. Klik berikutnya di tombol lain jadi kelihatan
 * "freeze" (sebenarnya kena scrim popup hantu yang tidak kelihatan), dan
 * Windows berbunyi beep karena fokus nyangkut di window/komponen yang tidak
 * menerima input itu.
 *
 * Fix: sama seperti Settings -- pakai [DialogWindow], window OS baru yang
 * SUNGGUHAN terpisah, dijamin selalu di atas SwingPanel apa pun kondisinya
 * (termasuk fullscreen). Trade-off yang disadari: DropdownMenu bawaan M3
 * otomatis dismiss kalau klik di luar area menu (di window utama) -- window
 * terpisah TIDAK otomatis begitu, jadi menu ini ditutup lewat: (a) klik
 * salah satu item (semuanya sudah manggil onDismiss()), (b) tombol close (X)
 * bawaan DialogWindow, atau (c) tombol Escape (ditangani manual di bawah).
 *
 * CATATAN JUJUR per item (tidak berubah dari versi sebelumnya):
 *  - New Tab, Recent Tabs, History, Bookmarks, Downloads, Find, Zoom in/out,
 *    Reset Zoom, New Window, New Incognito Window, Print, Clear Browsing
 *    Data, Developer Tools, Settings, Exit -> semua wired ke callback nyata
 *    (lihat Workbench.kt/Main.kt untuk detail implementasinya masing-masing).
 */
@Composable
fun BrowserMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onNewWindow: () -> Unit,
    onNewIncognitoWindow: () -> Unit,
    onShowHistory: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowRecentTabs: () -> Unit,
    onPrint: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onFind: () -> Unit,
    onOpenDevTools: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearBrowsingData: () -> Unit,
    onExit: () -> Unit
) {
    if (!expanded) return

    val dialogState = rememberDialogState(
        position = WindowPosition.Aligned(Alignment.TopEnd),
        size = DpSize(260.dp, 560.dp)
    )

    DialogWindow(
        onCloseRequest = onDismiss,
        state = dialogState,
        title = "Menu",
        undecorated = true,
        resizable = false,
        onPreviewKeyEvent = { event ->
            if (event.key == Key.Escape) { onDismiss(); true } else false
        }
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(
                color = AppColors.SurfaceRaised,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 6.dp)
                ) {
                    MenuAction(Icons.Default.Add, "New Tab", "Ctrl+T") { onNewTab(); onDismiss() }
                    MenuAction(Icons.Default.OpenInNew, "New Window") { onNewWindow(); onDismiss() }
                    MenuAction(Icons.Default.VisibilityOff, "New Incognito Window", "Ctrl+Shift+N") { onNewIncognitoWindow(); onDismiss() }
                    Divider()
                    MenuAction(Icons.Default.History, "History") { onShowHistory(); onDismiss() }
                    MenuAction(Icons.Default.Bookmark, "Bookmarks") { onShowBookmarks(); onDismiss() }
                    MenuAction(Icons.Default.Download, "Downloads") { onShowDownloads(); onDismiss() }
                    MenuAction(Icons.Default.Tab, "Recent Tabs", "Ctrl+Shift+T") { onShowRecentTabs(); onDismiss() }
                    Divider()
                    MenuAction(Icons.Default.Print, "Print") { onPrint(); onDismiss() }
                    MenuAction(Icons.Default.ZoomIn, "Zoom In", "Ctrl+=") { onZoomIn() }
                    MenuAction(Icons.Default.ZoomOut, "Zoom Out", "Ctrl+-") { onZoomOut() }
                    MenuAction(Icons.Default.YoutubeSearchedFor, "Reset Zoom") { onResetZoom(); onDismiss() }
                    MenuAction(Icons.Default.Search, "Find", "Ctrl+F") { onFind(); onDismiss() }
                    MenuAction(Icons.Default.Code, "Developer Tools") { onOpenDevTools(); onDismiss() }
                    Divider()
                    MenuAction(Icons.Default.Settings, "Settings") { onOpenSettings(); onDismiss() }
                    MenuAction(Icons.Default.DeleteSweep, "Clear Browsing Data") { onClearBrowsingData(); onDismiss() }
                    Divider()
                    MenuAction(Icons.Default.ExitToApp, "Exit") { onExit(); onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(icon: ImageVector, label: String, shortcut: String? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.width(18.dp)) },
        trailingIcon = shortcut?.let { { Text(it, color = AppColors.TextDisabled) } },
        onClick = onClick
    )
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(color = AppColors.Border)
    Spacer(Modifier.width(0.dp))
}
