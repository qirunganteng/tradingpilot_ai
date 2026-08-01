package com.tradepilot.desktop.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.activitybar.SidePanel
import com.tradepilot.desktop.browser.BrowserTab
import com.tradepilot.desktop.theme.AppColors

/**
 * Prioritas 3: Explorer panel VSCode-style. Isinya berubah sesuai
 * [activePanel] yang dipilih dari ActivityBar (lihat ActivityBar.kt).
 *
 * "Klik item explorer harus membuka tab browser" (persis kalimat di prompt)
 * -- itu kenapa setiap baris di sini menerima `onOpenUrl` yang oleh
 * pemanggil (Workbench di Main.kt) diwire ke `newTab(url)` / `selectTab`.
 */
@Composable
fun ExplorerPanel(
    activePanel: SidePanel,
    tabs: List<BrowserTab>,
    activeTabId: String,
    pinnedSites: List<PinnedSite>,
    onOpenUrl: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight().background(AppColors.Surface)) {
        SectionHeader(activePanel.label)
        when (activePanel) {
            SidePanel.EXPLORER -> ExplorerHome(
                tabs = tabs,
                activeTabId = activeTabId,
                pinnedSites = pinnedSites,
                onOpenUrl = onOpenUrl,
                onSelectTab = onSelectTab
            )
            SidePanel.HISTORY -> HistoryList(onOpenUrl = onOpenUrl)
            SidePanel.BOOKMARKS -> BookmarksList(onOpenUrl = onOpenUrl, onRemove = onRemoveBookmark)
            SidePanel.DOWNLOADS -> DownloadsList()
            SidePanel.WORKSPACE -> WorkspaceList(tabs = tabs)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/**
 * Isi default Explorer (Prioritas 3): "Recent Tabs / Bookmarks / History /
 * Downloads / Workspace / Trading Sessions / Pinned Websites" -- dirangkum
 * ringkas di sini (tiap kategori juga punya panel sendiri lewat ActivityBar
 * kalau mau lihat SEMUA isinya, bukan cuma ringkasan).
 */
@Composable
private fun ExplorerHome(
    tabs: List<BrowserTab>,
    activeTabId: String,
    pinnedSites: List<PinnedSite>,
    onOpenUrl: (String) -> Unit,
    onSelectTab: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { GroupLabel("Recent Tabs") }
        items(tabs, key = { "tab-${it.id}" }) { tab ->
            ExplorerRow(
                title = tab.title.ifBlank { "New Tab" },
                subtitle = tab.url,
                isHighlighted = tab.id == activeTabId,
                onClick = { onSelectTab(tab.id) }
            )
        }

        item { GroupLabel("Pinned Websites") }
        items(pinnedSites, key = { "pin-${it.url}" }) { site ->
            ExplorerRow(title = site.label, subtitle = site.url, onClick = { onOpenUrl(site.url) })
        }

        item { GroupLabel("Bookmarks") }
        items(BookmarkStore.entries.take(5), key = { "bm-${it.url}" }) { bm ->
            ExplorerRow(title = bm.title, subtitle = bm.url, onClick = { onOpenUrl(bm.url) })
        }
        if (BookmarkStore.entries.isEmpty()) {
            item { EmptyHint("Belum ada bookmark. Tekan Ctrl+D di sebuah halaman untuk menambah.") }
        }

        item { GroupLabel("History") }
        items(HistoryStore.entries.takeLast(5).reversed(), key = { "hist-${it.visitedAtEpochMillis}" }) { h ->
            ExplorerRow(title = h.title, subtitle = h.url, onClick = { onOpenUrl(h.url) })
        }

        item { GroupLabel("Trading Sessions") }
        item {
            ExplorerRow(
                title = "Sesi saat ini",
                subtitle = "${tabs.size} tab terbuka",
                onClick = {}
            )
        }

        item { GroupLabel("Downloads") }
        item { EmptyHint("Lihat panel Downloads terpisah -- belum ada handler unduhan aktif.") }
    }
}

@Composable
private fun HistoryList(onOpenUrl: (String) -> Unit) {
    if (HistoryStore.entries.isEmpty()) {
        EmptyHint("Belum ada riwayat kunjungan di sesi ini.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(HistoryStore.entries.reversed(), key = { it.visitedAtEpochMillis }) { entry ->
            ExplorerRow(title = entry.title, subtitle = entry.url, onClick = { onOpenUrl(entry.url) })
        }
    }
}

@Composable
private fun BookmarksList(onOpenUrl: (String) -> Unit, onRemove: (String) -> Unit) {
    if (BookmarkStore.entries.isEmpty()) {
        EmptyHint("Belum ada bookmark. Tekan Ctrl+D di sebuah halaman untuk menambah, atau klik ikon bintang di address bar.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(BookmarkStore.entries, key = { it.url }) { entry ->
            ExplorerRow(
                title = entry.title,
                subtitle = entry.url,
                onClick = { onOpenUrl(entry.url) },
                trailing = {
                    IconButton(onClick = { onRemove(entry.url) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus bookmark", tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            )
        }
    }
}

/**
 * Prioritas 3 -- Downloads.
 *
 * CATATAN JUJUR: JCEF (lewat CefClient) butuh `CefDownloadHandler` yang
 * didaftarkan supaya bisa tahu ada file yang mulai/sedang/selesai diunduh --
 * ini TIDAK ada di JCEFBootstrap.kt/JCEFBrowserEngine.kt versi paket file
 * ini (README asli bilang 2 file itu "paling rawan", jadi sengaja tidak
 * saya ubah tanpa sepengetahuan kamu). Panel ini SUDAH siap tampilan &
 * modelnya (DownloadEntry), tinggal disambungkan begitu handler-nya dibuat.
 * Menampilkan data downloads palsu di sini akan menyesatkan (user pikir
 * fitur unduh sungguhan sudah jalan), jadi saya tampilkan status jujur.
 */
@Composable
private fun DownloadsList() {
    EmptyHint(
        "Belum ada handler unduhan (CefDownloadHandler) yang disambungkan. " +
            "UI panel ini sudah siap -- lihat catatan di ExplorerModels.kt " +
            "(DownloadEntry) untuk cara menyambungkannya."
    )
}

@Composable
private fun WorkspaceList(tabs: List<BrowserTab>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { GroupLabel("Trading Sessions") }
        item { ExplorerRow(title = "Sesi saat ini", subtitle = "${tabs.size} tab terbuka", onClick = {}) }
        item { GroupLabel("Semua Tab") }
        items(tabs, key = { it.id }) { tab ->
            ExplorerRow(title = tab.title.ifBlank { "New Tab" }, subtitle = tab.url, onClick = {})
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.TextSecondary,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextDisabled,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun ExplorerRow(
    title: String,
    subtitle: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlighted) AppColors.SurfaceRaised else AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Language, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AppColors.TextDisabled, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}
