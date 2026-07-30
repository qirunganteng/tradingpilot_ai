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
import androidx.compose.ui.window.Dialog

/**
 * Dialog Settings, dibuka dari ikon Settings di ActivityBar (Main.kt).
 * Satu-satunya isi untuk sekarang: konfigurasi AI Gateway (dulu cuma bisa
 * lewat environment variable -- lihat DesktopSettingsStore.kt). Tab lain
 * (bahasa, tema, dst -- lihat TODO di SettingsRepository.kt shared) belum
 * ada di sini, sengaja belum diklaim.
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 8.dp) {
            Column(modifier = Modifier.width(420.dp).padding(24.dp)) {
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
                        "backend/cloudflare-worker). Disimpan plain text di " +
                        "~/.tradepilot/desktop-client.properties untuk fase ini " +
                        "-- enkripsi menyusul (lihat catatan di DesktopSettingsStore.kt).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(20.dp))
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
