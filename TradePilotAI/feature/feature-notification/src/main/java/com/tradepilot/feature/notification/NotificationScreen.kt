package com.tradepilot.feature.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notification Center (versi 4): log Order Block/BOS/Liquidity/CHOCH,
 * dengan tombol start/stop eksplisit agar user sadar kapan Copilot
 * aktif memanggil API (transparansi biaya & baterai).
 */
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    viewModel: CopilotMonitorViewModel = hiltViewModel()
) {
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val formatter = remember_SimpleDateFormat()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "AI Copilot Notification", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Aktif hanya selama layar ini terbuka — tidak berjalan di background.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            if (isMonitoring) {
                Button(onClick = { viewModel.stopMonitoring() }) { Text("Hentikan Monitor") }
            } else {
                Button(onClick = { viewModel.startMonitoring() }) { Text("Mulai Monitor") }
            }
        }

        Divider()

        if (logs.isEmpty()) {
            Text("Belum ada notifikasi.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(logs) { log ->
                    Column {
                        Text(text = "[${log.category}] ${log.message}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatter.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Divider(modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun remember_SimpleDateFormat(): SimpleDateFormat =
    androidx.compose.runtime.remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
