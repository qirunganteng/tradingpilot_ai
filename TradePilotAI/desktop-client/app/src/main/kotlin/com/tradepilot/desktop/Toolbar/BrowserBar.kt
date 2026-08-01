package com.tradepilot.desktop.duplicate.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.explorer.BookmarkStore
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens

/**
 * Toolbar navigasi ala browser sungguhan: Back / Forward / Reload + address
 * bar bebas ketik URL apa saja, tombol bookmark, tombol menu (Prioritas 6),
 * + toggle Fullscreen.
 *
 * Prioritas 4 (Compact Toolbar): dibanding versi lama --
 *  - padding vertical Row 6dp -> Dimens.TOOLBAR_PADDING_V_DP (4dp)
 *  - address field: OutlinedTextField M3 natural (~56dp) -> AddressBar
 *    custom 32dp (Dimens.ADDRESS_FIELD_HEIGHT_DP)
 *  - quick-link row: AssistChip default M3 (~32dp) -> chip custom
 *    Dimens.QUICK_LINK_CHIP_HEIGHT_DP (26dp), padding vertical dikurangi
 *  Estimasi total tinggi toolbar (2 baris): lama ~56+32=88dp+padding,
 *  baru ~32+26=58dp+padding -- kira-kira -25-30% sesuai target prompt.
 *
 * FIX bug lama yang tetap dipertahankan (dari versi sebelumnya, TIDAK
 * dihapus saat compact-kan):
 * 1. Tombol "Buka" SELALU tampil (bukan diganti spinner saat loading).
 * 2. Enter fisik ditangkap lewat onPreviewKeyEvent (bukan cuma
 *    KeyboardActions/ImeAction yang tidak konsisten di desktop AWT/Swing).
 */
@Composable
fun BrowserBar(
    engine: JCEFBrowserEngine?,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onOpenMenu: () -> Unit,
    addressFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var addressField by remember(engine) { mutableStateOf(engine?.addressState ?: "") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(engine?.addressState) {
        if (!isEditing) addressField = engine?.addressState ?: addressField
    }

    fun navigate(target: String) {
        if (target.isBlank()) return
        isEditing = false
        engine?.loadUrl(target)
    }

    val currentUrl = engine?.addressState ?: ""
    val isBookmarked = BookmarkStore.isBookmarked(currentUrl)

    Column(modifier = modifier.background(AppColors.SurfaceRaised)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.TOOLBAR_PADDING_H_DP.dp, vertical = Dimens.TOOLBAR_PADDING_V_DP.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { engine?.goBack() }, enabled = engine?.canGoBackState == true, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali (Alt+Left)", tint = iconTint(engine?.canGoBackState == true), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { engine?.goForward() }, enabled = engine?.canGoForwardState == true, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Maju (Alt+Right)", tint = iconTint(engine?.canGoForwardState == true), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { engine?.reload() }, enabled = engine != null, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang (Ctrl+R / F5)", tint = iconTint(engine != null), modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(4.dp))

            AddressBar(
                value = addressField,
                onValueChange = {
                    isEditing = true
                    addressField = it
                },
                onSubmit = ::navigate,
                isBookmarked = isBookmarked,
                onToggleBookmark = { BookmarkStore.toggle(currentUrl, engine?.titleState ?: currentUrl) },
                focusRequester = addressFocusRequester,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(4.dp))

            if (engine?.isLoadingState == true) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
            }
            // SELALU tampil (dulu: diganti spinner saat loading, jadi kadang
            // hilang & user tidak punya cara klik submit).
            IconButton(onClick = { navigate(addressField) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Send, contentDescription = "Buka", tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
            }

            IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(30.dp)) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Keluar fullscreen (F11/Esc)" else "Fullscreen (F11)",
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Prioritas 6: tombol menu Chrome-style, PopupMenu isinya di
            // BrowserMenu.kt.
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = AppColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        // Quick-links: bukti browser ini tidak dikunci ke satu situs.
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = Dimens.QUICK_LINK_ROW_PADDING_V_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(QUICK_LINKS) { link ->
                AssistChip(
                    onClick = { navigate(link.url) },
                    label = { Text2(link.label) },
                    modifier = Modifier.height(Dimens.QUICK_LINK_CHIP_HEIGHT_DP.dp),
                    colors = AssistChipDefaults.assistChipColors(containerColor = AppColors.SurfaceSunken)
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Text2(text: String) {
    androidx.compose.material3.Text(text, fontWeight = FontWeight.Medium, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = AppColors.TextPrimary)
}

private fun iconTint(enabled: Boolean): Color = if (enabled) AppColors.TextPrimary else AppColors.TextDisabled

private data class QuickLink(val label: String, val url: String)

private val QUICK_LINKS = listOf(
    QuickLink("Exness", "https://my.exness.com/webtrading"),
    QuickLink("TradingView", "https://www.tradingview.com/chart"),
    QuickLink("YouTube", "https://www.youtube.com"),
    QuickLink("GitHub", "https://github.com"),
    QuickLink("Shopee", "https://shopee.co.id"),
    QuickLink("Facebook", "https://www.facebook.com")
)
