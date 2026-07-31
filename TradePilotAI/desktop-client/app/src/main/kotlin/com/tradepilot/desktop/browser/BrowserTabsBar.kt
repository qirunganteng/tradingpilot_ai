package com.tradepilot.desktop.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Baris tab ala Chrome (Fase 10) -- lihat batasan di BrowserTab.kt (satu
 * engine dipakai bergantian, bukan tab paralel sungguhan).
 */
@Composable
fun BrowserTabsBar(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.background(Color(0xFF252526)).height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(tabs, key = { it.id }) { tab ->
                TabChip(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    canClose = tabs.size > 1,
                    onClick = { onSelectTab(tab.id) },
                    onClose = { onCloseTab(tab.id) }
                )
            }
        }
        IconButton(onClick = onNewTab, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Tab baru", tint = Color.White)
        }
    }
}

@Composable
private fun TabChip(
    tab: BrowserTab,
    isActive: Boolean,
    canClose: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(168.dp)
            .fillMaxHeight()
            .background(if (isActive) Color(0xFF1E1E1E) else Color(0xFF2D2D2D))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tab.title.ifBlank { "New Tab" },
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Color.White else Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (canClose) {
            IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup tab", tint = Color.Gray, modifier = Modifier.size(14.dp))
            }
        }
    }
}
