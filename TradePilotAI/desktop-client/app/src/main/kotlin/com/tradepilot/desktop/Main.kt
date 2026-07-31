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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tradepilot.desktop.browser.BrowserBar
import com.tradepilot.desktop.browser.BrowserTab
import com.tradepilot.desktop.browser.BrowserTabsBar
import com.tradepilot.desktop.browser.JCEFBrowserEngine
import com.tradepilot.desktop.browser.JCEFBrowserView
import com.tradepilot.desktop.copilot.CopilotPanel
import com.tradepilot.desktop.layout.VerticalResizeHandle
import com.tradepilot.desktop.settings.DesktopSettingsStore
import com.tradepilot.desktop.settings.SettingsDialog
import com.tradepilot.domain.browser.EXNESS_WEBTRADING_URL

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

private const val MIN_PANEL_WIDTH_DP = 160f
private const val MAX_SIDEBAR_WIDTH_DP = 420f
private const val MAX_COPILOT_WIDTH_DP = 520f

@Composable
fun Workbench() {
    var isCopilotVisible by remember { mutableStateOf(true) }
    var browserEngine by remember { mutableStateOf<JCEFBrowserEngine?>(null) }
    var gatewayConfig by remember { mutableStateOf(DesktopSettingsStore.resolve()) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Fase 10: fullscreen workspace -- sembunyikan ActivityBar/SideBar/
    // CopilotPanel, sisakan cuma tab bar + address bar + browser. Toggle-nya
    // ada di BrowserBar sendiri (ikon Fullscreen), supaya tetap gampang
    // keluar lagi walau semua panel lain lagi disembunyikan.
    var isWorkspaceFullscreen by remember { mutableStateOf(false) }

    // Fase 10: lebar SideBar & CopilotPanel bisa digeser (dulu hardcode
    // 240dp/320dp, tidak bisa diubah sama sekali).
    var sideBarWidthDp by remember { mutableStateOf(240f) }
    var copilotWidthDp by remember { mutableStateOf(320f) }
    val density = LocalDensity.current

    // Fase 10: multi-tab. Lihat BrowserTab.kt untuk batasan pendekatannya
    // (1 engine dipakai bergantian antar tab, bukan tab paralel sungguhan).
    val tabs = remember {
        mutableStateListOf(BrowserTab(id = "tab-0", title = "Exness", url = EXNESS_WEBTRADING_URL))
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var tabCounter by remember { mutableStateOf(1) }

    // Sinkronkan url/title tab aktif setiap kali navigasi terjadi di engine
    // (baik dari address bar, klik link di dalam halaman, atau ganti tab).
    LaunchedEffect(browserEngine?.addressState, browserEngine?.titleState, activeTabId) {
        val engine = browserEngine ?: return@LaunchedEffect
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index >= 0) {
            tabs[index] = tabs[index].copy(
                url = engine.addressState.ifBlank { tabs[index].url },
                title = engine.titleState.ifBlank { tabs[index].title }
            )
        }
    }

    fun selectTab(id: String) {
        val tab = tabs.find { it.id == id } ?: return
        activeTabId = id
        browserEngine?.loadUrl(tab.url)
    }

    fun newTab() {
        val id = "tab-${tabCounter++}"
        val tab = BrowserTab(id = id, title = "New Tab", url = "https://www.google.com")
        tabs.add(tab)
        activeTabId = id
        browserEngine?.loadUrl(tab.url)
    }

    fun closeTab(id: String) {
        if (tabs.size <= 1) return // selalu sisakan minimal 1 tab
        val closingIndex = tabs.indexOfFirst { it.id == id }
        if (closingIndex < 0) return
        tabs.removeAt(closingIndex)
        if (activeTabId == id) {
            val fallbackIndex = closingIndex.coerceAtMost(tabs.size - 1)
            val fallbackTab = tabs[fallbackIndex]
            activeTabId = fallbackTab.id
            browserEngine?.loadUrl(fallbackTab.url)
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (!isWorkspaceFullscreen) {
            ActivityBar(
                isCopilotVisible = isCopilotVisible,
                onToggleCopilot = { isCopilotVisible = !isCopilotVisible },
                onOpenSettings = { isSettingsOpen = true }
            )
            SideBar(modifier = Modifier.width(sideBarWidthDp.dp))
            VerticalResizeHandle(onDragDeltaPx = { deltaPx ->
                val deltaDp = with(density) { deltaPx.toDp().value }
                sideBarWidthDp = (sideBarWidthDp + deltaDp).coerceIn(MIN_PANEL_WIDTH_DP, MAX_SIDEBAR_WIDTH_DP)
            })
        }

        Workspace(
            engine = browserEngine,
            onEngineReady = { browserEngine = it },
            tabs = tabs,
            activeTabId = activeTabId,
            onSelectTab = ::selectTab,
            onCloseTab = ::closeTab,
            onNewTab = ::newTab,
            isFullscreen = isWorkspaceFullscreen,
            onToggleFullscreen = { isWorkspaceFullscreen = !isWorkspaceFullscreen },
            modifier = Modifier.weight(1f)
        )

        if (isCopilotVisible && !isWorkspaceFullscreen) {
            VerticalResizeHandle(onDragDeltaPx = { deltaPx ->
                val deltaDp = with(density) { deltaPx.toDp().value }
                // Drag ke kiri (delta negatif) memperlebar CopilotPanel karena
                // dia ada di kanan -- makanya tandanya dibalik (dikurangi, bukan
                // ditambah) dibanding SideBar yang ada di kiri.
                copilotWidthDp = (copilotWidthDp - deltaDp).coerceIn(MIN_PANEL_WIDTH_DP, MAX_COPILOT_WIDTH_DP)
            })
            CopilotPanel(
                engine = browserEngine,
                gatewayConfig = gatewayConfig,
                modifier = Modifier.width(copilotWidthDp.dp)
            )
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            // Chart & Analytics SENGAJA disabled (enabled = false) -- ini
            // placeholder fitur yang belum dikerjakan, bukan bug. Sebelumnya
            // onClick={} kosong tanpa indikasi visual, user (wajar) mengira
            // ini rusak. Sekarang jelas keliatan abu-abu/non-aktif = "belum
            // ada", bukan "coba tapi gagal".
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Default.CandlestickChart, contentDescription = "Chart (belum tersedia)", tint = Color(0xFF5A5A5A))
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Default.Analytics, contentDescription = "Analytics (belum tersedia)", tint = Color(0xFF5A5A5A))
            }
            IconButton(onClick = onToggleCopilot) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "AI Copilot",
                    tint = if (isCopilotVisible) Color(0xFF4FC3F7) else Color.White
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SideBar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().background(Color(0xFF252526)).padding(12.dp)
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
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight().background(Color(0xFF181818))
    ) {
        BrowserTabsBar(
            tabs = tabs,
            activeTabId = activeTabId,
            onSelectTab = onSelectTab,
            onCloseTab = onCloseTab,
            onNewTab = onNewTab,
            modifier = Modifier.fillMaxWidth()
        )
        BrowserBar(
            engine = engine,
            isFullscreen = isFullscreen,
            onToggleFullscreen = onToggleFullscreen,
            modifier = Modifier.fillMaxWidth()
        )
        JCEFBrowserView(
            modifier = Modifier.fillMaxSize(),
            onEngineReady = onEngineReady
        )
    }
}
