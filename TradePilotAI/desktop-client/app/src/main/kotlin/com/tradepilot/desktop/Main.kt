package com.tradepilot.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tradepilot.desktop.browser.JCEFBrowserView
import com.tradepilot.domain.usecase.CalculateRiskUseCase

// KONSTITUSI: file ini (Platform Client) HANYA boleh berisi rendering UI,
// window management, dan navigation. Business Logic (CalculateRiskUseCase
// di bawah) datang dari :shared — bukti bahwa module shared benar-benar
// dipakai bersama oleh android-client & desktop-client.

fun main() = application {
    Window(
        onCloseRequest = {
            // WAJIB: tanpa ini proses native CEF child bisa tertinggal jalan
            // di background setelah window ditutup (lihat catatan JCEFBootstrap.kt).
            com.tradepilot.desktop.browser.JCEFBootstrap.shutdown()
            exitApplication()
        },
        title = "TradePilot AI"
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Workbench()
        }
    }
}

@Composable
fun Workbench() {
    // Bukti :shared jalan: hitung contoh risk memakai use case dari domain.
    val sampleRisk = remember {
        CalculateRiskUseCase().invoke(
            balance = 1000.0,
            riskPercent = 1.0,
            entryPrice = 1.1000,
            stopLossPrice = 1.0950,
            takeProfitPrice = 1.1100
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        ActivityBar()
        SideBar()
        Workspace(sampleRiskLot = sampleRisk.lot, sampleRR = sampleRisk.riskRewardRatio)
    }
}

@Composable
private fun ActivityBar() {
    Column(
        modifier = Modifier.width(56.dp).fillMaxHeight().background(Color(0xFF1E1E1E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Sengaja HINDARI Modifier.weight() di sini -- pola ini pernah memicu
        // bug compiler "RowColumnParentData ... internal in file" di Compose
        // Android kita (lihat MainActivity.kt android-client). Belum tentu
        // bug yang sama muncul di Compose Desktop, tapi Arrangement.SpaceBetween
        // mencapai efek visual yang sama (icon atas + icon bawah terpisah)
        // tanpa risiko itu sama sekali.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            IconButton(onClick = {}) { Icon(Icons.Default.CandlestickChart, contentDescription = "Chart") }
            IconButton(onClick = {}) { Icon(Icons.Default.Analytics, contentDescription = "Analytics") }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SideBar() {
    Column(
        modifier = Modifier.width(240.dp).fillMaxHeight().background(Color(0xFF252526)).padding(12.dp)
    ) {
        Text("EXPLORER", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Text("XAUUSD H4/M15/M1", style = MaterialTheme.typography.bodyMedium)
        Text("Journal", style = MaterialTheme.typography.bodyMedium)
        Text("AI Copilot", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Workspace(sampleRiskLot: Double, sampleRR: Double) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF181818))
    ) {
        Row(modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp)) {
            Column {
                Text("TradePilot AI — Desktop Client", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Contoh hasil :shared → CalculateRiskUseCase — Lot: $sampleRiskLot   Risk/Reward: $sampleRR",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        // Fase 5: Browser Engine (JCEF) — terminal web Exness, sama seperti
        // ExnessWebView di android-client tapi lewat CefBrowser + SwingPanel.
        JCEFBrowserView(modifier = Modifier.fillMaxSize())
    }
}
