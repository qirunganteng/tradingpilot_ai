package com.tradepilot.desktop.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

/**
 * Browser Settings ala Chrome (BEDA dari [SettingsDialog] yang isinya
 * Gateway/API key -- lihat CONSTITUTION.md/note awal proyek: "tombol
 * pengaturan paling kiri bawah [ActivityBar] = set apikey & pengaturan
 * aplikasi", BUKAN pengaturan browser).
 *
 * BUG DIPERBAIKI: sebelumnya BrowserMenu -> "Settings" dan gear icon
 * ActivityBar SAMA-SAMA membuka [SettingsDialog] (Gateway) yang sama --
 * user klik "Settings" dari menu browser, harusnya lihat pengaturan
 * BROWSER (startup page, privacy, downloads, dst ala Chrome), tapi malah
 * lihat form Gateway URL/Token yang tidak nyambung sama sekali secara
 * konteks. Sekarang dipisah total -- dialog ini genuinely tentang browser.
 *
 * SENGAJA cuma berisi kategori yang BENAR-BENAR fungsional (bukan replika
 * visual semua kategori Chrome yang sebagian besar tidak relevan/belum ada
 * fiturnya di browser trading khusus ini -- mis. Extensions, AI innovations,
 * Search engine chooser, dst TIDAK ada di sini karena memang belum ada
 * implementasinya, daripada menampilkan menu yang keliatan tapi kosong):
 *  - On Startup: BENAR-BENAR mengontrol perilaku startup (lihat Main.kt --
 *    dibaca dari BrowserSettingsStore sebelum keputusan pulihkan sesi/tidak).
 *  - Privacy and security: tombol Clear Browsing Data (reuse aksi yang
 *    sudah ada di BrowserMenu).
 *  - Downloads: tampilkan lokasi folder unduhan (read-only untuk saat ini
 *    -- lihat JCEFBrowserEngine.kt, lokasi hardcode ke `~/Downloads`).
 *  - About: versi aplikasi.
 */
@Composable
fun BrowserSettingsDialog(
    onDismiss: () -> Unit,
    onClearBrowsingData: () -> Unit
) {
    var settings by remember { mutableStateOf(BrowserSettingsStore.load()) }
    var selectedCategory by remember { mutableStateOf(SettingsCategory.ON_STARTUP) }
    var specificUrlsText by remember { mutableStateOf(settings.specificStartupUrls.joinToString("\n")) }

    fun persist(update: BrowserSettings) {
        settings = update
        BrowserSettingsStore.save(update)
    }

    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(width = 620.dp, height = 520.dp),
        title = "Settings"
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Sidebar kategori, ala screenshot Chrome settings.
                    Column(
                        modifier = Modifier.width(200.dp).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        SettingsCategory.entries.forEach { category ->
                            CategoryRow(
                                icon = category.icon,
                                label = category.label,
                                selected = category == selectedCategory,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        when (selectedCategory) {
                            SettingsCategory.ON_STARTUP -> OnStartupSection(
                                settings = settings,
                                specificUrlsText = specificUrlsText,
                                onSpecificUrlsTextChange = { specificUrlsText = it },
                                onModeChange = { mode -> persist(settings.copy(startupMode = mode)) },
                                onSaveSpecificUrls = {
                                    val urls = specificUrlsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                                    persist(settings.copy(specificStartupUrls = urls))
                                }
                            )
                            SettingsCategory.PRIVACY -> PrivacySection(onClearBrowsingData = onClearBrowsingData)
                            SettingsCategory.DOWNLOADS -> DownloadsSection()
                            SettingsCategory.ABOUT -> AboutSection()
                        }
                    }
                }
            }
        }
    }
}

private enum class SettingsCategory(val label: String, val icon: ImageVector) {
    ON_STARTUP("On startup", Icons.Default.PowerSettingsNew),
    PRIVACY("Privacy and security", Icons.Default.Shield),
    DOWNLOADS("Downloads", Icons.Default.Download),
    ABOUT("About", Icons.Default.Info)
}

@Composable
private fun CategoryRow(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(6.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun OnStartupSection(
    settings: BrowserSettings,
    specificUrlsText: String,
    onSpecificUrlsTextChange: (String) -> Unit,
    onModeChange: (StartupMode) -> Unit,
    onSaveSpecificUrls: () -> Unit
) {
    SectionTitle("On startup")
    StartupOptionRow(
        selected = settings.startupMode == StartupMode.NEW_TAB,
        title = "Open the New Tab page",
        subtitle = "Selalu mulai dari halaman Google, mengabaikan sesi terakhir.",
        onClick = { onModeChange(StartupMode.NEW_TAB) }
    )
    StartupOptionRow(
        selected = settings.startupMode == StartupMode.CONTINUE_SESSION,
        title = "Continue where you left off",
        subtitle = "Buka lagi tab & window yang terakhir terbuka (default).",
        onClick = { onModeChange(StartupMode.CONTINUE_SESSION) }
    )
    StartupOptionRow(
        selected = settings.startupMode == StartupMode.SPECIFIC_PAGES,
        title = "Open a specific page or set of pages",
        subtitle = "Satu URL per baris.",
        onClick = { onModeChange(StartupMode.SPECIFIC_PAGES) }
    )
    if (settings.startupMode == StartupMode.SPECIFIC_PAGES) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = specificUrlsText,
            onValueChange = onSpecificUrlsTextChange,
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(start = 32.dp),
            placeholder = { Text("https://my.exness.com/webtrading") }
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSaveSpecificUrls, modifier = Modifier.padding(start = 32.dp)) { Text("Simpan halaman") }
    }
}

@Composable
private fun StartupOptionRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PrivacySection(onClearBrowsingData: () -> Unit) {
    SectionTitle("Privacy and security")
    Text(
        "Hapus cookie & history lokal browser ini (lihat catatan JCEFBrowserEngine.kt " +
            "soal batasan: disk cache TIDAK ikut terhapus, JCEF versi ini tidak expose API-nya).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
    Spacer(Modifier.height(12.dp))
    Button(onClick = onClearBrowsingData) {
        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
        Text("Clear browsing data")
    }
}

@Composable
private fun DownloadsSection() {
    SectionTitle("Downloads")
    val downloadsPath = remember { java.io.File(System.getProperty("user.home"), "Downloads").absolutePath }
    Text("Location", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(4.dp))
    Text(downloadsPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    Spacer(Modifier.height(8.dp))
    Text(
        "Belum bisa diubah lewat UI ini (lokasi hardcode di JCEFBrowserEngine.kt) -- " +
            "dicatat sebagai kandidat kerjaan berikutnya, bukan diklaim sudah bisa.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun AboutSection() {
    SectionTitle("About TradePilot AI")
    Text("Desktop client (JCEF/Compose Desktop)", style = MaterialTheme.typography.bodyMedium)
}
