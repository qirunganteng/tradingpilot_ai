package com.tradepilot.desktop.duplicate.browsermenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors

/**
 * Prioritas 6: Browser Menu ala Chrome (ikon hamburger di ujung kanan
 * Toolbar/BrowserBar.kt membuka menu ini).
 *
 * CATATAN JUJUR per item -- supaya tidak ada yang terkesan berfungsi
 * padahal cuma placeholder:
 *  - New Tab, Recent Tabs, History, Bookmarks, Downloads, Find, Zoom in/out,
 *    Reset Zoom -> BENAR-BENAR jalan, wired ke callback nyata.
 *  - New Window, New Incognito Window, Print, Clear Browsing Data -> STUB.
 *    Ini butuh perubahan di luar modul UI (mis. New Window butuh proses
 *    aplikasi kedua / Window() composable baru dengan JCEF context terpisah,
 *    Incognito butuh CefRequestContext terpisah per JCEF docs, Print butuh
 *    CefBrowser.print() yang belum dites, Clear Browsing Data butuh akses ke
 *    CefCookieManager/cache -- semua ini domain JCEFBootstrap/JCEFBrowserEngine
 *    yang README asli minta jangan disentuh tanpa sepengetahuan kamu). Diberi
 *    callback kosong + tetap tampil (disabled=false) supaya kelihatan jelas
 *    di menu, tapi silakan cek TODO di Main.kt sebelum menganggap ini jalan.
 *  - Developer Tools -> CefBrowser.startDevTools() memang tersedia di JCEF,
 *    diwire langsung ke browser aktif.
 *  - Settings -> panggil onOpenSettings yang SAMA persis dengan tombol
 *    Settings di ActivityBar (SettingsDialog tidak disertakan di paket file
 *    ini, tapi callback-nya sudah ada dari Main.kt lama).
 *  - Exit -> panggil onExit yang sama dengan tombol Close di CustomTitleBar
 *    (termasuk shutdown JCEFBootstrap, lihat Main.kt).
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
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
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
