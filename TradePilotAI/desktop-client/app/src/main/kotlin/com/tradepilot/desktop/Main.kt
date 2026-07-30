package com.tradepilot.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tradepilot.desktop.browser.BrowserBar
import com.tradepilot.desktop.browser.JCEFBrowserEngine
import com.tradepilot.desktop.browser.JCEFBrowserView
import com.tradepilot.desktop.copilot.CopilotPanel
import com.tradepilot.desktop.settings.DesktopSettingsStore
import com.tradepilot.desktop.settings.SettingsDialog
import com.tradepilot.domain.config.GatewayConfig

// KONSTITUSI: file ini (Platform Client) HANYA boleh berisi rendering UI,
// window management, dan navigation. Business Logic (CalculateRiskUseCase
// dkk) datang dari :shared lewat CopilotPanel/BrowserBar — bukti bahwa
// module shared benar-benar dipakai bersama oleh android-client & desktop-client.

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
    // Panel kanan (AI Copilot) bisa ditoggle dari ActivityBar, sama seperti
    // Copilot Chat panel di VS Code.
    var isCopilotVisible by remember { mutableStateOf(true) }
    // Engine di-hoist ke sini (bukan cuma di dalam JCEFBrowserView) supaya
    // BrowserBar di atasnya bisa panggil goBack/goForward/loadUrl.
    var browserEngine by remember { mutableStateOf<JCEFBrowserEngine?>(null) }
    // Fase 8: gateway config sekarang dari Settings panel (dulu cuma env
    // var) -- di-hoist di sini supaya begitu disimpan di dialog, CopilotPanel
    // langsung ikut update tanpa restart aplikasi.
    var gatewayConfig by remember { mutableStateOf(DesktopSettingsStore.resolve()) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        ActivityBar(
            isCopilotVisible = isCopilotVisible,
            onToggleCopilot = { isCopilotVisible = !isCopilotVisible },
            onOpenSettings = { isSettingsOpen = true }
        )
        SideBar()
        Workspace(
            engine = browserEngine,
            onEngineReady = { browserEngine = it },
            modifier = Modifier.weight(1f)
        )
        if (isCopilotVisible) {
            VerticalDivider(color = Color(0xFF3A3A3A))
            CopilotPanel(engine = browserEngine, gatewayConfig = gatewayConfig)
        }
    }

    if (isSettingsOpen) {
        SettingsDialog(
            initial = DesktopSettingsStore.load(),
            onDismiss = { isSettingsOpen = false },
            onSaved = { saved ->
                gatewayConfig = saved.toGatewayConfig()
                isSettingsOpen = false
            }
        )
    }
}

@Composable
private fun ActivityBar(
    isCopilotVisible: Boolean,
    onToggleCopilot: () -> Unit,
    onOpenSettings: () -> Unit
) {
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
            IconButton(onClick = onToggleCopilot) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "AI Copilot",
                    tint = if (isCopilotVisible) Color(0xFF4FC3F7) else Color.White
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
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
private fun Workspace(
    engine: JCEFBrowserEngine?,
    onEngineReady: (JCEFBrowserEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight().background(Color(0xFF181818))
    ) {
        // Browser sungguhan: address bar bebas ketik URL apa saja (bukan cuma
        // Exness/TradingView -- lihat catatan di JCEFBrowserEngine & BrowserBar).
        BrowserBar(engine = engine, modifier = Modifier.fillMaxWidth())
        JCEFBrowserView(
            modifier = Modifier.fillMaxSize(),
            onEngineReady = onEngineReady
        )
    }
}
