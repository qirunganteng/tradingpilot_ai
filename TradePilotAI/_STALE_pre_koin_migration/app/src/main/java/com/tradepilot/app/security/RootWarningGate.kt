package com.tradepilot.app.security

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tradepilot.core.security.RootDetector

/**
 * Blueprint 13 (Security Design): tampilkan warning jika device
 * terindikasi root — TIDAK memblokir total (biar tetap bisa dipakai
 * developer/tester), hanya beri peringatan risiko.
 */
@Composable
fun RootWarningGate(content: @Composable () -> Unit) {
    var showWarning by remember { mutableStateOf(RootDetector.isLikelyRooted()) }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text("Peringatan Keamanan") },
            text = {
                Text(
                    "Perangkat ini terindikasi root. Menyimpan API key dan data trading " +
                        "di perangkat root berisiko lebih tinggi terhadap kebocoran data. " +
                        "Lanjutkan dengan risiko Anda sendiri."
                )
            },
            confirmButton = {
                TextButton(onClick = { showWarning = false }) { Text("Mengerti") }
            }
        )
    }

    content()
}
