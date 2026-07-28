package com.tradepilot.feature.browser

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Toolbar sederhana di atas WebView: Back / Forward / Refresh / Fullscreen.
 * Sengaja minimalis (bukan toolbar browser umum) sesuai requirement versi 1:
 * "Tampilan bersih. Toolbar sederhana."
 */
@Composable
fun BrowserToolbar(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            ToolbarIcon(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            ToolbarIcon(onClick = onForward) { Icon(Icons.Default.ArrowForward, contentDescription = "Forward") }
            ToolbarIcon(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
            ToolbarIcon(onClick = onToggleFullscreen) { Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen") }
        }
    }
}

@Composable
private fun ToolbarIcon(onClick: () -> Unit, icon: @Composable () -> Unit) {
    IconButton(onClick = onClick) { icon() }
}
