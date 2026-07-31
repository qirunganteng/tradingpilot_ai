package com.tradepilot.desktop.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Toolbar navigasi ala browser sungguhan: Back / Forward / Reload + address
 * bar bebas ketik URL apa saja, + toggle Fullscreen (Fase 10).
 *
 * FIX bug yang dilaporkan user (versi sebelumnya):
 * 1. "Teks URL ketutup" -- OutlinedTextField sebelumnya dipaksa .height(40.dp),
 *    lebih kecil dari tinggi wajar Material3 OutlinedTextField (~56dp),
 *    bikin teksnya ke-clip. Sekarang tinggi dibiarkan natural.
 * 2. "Buka website selain shortcut tidak bisa" -- 2 penyebab digabung:
 *    a) Tombol "Buka" sebelumnya DIGANTI oleh spinner loading (bukan
 *       ditampilkan BERSAMA), jadi kalau isLoadingState nyangkut true,
 *       tombolnya hilang & tidak ada cara submit URL lewat mouse.
 *       Sekarang tombol Buka SELALU ada, spinner cuma indikator kecil
 *       tambahan di sampingnya.
 *    b) KeyboardActions(onDone=...) itu konsep ImeAction (mobile/IME),
 *       belum tentu ke-trigger konsisten oleh tombol Enter FISIK di
 *       desktop (AWT/Swing input, bukan IME sungguhan). Ditambah
 *       Modifier.onPreviewKeyEvent yang tangkap Key.Enter langsung
 *       sebagai jalur kedua yang lebih pasti di desktop.
 */
@Composable
fun BrowserBar(
    engine: JCEFBrowserEngine?,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
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

    Column(modifier = modifier.background(Color(0xFF2D2D2D))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { engine?.goBack() }, enabled = engine?.canGoBackState == true) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = iconTint(engine?.canGoBackState == true))
            }
            IconButton(onClick = { engine?.goForward() }, enabled = engine?.canGoForwardState == true) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Maju", tint = iconTint(engine?.canGoForwardState == true))
            }
            IconButton(onClick = { engine?.reload() }, enabled = engine != null) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang", tint = iconTint(engine != null))
            }

            Spacer(Modifier.width(4.dp))

            OutlinedTextField(
                value = addressField,
                onValueChange = {
                    isEditing = true
                    addressField = it
                },
                // TIDAK ada .height() manual di sini -- itu penyebab bug
                // "teks ketutup" sebelumnya (lihat catatan kelas).
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            (event.key == Key.Enter || event.key == Key.NumPadEnter)
                        ) {
                            navigate(addressField)
                            true
                        } else {
                            false
                        }
                    },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("Ketik URL — mis. youtube.com, github.com, shopee.co.id...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                )
            )

            Spacer(Modifier.width(4.dp))

            if (engine?.isLoadingState == true) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
            }
            // SELALU tampil (dulu: diganti spinner saat loading, jadi kadang
            // hilang & user tidak punya cara klik submit -- lihat catatan kelas).
            TextButton(onClick = { navigate(addressField) }) { Text("Buka") }

            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Keluar fullscreen" else "Fullscreen",
                    tint = Color.White
                )
            }
        }

        // Quick-links: bukti browser ini tidak dikunci ke satu situs.
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(QUICK_LINKS) { link ->
                AssistChip(
                    onClick = { navigate(link.url) },
                    label = { Text(link.label, fontWeight = FontWeight.Medium) }
                )
            }
        }
    }
}

private fun iconTint(enabled: Boolean): Color = if (enabled) Color.White else Color.Gray

private data class QuickLink(val label: String, val url: String)

private val QUICK_LINKS = listOf(
    QuickLink("Exness", "https://my.exness.com/webtrading"),
    QuickLink("TradingView", "https://www.tradingview.com/chart"),
    QuickLink("YouTube", "https://www.youtube.com"),
    QuickLink("GitHub", "https://github.com"),
    QuickLink("Shopee", "https://shopee.co.id"),
    QuickLink("Facebook", "https://www.facebook.com")
)
