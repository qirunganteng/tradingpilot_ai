package com.tradepilot.desktop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors

/**
 * Prioritas 10: Ctrl+F -- search-in-page. Muncul sebagai overlay kecil di
 * kanan-atas area browser (ala Chrome/Firefox), memanggil
 * BrowserEngine.find()/stopFind() (lihat JCEFBrowserEngine.kt) yang
 * membungkus CefBrowser.find() asli.
 *
 * Dipanggil dari Layouts/Workbench.kt, ditampilkan/disembunyikan lewat
 * state `isFindBarOpen` yang dikontrol shortcut Ctrl+F & tombol Find di
 * BrowserMenu.
 */
@Composable
fun FindBar(
    onSearch: (text: String, forward: Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.SurfaceRaised)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(160.dp)) {
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isNotBlank()) onSearch(it, true)
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = AppColors.TextPrimary, fontSize = MaterialTheme.typography.bodySmall.fontSize),
                cursorBrush = SolidColor(AppColors.Accent),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false
                            event.key == Key.Enter && event.isShiftPressed -> { onSearch(query, false); true }
                            event.key == Key.Enter -> { onSearch(query, true); true }
                            event.key == Key.Escape -> { onClose(); true }
                            else -> false
                        }
                    }
            )
        }
        IconButton(onClick = { if (query.isNotBlank()) onSearch(query, false) }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Sebelumnya", tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { if (query.isNotBlank()) onSearch(query, true) }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Berikutnya", tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Tutup (Esc)", tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}
