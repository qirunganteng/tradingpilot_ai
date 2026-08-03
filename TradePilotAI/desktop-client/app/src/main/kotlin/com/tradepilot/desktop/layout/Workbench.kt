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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.tradepilot.desktop.activitybar.ActivityBar
import com.tradepilot.desktop.activitybar.SidePanel
import com.tradepilot.desktop.browser.BrowserBar
import com.tradepilot.desktop.browser.BrowserMenu
import com.tradepilot.desktop.browser.BrowserTab
import com.tradepilot.desktop.browser.BrowserTabsBar
import com.tradepilot.desktop.browser.JCEFBrowserEngine
import com.tradepilot.desktop.browser.TabbedBrowserHost
import com.tradepilot.desktop.components.BrowserShortcutActions
import com.tradepilot.desktop.components.FindBar
import com.tradepilot.desktop.components.handleBrowserShortcuts
import com.tradepilot.desktop.components.handleBrowserShortcutsNative
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
import com.tradepilot.desktop.window.LocalAppFullscreenState
import com.tradepilot.desktop.window.LocalAppWindowState
import com.tradepilot.domain.browser.EXNESS_WEBTRADING_URL

private const val MIN_PANEL_WIDTH_DP = 160f
private const val MAX_SIDEBAR_WIDTH_DP = 420f
private const val MAX_COPILOT_WIDTH_DP = 520f

/**
 * Perakit utama UI (ActivityBar/SideBar/Workspace/CopilotPanel + dialog).
 *
 * PEMBARUAN (perbaikan bug laporan terbaru):
 *  - Bug #8 (fullscreen mentok di bawah tombol X): "isWorkspaceFullscreen"
 *    dulu state LOKAL murni di sini -- sekarang dibaca/ditulis lewat
 *    [LocalAppFullscreenState] (CompositionLocal, lihat window/
 *    AppFullscreenState.kt) supaya AppWindow.kt & CustomTitleBar.kt di luar
 *    Workbench ini juga tahu & ikut menyembunyikan diri + mendorong window
 *    ke WindowPlacement.Fullscreen sungguhan.
 *  - Bug #9 (kolom Explorer belum bisa disembunyikan): klik ulang icon
 *    ActivityBar yang SEDANG aktif sekarang toggle `isSidebarVisible`,
 *    bukan cuma ganti activePanel (yang isinya sama, jadi user tidak lihat
 *    perubahan apa pun).
 *  - New Window / New Incognito Window (dulu stub kosong di BrowserMenu):
 *    diteruskan lewat parameter `onOpenNewWindow`/`onOpenIncognitoWindow`
 *    dari Main.kt (lihat catatan lengkap di sana soal daftar window).
 *  - Print / Clear Browsing Data (dulu stub kosong): sekarang benar-benar
 *    memanggil `browserEngine?.print()` / `browserEngine?.clearBrowsingData()`
 *    (lihat JCEFBrowserEngine.kt).
 */
@Composable
fun Workbench(
    onRequestExit: () -> Unit,
    isIncognito: Boolean = false,
    onOpenNewWindow: () -> Unit = {},
    onOpenIncognitoWindow: () -> Unit = {},
    // Browser Session/Restore (FASE 2, lihat SessionStore.kt): kalau window
    // ini dipulihkan dari sesi sebelumnya, tab awalnya dari sini -- kalau
    // null (window baru/incognito), fallback ke default (1 tab Exness).
    initialTabs: List<com.tradepilot.desktop.session.SessionTab>? = null,
    // Dipanggil (debounced di dalam) tiap kali daftar tab window ini
    // berubah, supaya Main.kt bisa menulis ulang session.properties gabungan
    // SEMUA window. TIDAK dipanggil sama sekali untuk window incognito
    // (lihat pemanggilan di bawah) -- incognito tidak pernah masuk session.
    onTabsChanged: (List<com.tradepilot.desktop.session.SessionTab>) -> Unit = {}
) {
    var isCopilotVisible by remember { mutableStateOf(true) }
    // FASE 2 (Multi Tab sungguhan): dulu SATU browserEngine dibagi ke semua
    // tab (lihat TabbedBrowserHost.kt untuk penjelasan lengkap kenapa itu
    // bukan tab sungguhan). Sekarang tiap tab punya engine sendiri di sini,
    // `browserEngine` di bawah cuma DERIVED (yang lagi aktif) -- dipakai
    // seadanya oleh BrowserBar/CopilotPanel/menu yang memang cuma peduli
    // tab yang sedang dilihat user.
    val tabEngines = remember { mutableStateMapOf<String, JCEFBrowserEngine>() }
    var gatewayConfig by remember { mutableStateOf(DesktopSettingsStore.resolve()) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Bug #8 fix: fullscreen sekarang state BERSAMA lewat CompositionLocal,
    // bukan `remember { mutableStateOf(false) }` lokal -- lihat dokumentasi
    // lengkap di window/AppFullscreenState.kt.
    val fullscreenState = LocalAppFullscreenState.current
    val isWorkspaceFullscreen = fullscreenState.isFullscreen
    fun setWorkspaceFullscreen(value: Boolean) { fullscreenState.isFullscreen = value }

    // Bug #3 fix (resize edge bawah window tidak jalan): butuh windowState
    // buat HorizontalResizeHandle ubah tinggi window secara manual.
    val windowState = LocalAppWindowState.current

    var sideBarWidthDp by remember { mutableStateOf(240f) }
    var copilotWidthDp by remember { mutableStateOf(320f) }
    val density = LocalDensity.current

    var activePanel by remember { mutableStateOf(SidePanel.EXPLORER) }
    // Bug #9 fix: sidebar (ExplorerPanel) sekarang bisa disembunyikan.
    var isSidebarVisible by remember { mutableStateOf(true) }
    var isFindBarOpen by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    // Perbaikan bug posisi menu (lihat BrowserMenu.kt) -- dilaporkan oleh
    // BrowserBar via onMenuButtonPositioned.
    var menuAnchorPositionPx by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var menuAnchorSizePx by remember { mutableStateOf<androidx.compose.ui.geometry.Size?>(null) }
    val addressFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }

    val tabs = remember {
        val restored = initialTabs?.takeIf { it.isNotEmpty() }?.mapIndexed { index, saved ->
            BrowserTab(id = "tab-$index", title = saved.title, url = saved.url, isPinned = saved.isPinned)
        }
        // Perbaikan bug (laporan user): default tab pertama kali app dibuka
        // (dan tiap window baru tanpa sesi tersimpan) HARUS Google, bukan
        // Exness -- Exness tetap ada sebagai quick-link/pinned site (lihat
        // QUICK_LINKS di BrowserBar.kt & pinnedSites di bawah), cuma bukan
        // lagi paksa jadi halaman pertama yang dibuka.
        mutableStateListOf(*(restored ?: listOf(BrowserTab(id = "tab-0", title = "Google", url = "https://www.google.com"))).toTypedArray())
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var tabCounter by remember { mutableStateOf(tabs.size) }
    // Prioritas 10 (Ctrl+Shift+T -- reopen closed tab).
    val closedTabsStack = remember { mutableStateListOf<BrowserTab>() }

    LaunchedEffect(Unit) { rootFocusRequester.requestFocus() }

    // Auto-updater (Level 2): cek update SEKALI saat app pertama dibuka --
    // TIDAK ada polling berkala (keputusan produk). Kalau ada update,
    // download berjalan otomatis di background (lihat UpdateManager.kt);
    // hasilnya ditampilkan lewat UpdateBanner di bawah, restart tetap
    // wajib klik konfirmasi user. Sengaja TIDAK dijalankan di window
    // incognito/kedua (cek update sekali per PROSES aplikasi sudah cukup).
    LaunchedEffect(Unit) { if (!isIncognito) UpdateManager.checkAndDownload() }

    // Browser Session/Restore (FASE 2): laporkan snapshot tab ke Main.kt
    // setiap ada perubahan berarti (buka/tutup/pindah url/pin), DEBOUNCED
    // 500ms lewat delay() di bawah -- key LaunchedEffect berupa String hasil
    // snapshot tabs saat ini, jadi tiap kali isinya berubah, coroutine LAMA
    // otomatis dibatalkan (perilaku bawaan LaunchedEffect) & yang baru mulai
    // nunggu 500ms lagi -- ini yang jadi mekanisme debounce-nya, supaya
    // tidak menulis file tiap 1 huruf saat title masih berubah-ubah pas
    // halaman loading. SENGAJA skip total untuk incognito (privasi).
    if (!isIncognito) {
        val tabsSnapshotKey = tabs.joinToString("~~") { "${it.url}||${it.title}||${it.isPinned}" }
        LaunchedEffect(tabsSnapshotKey) {
            kotlinx.coroutines.delay(500)
            onTabsChanged(tabs.map { com.tradepilot.desktop.session.SessionTab(it.url, it.title, it.isPinned) })
        }
    }

    // Derived -- engine milik tab yang SEDANG AKTIF (lihat catatan di atas
    // deklarasi tabEngines). null selagi tab itu baru dibuat & JCEF belum
    // selesai bootstrap (lihat JCEFBrowserView -- ada spinner selagi ini null).
    val browserEngine: JCEFBrowserEngine? = tabEngines[activeTabId]

    // FASE 2: dulu ada LaunchedEffect(browserEngine?.addressState, ...) di
    // sini yang sinkron url/title HANYA untuk tab aktif (karena memang cuma
    // ada satu engine). Sekarang sinkronisasi per-tab (termasuk tab
    // background) dilakukan di dalam TabbedBrowserHost lewat callback
    // `onTabNavigated` di bawah -- lihat pemanggilannya di Workspace.

    fun selectTab(id: String) {
        // FASE 2: dulu manggil browserEngine?.loadUrl(tab.url) di sini --
        // engine tab tujuan SUDAH punya state-nya sendiri (lihat
        // TabbedBrowserHost.kt), jadi cukup pindah activeTabId, TIDAK ada
        // navigasi/reload apa pun yang perlu dipicu manual lagi.
        if (tabs.any { it.id == id }) activeTabId = id
    }

    fun newTab(url: String = "https://www.google.com") {
        val id = "tab-${tabCounter++}"
        val tab = BrowserTab(id = id, title = "New Tab", url = url)
        tabs.add(tab)
        activeTabId = id
        // FASE 2: TIDAK perlu browserEngine?.loadUrl(url) lagi -- tab baru
        // otomatis mendapat JCEFBrowserEngine sendiri yang start langsung ke
        // `url` ini (lihat startUrl di TabbedBrowserHost/JCEFBrowserView).
    }

    fun closeTab(id: String) {
        if (tabs.size <= 1) return
        val closingIndex = tabs.indexOfFirst { it.id == id }
        if (closingIndex < 0) return
        closedTabsStack.add(tabs[closingIndex])
        tabs.removeAt(closingIndex)
        // Engine tab ini otomatis di-dispose oleh Compose (DisposableEffect
        // bawaan JCEFBrowserView) begitu key(tab.id)-nya hilang dari
        // composition (TabbedBrowserHost cuma me-loop `tabs`) -- baris ini
        // cuma bersih-bersih referensi di map lokal, bukan yang men-trigger
        // dispose sungguhan.
        tabEngines.remove(id)
        if (activeTabId == id) {
            val fallbackIndex = closingIndex.coerceAtMost(tabs.size - 1)
            activeTabId = tabs[fallbackIndex].id
        }
    }

    fun reopenClosedTab() {
        val reopened = closedTabsStack.removeLastOrNull() ?: return
        val id = "tab-${tabCounter++}"
        val tab = reopened.copy(id = id)
        tabs.add(tab)
        activeTabId = id
    }

    fun duplicateTab(id: String) {
        val source = tabs.find { it.id == id } ?: return
        val newId = "tab-${tabCounter++}"
        val index = tabs.indexOfFirst { it.id == id }
        tabs.add(index + 1, source.copy(id = newId))
        activeTabId = newId
        // FASE 2 CATATAN JUJUR: ini "duplicate" di level URL saja (tab baru
        // navigasi fresh ke URL yang sama) -- BUKAN clone session/history/
        // form-state browser sungguhan seperti Ctrl+drag tab di Chrome asli.
        // JCEF versi ini tidak expose API clone-browser untuk itu. Sama
        // seperti perilaku sebelum FASE 2 ini, tidak ada regresi.
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
        // FASE 2: dulu HANYA tab aktif yang bisa direload sungguhan (lihat
        // catatan jujur versi lama yang DIHAPUS di sini -- karena memang
        // cuma ada satu engine dipakai bergantian). Sekarang SEMUA tab punya
        // engine sendiri, jadi reload tab non-aktif pun sekarang beneran
        // jalan -- ini perbaikan nyata, bukan cuma refactor.
        tabEngines[id]?.reload()
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
            toggleFullscreen = { setWorkspaceFullscreen(!isWorkspaceFullscreen) },
            goBack = { browserEngine?.goBack() },
            goForward = { browserEngine?.goForward() },
            isFullscreen = { isWorkspaceFullscreen },
            // Permintaan eksplisit: ESC keluar dari fullscreen BROWSER (HTML5
            // Fullscreen API -- video/chart) DAN fullscreen APLIKASI sekaligus,
            // bukan cuma salah satu. exitPageFullscreen() aman dipanggil kapan
            // pun (no-op kalau halaman memang tidak sedang HTML5 fullscreen).
            exitFullscreen = {
                setWorkspaceFullscreen(false)
                browserEngine?.exitPageFullscreen()
            }
        )
    }

    // Focus Management fix (lihat catatan panjang di JCEFBrowserEngine.kt &
    // components/KeyboardShortcuts.kt/handleBrowserShortcutsNative): tanpa
    // ini, shortcut di atas HANYA aktif selama fokus ada di chrome Compose --
    // begitu user klik ke dalam halaman web (kondisi paling umum), semuanya
    // diam. `onNativeKeyDown` dipasang ulang tiap kali engine ATAU
    // shortcutActions berganti supaya closure di dalamnya selalu memegang
    // tabs/activeTabId/dll YANG TERBARU (shortcutActions sendiri sudah
    // di-remember dengan key yang sama di atas).
    //
    // `onPageFullscreenChange` (BARU): sinkronkan HTML5 fullscreen HALAMAN
    // (video/chart TradingView, dst) dengan fullscreen APLIKASI kita --
    // begitu halaman masuk HTML5 fullscreen, chrome aplikasi (tab bar,
    // address bar, dst) ikut disembunyikan otomatis (persis seperti Chrome
    // sungguhan), dan kalau halaman keluar dari fullscreen-nya sendiri
    // (mis. video selesai, bukan lewat ESC kita), fullscreen aplikasi ikut
    // ditutup juga -- supaya dua state ini tidak pernah nyangkut tidak
    // sinkron satu sama lain.
    DisposableEffect(browserEngine, shortcutActions) {
        browserEngine?.onNativeKeyDown = { windowsKeyCode, isCtrl, isShift, isAlt ->
            handleBrowserShortcutsNative(windowsKeyCode, isCtrl, isShift, isAlt, shortcutActions)
        }
        browserEngine?.onPageFullscreenChange = { isPageFullscreen ->
            setWorkspaceFullscreen(isPageFullscreen)
        }
        onDispose {
            browserEngine?.onNativeKeyDown = null
            browserEngine?.onPageFullscreenChange = null
        }
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

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (!isWorkspaceFullscreen) {
                    ActivityBar(
                        activePanel = activePanel,
                        onSelectPanel = { panel ->
                            // Bug #9 fix: klik icon yang SUDAH aktif & sidebar
                            // sedang terbuka -> tutup sidebar (toggle). Klik icon
                            // lain, atau klik icon yang sama saat sidebar lagi
                            // tertutup -> buka sidebar dengan panel itu.
                            if (panel == activePanel && isSidebarVisible) {
                                isSidebarVisible = false
                            } else {
                                activePanel = panel
                                isSidebarVisible = true
                            }
                        },
                        isCopilotVisible = isCopilotVisible,
                        onToggleCopilot = { isCopilotVisible = !isCopilotVisible },
                        onOpenSettings = { isSettingsOpen = true }
                    )
                    if (isSidebarVisible) {
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
                }

                Workspace(
                    engine = browserEngine,
                    onEngineForTab = { tabId, engine -> tabEngines[tabId] = engine },
                    onTabNavigated = { tabId, url, title ->
                        val index = tabs.indexOfFirst { it.id == tabId }
                        if (index >= 0) {
                            tabs[index] = tabs[index].copy(
                                url = url.ifBlank { tabs[index].url },
                                title = title.ifBlank { tabs[index].title }
                            )
                        }
                        // Catat ke History (Prioritas 3) begitu URL berubah --
                        // termasuk untuk tab background (lihat catatan di
                        // TabbedBrowserHost.kt).
                        if (url.isNotBlank()) {
                            HistoryStore.record(url, title)
                        }
                    },
                    isIncognito = isIncognito,
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
                    onToggleFullscreen = { setWorkspaceFullscreen(!isWorkspaceFullscreen) },
                    isFindBarOpen = isFindBarOpen,
                    onCloseFindBar = { isFindBarOpen = false; browserEngine?.stopFind() },
                    isMenuOpen = isMenuOpen,
                    onOpenMenu = { isMenuOpen = true },
                    onDismissMenu = { isMenuOpen = false },
                    menuAnchorPositionPx = menuAnchorPositionPx,
                    menuAnchorSizePx = menuAnchorSizePx,
                    onMenuButtonPositioned = { position, size ->
                        menuAnchorPositionPx = position
                        menuAnchorSizePx = size
                    },
                    onShowPanel = { panel -> activePanel = panel; isSidebarVisible = true },
                    onOpenSettings = { isSettingsOpen = true },
                    onRequestExit = onRequestExit,
                    onOpenNewWindow = onOpenNewWindow,
                    onOpenIncognitoWindow = onOpenIncognitoWindow,
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

            // Bug #3 fix: strip 6dp murni Compose di paling bawah -- SENGAJA
            // di luar Row di atas (bukan overlay), supaya benar-benar TIDAK
            // ketutupan SwingPanel milik JCEFBrowserView. Cuma tampil saat
            // window floating (bukan maximized/fullscreen -- resize manual
            // tidak relevan di kondisi itu).
            if (!isWorkspaceFullscreen && windowState?.placement == WindowPlacement.Floating) {
                HorizontalResizeHandle(onDragDeltaPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp().value }
                    windowState?.let { ws ->
                        val newHeight = (ws.size.height.value + deltaDp).coerceAtLeast(480f)
                        ws.size = DpSize(ws.size.width, newHeight.dp)
                    }
                })
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
    onEngineForTab: (tabId: String, engine: JCEFBrowserEngine) -> Unit,
    onTabNavigated: (tabId: String, url: String, title: String) -> Unit,
    isIncognito: Boolean,
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
    menuAnchorPositionPx: androidx.compose.ui.geometry.Offset?,
    menuAnchorSizePx: androidx.compose.ui.geometry.Size?,
    onMenuButtonPositioned: (androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Size) -> Unit,
    onShowPanel: (SidePanel) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestExit: () -> Unit,
    onOpenNewWindow: () -> Unit,
    onOpenIncognitoWindow: () -> Unit,
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
                            onMenuButtonPositioned = onMenuButtonPositioned,
                            addressFocusRequester = addressFocusRequester,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 44.dp)) {
                            BrowserMenu(
                                expanded = isMenuOpen,
                                onDismiss = onDismissMenu,
                                anchorPositionInWindowPx = menuAnchorPositionPx,
                                anchorSizeInWindowPx = menuAnchorSizePx,
                                onNewTab = onNewTab,
                                onNewWindow = onOpenNewWindow,
                                onNewIncognitoWindow = onOpenIncognitoWindow,
                                onShowHistory = { onShowPanel(SidePanel.HISTORY) },
                                onShowBookmarks = { onShowPanel(SidePanel.BOOKMARKS) },
                                onShowDownloads = { onShowPanel(SidePanel.DOWNLOADS) },
                                onShowRecentTabs = { onShowPanel(SidePanel.EXPLORER) },
                                onPrint = { engine?.print() },
                                onZoomIn = { engine?.zoomIn() },
                                onZoomOut = { engine?.zoomOut() },
                                onResetZoom = { engine?.resetZoom() },
                                onFind = { /* dibuka lewat isFindBarOpen di Workbench, lihat shortcutActions */ },
                                onOpenDevTools = { engine?.openDevTools() },
                                onOpenSettings = onOpenSettings,
                                onClearBrowsingData = {
                                    HistoryStore.clear()
                                    engine?.clearBrowsingData()
                                },
                                onExit = onRequestExit
                            )
                        }
                    }
                }
            },
            browserContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    TabbedBrowserHost(
                        modifier = Modifier.fillMaxSize(),
                        tabs = tabs,
                        activeTabId = activeTabId,
                        isIncognito = isIncognito,
                        onEngineForTab = onEngineForTab,
                        onTabNavigated = onTabNavigated
                    )
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
