package com.tradepilot.desktop.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens

/**
 * Prioritas 6: Browser Menu ala Chrome.
 *
 * PERBAIKAN BESAR (laporan bug: "klik titik tiga malah beep, harus klik new
 * tab dulu buat nutup, klik di luar tidak menutup"): versi SEBELUMNYA pakai
 * [androidx.compose.ui.window.DialogWindow] (window OS TERPISAH sungguhan)
 * supaya tidak ketutupan SwingPanel (JCEF, komponen heavyweight AWT yang
 * SELALU digambar di atas layer Compose/Skia apa pun urutannya). Tapi
 * DialogWindow terpisah PUNYA MASALAH FOKUS NATIVE SENDIRI di Windows --
 * bikin OS beep (fokus "nyangkut") & WindowFocusListener untuk auto-dismiss
 * tidak reliable, terutama berinteraksi dengan window utama yang undecorated.
 *
 * FIX AKAR MASALAH (bukan tambal lagi): balik ke [Popup] Compose BIASA
 * (satu window yang sama, dismiss-on-outside-click BAWAAN lewat
 * `onDismissRequest` + `PopupProperties(focusable = true)` -- TIDAK perlu
 * hack apa pun), TAPI sekarang browser (SwingPanel) disembunyikan sementara
 * (`Modifier.size(0.dp)`) SELAGI menu ini terbuka -- lihat pemanggilannya di
 * Workbench.kt (`browserContent`). Karena SwingPanel tidak lagi digambar
 * sama sekali (0 ukuran = tidak ada yang perlu "ditumpuk"), masalah asli
 * yang dulu memaksa pindah ke DialogWindow (SwingPanel selalu di atas) jadi
 * tidak relevan lagi -- dan kita dapat semua perilaku Popup Compose standar
 * (posisi presisi relatif ke anchor DALAM window yang sama -- tidak perlu
 * lagi hitung-hitung posisi window+tombol seperti versi DialogWindow, tinggal
 * offset px langsung; dismiss-on-outside-click; dismiss-on-Escape) secara
 * GRATIS, tanpa WindowFocusListener/beep/dsb.
 */
@Composable
fun BrowserMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    anchorPositionInWindowPx: Offset?,
    anchorSizeInWindowPx: Size?,
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

    val density = LocalDensity.current
    val menuWidth = Dimens.MENU_WIDTH_DP.dp
    val menuWidthPx = with(density) { menuWidth.roundToPx() }
    val gapPx = with(density) { 4.dp.roundToPx() }

    // Offset dalam PX, relatif ke window yang SAMA (bukan lagi perlu gabung
    // dengan posisi window di layar seperti versi DialogWindow) -- rata
    // kanan ke tepi kanan tombol, muncul persis di bawahnya.
    val offsetX = anchorPositionInWindowPx?.let {
        (it.x + (anchorSizeInWindowPx?.width ?: 0f) - menuWidthPx).toInt()
    } ?: 0
    val offsetY = anchorPositionInWindowPx?.let {
        (it.y + (anchorSizeInWindowPx?.height ?: 30f)).toInt() + gapPx
    } ?: 0

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(offsetX, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            color = AppColors.SurfaceRaised,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.width(menuWidth)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
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

/** Baris menu compact custom (Dimens.MENU_ITEM_HEIGHT_DP) -- BUKAN DropdownMenuItem M3 (terlalu besar/jadul, lihat catatan kelas). */
@Composable
private fun MenuAction(icon: ImageVector, label: String, shortcut: String? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.MENU_ITEM_HEIGHT_DP.dp)
            .background(if (isHovered) AppColors.SurfaceSunken else Color.Transparent)
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
    HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(vertical = 3.dp))
}
