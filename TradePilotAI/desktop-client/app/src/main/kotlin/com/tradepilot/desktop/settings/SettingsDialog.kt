package com.tradepilot.desktop.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

/**
 * Dialog Settings, dibuka dari ikon Settings di ActivityBar (Main.kt).
 * Satu-satunya isi untuk sekarang: konfigurasi AI Gateway (dulu cuma bisa
 * lewat environment variable -- lihat DesktopSettingsStore.kt). Tab lain
 * (bahasa, tema, dst -- lihat TODO di SettingsRepository.kt shared) belum
 * ada di sini, sengaja belum diklaim.
 *
 * FIX BUG "tombol Settings tidak bisa diklik / tidak bisa set gateway":
 * sebelumnya pakai androidx.compose.ui.window.Dialog (composable gaya
 * Android, di-render sebagai popup overlay dalam window yang sama).
 * Di Compose Desktop itu API yang TEPAT buat dialog adalah [DialogWindow]
 * -- bikin window OS terpisah sungguhan (sama seperti [Window] utama),
 * jadi dijamin punya posisi/ukuran/fokus input yang benar & pasti bisa
 * diklik, bukan bergantung pada perilaku overlay Dialog yang di Compose
 * Desktop pernah bermasalah (bisa ke-render di posisi salah/tidak
 * menerima fokus klik).
 *
 * Karena ini window terpisah (root composition baru), MaterialTheme harus
 * di-declare ULANG di sini -- tidak otomatis mewarisi dari Window utama
 * di Main.kt.
 *
 * onSaved dipanggil dengan GatewayConfig baru supaya Workbench bisa update
 * state-nya seketika tanpa perlu restart aplikasi.
 */
@Composable
fun SettingsDialog(
    initial: DesktopSettings,
    onDismiss: () -> Unit,
    onSaved: (DesktopSettings) -> Unit
) {
    var gatewayUrl by remember { mutableStateOf(initial.gatewayUrl) }
    var gatewayToken by remember { mutableStateOf(initial.gatewayToken) }
    var isTokenVisible by remember { mutableStateOf(false) }

    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(width = 460.dp, height = 480.dp),
        title = "Settings — TradePilot AI"
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "AI Gateway (Cloudflare Worker)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gatewayUrl,
                        onValueChange = { gatewayUrl = it },
                        label = { Text("Gateway URL") },
                        placeholder = { Text("https://tradepilot-ai-gateway.<subdomain>.workers.dev") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = gatewayToken,
                        onValueChange = { gatewayToken = it },
                        label = { Text("Auth Token") },
                        singleLine = true,
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isTokenVisible) "Sembunyikan token" else "Tampilkan token"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sama dengan nilai GATEWAY_AUTH_TOKEN di Worker (lihat " +
                            "backend/cloudflare-worker). Token dienkripsi (AES-256-GCM) " +
                            "sebelum disimpan di ~/.tradepilot/desktop-client.properties " +
                            "-- lihat DesktopCrypto.kt untuk batasan pendekatan ini.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(Modifier.weight(1f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val settings = DesktopSettings(gatewayUrl = gatewayUrl.trim(), gatewayToken = gatewayToken.trim())
                                DesktopSettingsStore.save(settings)
                                onSaved(settings)
                            }
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}
