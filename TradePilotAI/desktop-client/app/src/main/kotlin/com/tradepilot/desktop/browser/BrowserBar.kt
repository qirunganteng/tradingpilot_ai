package com.tradepilot.desktop.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Toolbar navigasi ala browser sungguhan: Back / Forward / Reload + address
 * bar bebas ketik URL apa saja. INI bagian yang tadinya hilang -- engine JCEF
 * sudah generic sejak awal, tapi tanpa address bar user cuma bisa lihat
 * Exness (satu-satunya URL yang di-hardcode di startup) tanpa cara pindah
 * ke situs lain. Quick-links di bawah cuma shortcut, BUKAN whitelist --
 * kolom alamat menerima domain apa pun (youtube.com, github.com,
 * shopee.co.id, facebook.com, dst) persis seperti Chrome/Edge biasa.
 */
@Composable
fun BrowserBar(
    engine: JCEFBrowserEngine?,
    modifier: Modifier = Modifier
) {
    // Teks di kolom alamat: default ikut addressState engine (hasil navigasi
    // nyata), tapi begitu user mulai ngetik, field ini "lepas" dari engine
    // sampai dia menekan Enter/Go -- supaya tidak "direbut kursor"-nya
    // di tengah mengetik saat halaman lagi loading.
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
                modifier = Modifier.weight(1f).height(40.dp),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("Ketik URL — mis. youtube.com, github.com, shopee.co.id...") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { navigate(addressField) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                )
            )

            Spacer(Modifier.width(4.dp))

            if (engine?.isLoadingState == true) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = { navigate(addressField) }) { Text("Buka") }
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
