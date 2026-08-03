package com.tradepilot.desktop.browser

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Baris tab ala Chrome (Fase 10, dikembangkan Prioritas 11, Multi Tab
 * sungguhan sejak FASE 2 -- lihat TabbedBrowserHost.kt: setiap tab di sini
 * sekarang punya JCEFBrowserEngine PARALEL sendiri, bukan lagi satu engine
 * dipakai bergantian seperti sebelumnya). Mute masih murni state UI (belum
 * ada API JCEF untuk benar-benar mute audio per-tab, lihat catatan di
 * `toggleMuteTab` Workbench.kt kalau ditambahkan).
 *
 * Prioritas 11 -- tambahan dibanding versi lama:
 *  - Klik kanan tab -> context menu: Close / Duplicate / Pin-Unpin / Mute-
 *    Unmute / Reload.
 *  - Tab bisa di-drag untuk diurutkan ulang (drag reorder) -- implementasi
 *    "index swap" sederhana: begitu drag melewati setengah lebar tab
 *    tetangga, posisinya ditukar. Ini BUKAN animasi reorder penuh ala
 *    LazyColumn.animateItemPlacement (itu perlu Compose versi lebih baru +
 *    key stabil across reorder yang effort-nya jauh lebih besar) -- tapi
 *    fungsinya (urutan tab bisa diubah dengan drag) sudah dapat.
 *  - Tab pin selalu ditaruh di depan (di-enforce lewat sorting di
 *    Workbench/Main.kt saat memanggil reorder, bukan di file ini).
 *
 * Prioritas 4 (Compact Toolbar): tinggi 36dp -> Dimens.TAB_BAR_HEIGHT_DP
 * (28dp), lebar tab 168dp -> Dimens.TAB_WIDTH_DP (152dp).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserTabsBar(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onDuplicateTab: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onReloadTab: (String) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Row(
        modifier = modifier.background(AppColors.Surface).height(Dimens.TAB_BAR_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(tabs, key = { it.id }) { tab ->
                val index = tabs.indexOf(tab)
                TabChip(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    canClose = tabs.size > 1,
                    onClick = { onSelectTab(tab.id) },
                    onClose = { onCloseTab(tab.id) },
                    onDuplicate = { onDuplicateTab(tab.id) },
                    onTogglePin = { onTogglePin(tab.id) },
                    onToggleMute = { onToggleMute(tab.id) },
                    onReload = { onReloadTab(tab.id) },
                    onDragSwap = { deltaDp ->
                        val steps = (deltaDp / Dimens.TAB_WIDTH_DP).let {
                            if (it >= 1f) 1 else if (it <= -1f) -1 else 0
                        }
                        if (steps != 0) {
                            val target = (index + steps).coerceIn(0, tabs.lastIndex)
                            if (target != index) onReorder(index, target)
                        }
                    }
                )
            }
        }
        IconButton(
            onClick = onNewTab,
            modifier = Modifier
                .size(Dimens.TAB_BAR_HEIGHT_DP.dp)
                // Prioritas 10: Middle Click juga buka tab baru dari tombol "+".
                // Dipakai API resmi desktop Compose (PointerMatcher.mouse +
                // PointerButton.Tertiary) -- bukan awaitPointerEvent() manual
                // yang sebelumnya gagal compile ('isMiddlePressed' tidak ada
                // di versi Compose Foundation yang dipakai project ini).
                .onClick(matcher = PointerMatcher.mouse(PointerButton.Tertiary), onClick = onNewTab)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tab baru (Ctrl+T)", tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TabChip(
    tab: BrowserTab,
    isActive: Boolean,
    canClose: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onReload: () -> Unit,
    onDragSwap: (deltaAccumulatedDp: Float) -> Unit
) {
    var dragAccumulatorDp by remember { mutableStateOf(0f) }

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Reload") { onReload() },
                ContextMenuItem("Duplicate") { onDuplicate() },
                ContextMenuItem(if (tab.isPinned) "Unpin" else "Pin") { onTogglePin() },
                ContextMenuItem(if (tab.isMuted) "Unmute" else "Mute") { onToggleMute() },
                ContextMenuItem("Close") { onClose() }
            )
        }
    ) {
        Row(
            modifier = Modifier
                .width(if (tab.isPinned) 40.dp else Dimens.TAB_WIDTH_DP.dp)
                .fillMaxHeight()
                .background(if (isActive) AppColors.Base else AppColors.SurfaceRaised)
                .clickable(onClick = onClick)
                .pointerInput(tab.id) {
                    detectDragGestures(
                        onDragEnd = { dragAccumulatorDp = 0f },
                        onDragCancel = { dragAccumulatorDp = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        dragAccumulatorDp += dragAmount.x
                        onDragSwap(dragAccumulatorDp)
                        // Reset akumulator tiap kali sudah memicu 1 swap, supaya
                        // drag panjang bisa memicu beberapa swap berturut-turut.
                        if (kotlin.math.abs(dragAccumulatorDp) >= Dimens.TAB_WIDTH_DP) {
                            dragAccumulatorDp = 0f
                        }
                    }
                }
                .padding(horizontal = Dimens.TAB_PADDING_H_DP.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tab.isMuted) {
                Icon(Icons.Default.VolumeOff, contentDescription = "Muted", tint = AppColors.TextSecondary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
            }
            if (tab.isPinned) {
                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = AppColors.TextSecondary, modifier = Modifier.size(12.dp))
            } else {
                Text(
                    text = tab.title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) AppColors.TextPrimary else AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (canClose) {
                    IconButton(onClick = onClose, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup tab", tint = AppColors.TextSecondary, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
