package com.tradepilot.feature.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Toolbar di atas WebView: Back / Forward / Refresh / Fullscreen, + address
 * bar bebas ketik URL (BARU -- sebelumnya toolbar ini sengaja minimalis
 * tanpa address bar sesuai requirement versi 1 "cuma Exness"). Sekarang
 * browser genuinely bisa dipakai ke situs apa pun (YouTube, GitHub, Shopee,
 * Facebook, dst), padanan persis dari BrowserBar.kt di desktop-client --
 * WebViewBrowserEngine.loadUrl() sendiri sebenarnya sudah generic sejak
 * awal, yang kurang cuma UI-nya.
 */
@Composable
fun BrowserToolbar(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFullscreen: () -> Unit,
    currentUrl: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var addressField by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(currentUrl) {
        if (!isEditing) addressField = currentUrl
    }

    fun navigate(target: String) {
        if (target.isBlank()) return
        isEditing = false
        onNavigate(target)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        androidx.compose.foundation.layout.Column {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIcon(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                ToolbarIcon(onClick = onForward) { Icon(Icons.Default.ArrowForward, contentDescription = "Forward") }
                ToolbarIcon(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                ToolbarIcon(onClick = onToggleFullscreen) { Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen") }

                Spacer(Modifier.width(4.dp))

                OutlinedTextField(
                    value = addressField,
                    onValueChange = {
                        isEditing = true
                        addressField = it
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = { Text("URL — youtube.com, github.com, dst", style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigate(addressField) }),
                    colors = OutlinedTextFieldDefaults.colors()
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(QUICK_LINKS) { link ->
                    AssistChip(
                        onClick = { navigate(link.url) },
                        label = { Text(link.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarIcon(onClick: () -> Unit, icon: @Composable () -> Unit) {
    IconButton(onClick = onClick) { icon() }
}

private data class QuickLink(val label: String, val url: String)

private val QUICK_LINKS = listOf(
    QuickLink("Exness", "https://my.exness.com/webtrading"),
    QuickLink("TradingView", "https://www.tradingview.com/chart"),
    QuickLink("YouTube", "https://www.youtube.com"),
    QuickLink("GitHub", "https://github.com"),
    QuickLink("Shopee", "https://shopee.co.id"),
    QuickLink("Facebook", "https://www.facebook.com")
)
