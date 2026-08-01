package com.tradepilot.desktop.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.activitybar.ActivityBar
import com.tradepilot.desktop.activitybar.SidePanel
import com.tradepilot.desktop.browser.BrowserBar
import com.tradepilot.desktop.browser.BrowserMenu
import com.tradepilot.desktop.browser.BrowserTab
import com.tradepilot.desktop.browser.BrowserTabsBar
import com.tradepilot.desktop.browser.JCEFBrowserEngine
import com.tradepilot.desktop.browser.JCEFBrowserView
import com.tradepilot.desktop.components.BrowserShortcutActions
import com.tradepilot.desktop.components.FindBar
import com.tradepilot.desktop.components.handleBrowserShortcuts
import com.tradepilot.desktop.copilot.CopilotPanel
import com.tradepilot.desktop.explorer.BookmarkStore
import com.tradepilot.desktop.explorer.ExplorerPanel
import com.tradepilot.desktop.explorer.HistoryStore
import com.tradepilot.desktop.explorer.PinnedSite
import com.tradepilot.desktop.fullscreen.FullscreenRevealHost
import com.tradepilot.desktop.settings.DesktopSettingsStore
import com.tradepilot.desktop.settings.SettingsDialog
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.updater.UpdateBanner
import com.tradepilot.desktop.updater.UpdateManager
import com.tradepilot.domain.browser.EXNESS_WEBTRADING_URL

private const val MIN_PANEL_WIDTH_DP = 160f
private const val MAX_SIDEBAR_WIDTH_DP = 420f
private const val MAX_COPILOT_WIDTH_DP = 520f

/**
 * Pengganti `Workbench()` lama di `Main.kt`. Kalau kamu bandingkan dengan
 * versi lama: struktur besar (Row berisi ActivityBar/SideBar/Workspace/
 * CopilotPanel + SettingsDialog di bawahnya) TETAP SAMA -- yang berubah
 * adalah ActivityBar & SideBar sekarang datang dari modul terpisah
 * (activitybar/ dan explorer/) yang sungguhan berfungsi, bukan cuma teks
 * statis, dan Workspace sekarang membungkus segalanya lewat
 * [FullscreenRevealHost] supaya Prioritas 9 (address bar hilang total saat
 * fullscreen) benar-benar terpenuhi.
 *
 * CopilotPanel / SettingsDialog / DesktopSettingsStore dipanggil PERSIS
 * seperti versi lama -- import & pemakaiannya tidak diubah sama sekali
 * (Prioritas 14: jangan sentuh business logic / gateway).
 */
@Composable
fun Workbench(onRequestExit: () -> Unit) {
    var isCopilotVisible by remember { mutableStateOf(true) }
    var browserEngine by remember { mutableStateOf<JCEFBrowserEngine?>(null) }
    var gatewayConfig by remember { mutableStateOf(DesktopSettingsStore.resolve()) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var isWorkspaceFullscreen by remember { mutableStateOf(false) }
    var sideBarWidthDp by remember { mutableStateOf(240f) }
    var copilotWidthDp by remember { mutableStateOf(320f) }
    val density = LocalDensity.current

    var activePanel by remember { mutableStateOf(SidePanel.EXPLORER) }
    var isFindBarOpen by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val addressFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }

    val tabs = remember {
        mutableStateListOf(BrowserTab(id = "tab-0", title = "Exness", url = EXNESS_WEBTRADING_URL))
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var tabCounter by remember { mutableStateOf(1) }
    // Prioritas 10 (Ctrl+Shift+T -- reopen closed tab).
    val closedTabsStack = remember { mutableStateListOf<BrowserTab>() }

    LaunchedEffect(Unit) { rootFocusRequester.requestFocus() }

    // Auto-updater (Level 2): cek update SEKALI saat app pertama dibuka --
    // TIDAK ada polling berkala (keputusan produk). Kalau ada update,
    // download berjalan otomatis di background (lihat UpdateManager.kt);
    // hasilnya ditampilkan lewat UpdateBanner di bawah, restart tetap
    // wajib klik konfirmasi user.
    LaunchedEffect(Unit) { UpdateManager.checkAndDownload() }

    // Sinkronkan url/title tab aktif setiap kali navigasi terjadi, DAN catat
    // ke History (Prioritas 3) begitu URL berubah.
    LaunchedEffect(browserEngine?.addressState, browserEngine?.titleState, activeTabId) {
        val engine = browserEngine ?: return@LaunchedEffect
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index >= 0) {
            tabs[index] = tabs[index].copy(
                url = engine.addressState.ifBlank { tabs[index].url },
                title = engine.titleState.ifBlank { tabs[index].title }
            )
        }
        if (engine.addressState.isNotBlank()) {
            HistoryStore.record(engine.addressState, engine.titleState)
        }
    }

    fun selectTab(id: String) {
        val tab = tabs.find { it.id == id } ?: return
        activeTabId = id
        browserEngine?.loadUrl(tab.url)
    }

    fun newTab(url: String = "https://www.google.com") {
        val id = "tab-${tabCounter++}"
        val tab = BrowserTab(id = id, title = "New Tab", url = url)
        tabs.add(tab)
        activeTabId = id
        browserEngine?.loadUrl(tab.url)
    }

    fun closeTab(id: String) {
        if (tabs.size <= 1) return
        val closingIndex = tabs.indexOfFirst { it.id == id }
        if (closingIndex < 0) return
        closedTabsStack.add(tabs[closingIndex])
        tabs.removeAt(closingIndex)
        if (activeTabId == id) {
            val fallbackIndex = closingIndex.coerceAtMost(tabs.size - 1)
            val fallbackTab = tabs[fallbackIndex]
            activeTabId = fallbackTab.id
            browserEngine?.loadUrl(fallbackTab.url)
        }
    }

    fun reopenClosedTab() {
        val reopened = closedTabsStack.removeLastOrNull() ?: return
        val id = "tab-${tabCounter++}"
        val tab = reopened.copy(id = id)
        tabs.add(tab)
        activeTabId = id
        browserEngine?.loadUrl(tab.url)
    }

    fun duplicateTab(id: String) {
        val source = tabs.find { it.id == id } ?: return
        val newId = "tab-${tabCounter++}"
        val index = tabs.indexOfFirst { it.id == id }
        tabs.add(index + 1, source.copy(id = newId))
        activeTabId = newId
        browserEngine?.loadUrl(source.url)
    }

    fun togglePinTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val tab = tabs[index]
        tabs[index] = tab.copy(isPinned = !tab.isPinned)
        // Tab pin selalu tampil duluan (Prioritas 11).
        val pinned = tabs.filter { it.isPinned }
        val unpinned = tabs.filter { !it.isPinned }
        tabs.clear()
        tabs.addAll(pinned + unpinned)
    }

    fun toggleMuteTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs[index] = tabs[index].copy(isMuted = !tabs[index].isMuted)
    }

    fun reloadTab(id: String) {
        // CATATAN JUJUR: karena satu engine dipakai bergantian (lihat
        // BrowserTab.kt), reload tab yang SEDANG AKTIF benar-benar reload
        // sungguhan; reload tab lain (belum aktif) hanya mungkin lewat
        // navigasi ulang begitu tab itu dipilih -- tidak ada yang perlu
        // dilakukan sekarang untuk tab non-aktif.
        if (id == activeTabId) browserEngine?.reload()
    }

    fun reorderTabs(from: Int, to: Int) {
        if (from !in tabs.indices || to !in tabs.indices || from == to) return
        val item = tabs.removeAt(from)
        tabs.add(to, item)
    }

    val currentUrl = browserEngine?.addressState ?: ""
    val currentTitle = browserEngine?.titleState ?: ""

    val shortcutActions = remember(tabs, activeTabId, browserEngine) {
        BrowserShortcutActions(
            newTab = { newTab() },
            closeTab = { closeTab(activeTabId) },
            reopenClosedTab = { reopenClosedTab() },
            nextTab = {
                val index = tabs.indexOfFirst { it.id == activeTabId }
                if (index >= 0 && tabs.isNotEmpty()) {
                    val next = tabs[(index + 1) % tabs.size]
                    selectTab(next.id)
                }
            },
            focusAddressBar = { addressFocusRequester.requestFocus() },
            reload = { browserEngine?.reload() },
            openFind = { isFindBarOpen = true },
            toggleBookmark = { BookmarkStore.toggle(currentUrl, currentTitle.ifBlank { currentUrl }) },
            toggleFullscreen = { isWorkspaceFullscreen = !isWorkspaceFullscreen },
            goBack = { browserEngine?.goBack() },
            goForward = { browserEngine?.goForward() },
            isFullscreen = { isWorkspaceFullscreen },
            exitFullscreen = { isWorkspaceFullscreen = false }
        )
    }

    val pinnedSites = remember {
        listOf(
            PinnedSite("Exness", EXNESS_WEBTRADING_URL),
            PinnedSite("TradingView", "https://www.tradingview.com/chart")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            // Prioritas 10: semua shortcut ditangkap di sini, di ROOT --
            // supaya tetap jalan walau fokus sedang ada di child manapun
            // (onPreviewKeyEvent turun dari root ke child yang fokus dulu,
            // baru child bisa proses sendiri kalau di sini return false).
            .onPreviewKeyEvent { handleBrowserShortcuts(it, shortcutActions) }
    ) {
        if (!isWorkspaceFullscreen) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                UpdateBanner()
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (!isWorkspaceFullscreen) {
                ActivityBar(
                    activePanel = activePanel,
                    onSelectPanel = { activePanel = it },
                    isCopilotVisible = isCopilotVisible,
                    onToggleCopilot = { isCopilotVisible = !isCopilotVisible },
                    onOpenSettings = { isSettingsOpen = true }
                )
                ExplorerPanel(
                    activePanel = activePanel,
                    tabs = tabs,
                    activeTabId = activeTabId,
                    pinnedSites = pinnedSites,
                    onOpenUrl = { url -> newTab(url) },
                    onSelectTab = ::selectTab,
                    onRemoveBookmark = { BookmarkStore.remove(it) },
                    modifier = Modifier.width(sideBarWidthDp.dp)
                )
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
                onNewTab = { newTab() },
                onDuplicateTab = ::duplicateTab,
                onTogglePin = ::togglePinTab,
                onToggleMute = ::toggleMuteTab,
                onReloadTab = ::reloadTab,
                onReorder = ::reorderTabs,
                isFullscreen = isWorkspaceFullscreen,
                onToggleFullscreen = { isWorkspaceFullscreen = !isWorkspaceFullscreen },
                isFindBarOpen = isFindBarOpen,
                onCloseFindBar = { isFindBarOpen = false; browserEngine?.stopFind() },
                isMenuOpen = isMenuOpen,
                onOpenMenu = { isMenuOpen = true },
                onDismissMenu = { isMenuOpen = false },
                onShowPanel = { activePanel = it },
                onOpenSettings = { isSettingsOpen = true },
                onRequestExit = onRequestExit,
                addressFocusRequester = addressFocusRequester,
                modifier = Modifier.weight(1f)
            )

            if (isCopilotVisible && !isWorkspaceFullscreen) {
                VerticalResizeHandle(onDragDeltaPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp().value }
                    copilotWidthDp = (copilotWidthDp - deltaDp).coerceIn(MIN_PANEL_WIDTH_DP, MAX_COPILOT_WIDTH_DP)
                })
                CopilotPanel(
                    engine = browserEngine,
                    gatewayConfig = gatewayConfig,
                    modifier = Modifier.width(copilotWidthDp.dp)
                )
            }
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
private fun Workspace(
    engine: JCEFBrowserEngine?,
    onEngineReady: (JCEFBrowserEngine) -> Unit,
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onDuplicateTab: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onReloadTab: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    isFindBarOpen: Boolean,
    onCloseFindBar: () -> Unit,
    isMenuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onShowPanel: (SidePanel) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestExit: () -> Unit,
    addressFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight().background(AppColors.Base)) {
        // Prioritas 9: chrome (TabsBar + BrowserBar + menu) yang disembunyikan
        // total saat fullscreen, dan cuma muncul lagi sesaat kalau mouse
        // digerakkan ke atas (lihat FullscreenRevealHost).
        FullscreenRevealHost(
            isFullscreen = isFullscreen,
            hiddenChrome = {
                Column {
                    BrowserTabsBar(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onNewTab = onNewTab,
                        onDuplicateTab = onDuplicateTab,
                        onTogglePin = onTogglePin,
                        onToggleMute = onToggleMute,
                        onReloadTab = onReloadTab,
                        onReorder = onReorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        BrowserBar(
                            engine = engine,
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = onToggleFullscreen,
                            onOpenMenu = onOpenMenu,
                            addressFocusRequester = addressFocusRequester,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 44.dp)) {
                            BrowserMenu(
                                expanded = isMenuOpen,
                                onDismiss = onDismissMenu,
                                onNewTab = onNewTab,
                                onNewWindow = { /* TODO: lihat catatan jujur di BrowserMenu.kt */ },
                                onNewIncognitoWindow = { /* TODO */ },
                                onShowHistory = { onShowPanel(SidePanel.HISTORY) },
                                onShowBookmarks = { onShowPanel(SidePanel.BOOKMARKS) },
                                onShowDownloads = { onShowPanel(SidePanel.DOWNLOADS) },
                                onShowRecentTabs = { onShowPanel(SidePanel.EXPLORER) },
                                onPrint = { /* TODO */ },
                                onZoomIn = { engine?.zoomIn() },
                                onZoomOut = { engine?.zoomOut() },
                                onResetZoom = { engine?.resetZoom() },
                                onFind = { /* dibuka lewat isFindBarOpen di Workbench, lihat shortcutActions */ },
                                onOpenDevTools = { engine?.openDevTools() },
                                onOpenSettings = onOpenSettings,
                                onClearBrowsingData = { HistoryStore.clear() /* cookie/cache: lihat catatan BrowserMenu.kt */ },
                                onExit = onRequestExit
                            )
                        }
                    }
                }
            },
            browserContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    JCEFBrowserView(modifier = Modifier.fillMaxSize(), onEngineReady = onEngineReady)
                    if (isFindBarOpen) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                            FindBar(
                                onSearch = { text, forward -> engine?.find(text, forward) },
                                onClose = onCloseFindBar
                            )
                        }
                    }
                }
            }
        )
    }
}
