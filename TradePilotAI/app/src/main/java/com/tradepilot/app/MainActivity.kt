package com.tradepilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tradepilot.app.navigation.TradePilotNavHost
import com.tradepilot.app.security.RootWarningGate
import com.tradepilot.core.ui.theme.TradePilotTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity host. Semua navigasi (Browser, AI Analysis, Journal,
 * Statistic, Notification, Money Management, Settings) terjadi lewat
 * TradePilotNavHost (Compose Navigation), bukan Activity terpisah.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradePilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootWarningGate {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TradePilotNavHost(
                                navController = androidx.navigation.compose.rememberNavController(),
                                modifier = Modifier.weight(1f)
                            )
                            StatusBarPlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBarPlaceholder() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = "TradePilot AI — bukan broker. AI hanya memberi rekomendasi, tidak melakukan transaksi.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
