package com.tradepilot.desktop.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens

/**
 * Prioritas 6: Browser Menu ala Chrome, dirender lewat [DialogWindow]
 * (window OS terpisah -- lihat catatan panjang versi sebelumnya soal kenapa
 * ini, bukan DropdownMenu M3 biasa: SwingPanel/JCEF heavyweight component
 * selalu digambar OS di atas layer Compose/Skia apa pun urutan komposisinya).
 *
 * PERBAIKAN (laporan bug terbaru, 2 hal):
 *
 * 1. POSISI SALAH ("menu harusnya di pojok kanan BROWSER, bukan pojok kanan
 *    APLIKASI"): versi sebelumnya menghitung posisi cuma dari
 *    `LocalAppWindowState` (posisi+ukuran WHOLE WINDOW aplikasi) -- window
 *    aplikasi ini punya AI Copilot panel di sisi kanan, jadi "kanan window"
 *    != "kanan area browser" (tombol menu-nya sendiri ada di toolbar
 *    browser, BUKAN di ujung kanan window). Fix: [BrowserBar] sekarang
 *    melaporkan posisi ASLI tombol menu (lewat `onGloballyPositioned`,
 *    posisi relatif ke WINDOW dalam px) ke Workbench.kt -> diteruskan ke
 *    sini sebagai [anchorPositionInWindowPx] -- menu sekarang dihitung
 *    presisi dari situ (window screen position + posisi tombol di dalam
 *    window), bukan lagi dari lebar window secara keseluruhan.
 *
 * 2. TIDAK AUTO-DISMISS SAAT KLIK DI LUAR: karena window OS terpisah
 *    sungguhan, Compose tidak otomatis tahu "klik di luar". Fix: pasang
 *    `java.awt.event.WindowFocusListener` ke window dialog ini lewat
 *    `WindowScope.window` -- begitu dialog KEHILANGAN fokus OS (user klik
 *    balik ke window utama, atau window lain), otomatis panggil
 *    [onDismiss]. Ini pola AWT standar untuk "click-outside-to-dismiss" pada
 *    window terpisah (Compose Desktop TIDAK expose ini secara langsung).
 *
 * Tampilan juga dibuat lebih compact (Dimens.MENU_*) sesuai laporan bug
 * "tampilan menu masih terlalu jadul" -- baris custom (bukan DropdownMenuItem
 * M3 default ~48dp), disamakan filosofinya dengan AI_PANEL_* yang sudah
 * dipakai CopilotPanel.
 */
@Composable
fun BrowserMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    anchorPositionInWindowPx: Offset?,
    anchorSizeInWindowPx: androidx.compose.ui.geometry.Size?,
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

    val parentWindowState = com.tradepilot.desktop.window.LocalAppWindowState.current
    val menuWidth = Dimens.MENU_WIDTH_DP.dp
    val menuHeight = 420.dp
    val density = LocalDensity.current

    val dialogPosition = run {
        val parentPos = parentWindowState?.position
        if (parentPos is WindowPosition.Absolute && anchorPositionInWindowPx != null) {
            // Posisi tombol menu (px, relatif ke window) -> dp, lalu tambah
            // posisi window itu sendiri di layar -> koordinat layar absolut.
            val buttonXDp = with(density) { anchorPositionInWindowPx.x.toDp() }
            val buttonYDp = with(density) { anchorPositionInWindowPx.y.toDp() }
            val buttonHeightDp = anchorSizeInWindowPx?.let { with(density) { it.height.toDp() } } ?: 30.dp
            val buttonWidthDp = anchorSizeInWindowPx?.let { with(density) { it.width.toDp() } } ?: 30.dp
            WindowPosition(
                // Rata kanan ke tepi KANAN tombol (bukan kiri) -- persis
                // seperti dropdown Chrome yang menggantung di bawah ikon,
                // rata sisi kanannya dengan ikon.
                x = parentPos.x + buttonXDp + buttonWidthDp - menuWidth,
                y = parentPos.y + buttonYDp + buttonHeightDp + 4.dp
            )
        } else if (parentPos is WindowPosition.Absolute) {
            // Fallback lama kalau posisi tombol belum sempat dilaporkan.
            val parentSize = parentWindowState.size
            WindowPosition(x = parentPos.x + parentSize.width - menuWidth - 12.dp, y = parentPos.y + 56.dp)
        } else {
            WindowPosition.Aligned(Alignment.TopEnd)
        }
    }

    val dialogState = rememberDialogState(position = dialogPosition, size = DpSize(menuWidth, menuHeight))

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
        // Click-outside-to-dismiss (lihat catatan kelas #2 di atas) --
        // WindowScope di dalam DialogWindow{} expose `window` (java.awt.Window
        // sungguhan di baliknya).
        androidx.compose.runtime.DisposableEffect(Unit) {
            val listener = object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {}
                override fun windowLostFocus(e: java.awt.event.WindowEvent?) { onDismiss() }
            }
            window.addWindowFocusListener(listener)
            onDispose { window.removeWindowFocusListener(listener) }
        }

        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(
                color = AppColors.SurfaceRaised,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
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

/** Baris menu compact custom (Dimens.MENU_ITEM_HEIGHT_DP) -- BUKAN DropdownMenuItem M3 (terlalu besar/jadul, lihat catatan kelas). */
@Composable
private fun MenuAction(icon: ImageVector, label: String, shortcut: String? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.MENU_ITEM_HEIGHT_DP.dp)
            .background(if (isHovered) AppColors.SurfaceSunken else androidx.compose.ui.graphics.Color.Transparent)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(Dimens.MENU_ICON_SIZE_DP.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (shortcut != null) {
            Spacer(Modifier.width(8.dp))
            Text(shortcut, color = AppColors.TextDisabled, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(vertical = 3.dp))
}
