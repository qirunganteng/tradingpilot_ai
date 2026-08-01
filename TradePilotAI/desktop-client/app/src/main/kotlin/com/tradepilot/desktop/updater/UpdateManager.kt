package com.tradepilot.desktop.updater

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** State auto-updater yang diamati UpdateBanner.kt untuk tampilan. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val manifest: UpdateManifest, val stagingDir: File) : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/**
 * Orkestrasi auto-updater sesuai keputusan produk (bukan asumsi sepihak):
 *  - Cek update HANYA SEKALI saat app pertama dibuka (dipanggil dari
 *    Workbench.kt lewat LaunchedEffect(Unit), tidak ada polling berkala).
 *  - Kalau ada update: download OTOMATIS di background, TANPA nanya user.
 *  - TAPI install/restart TETAP wajib klik konfirmasi user -- app trading
 *    tidak boleh tiba-tiba nutup sendiri pas user lagi entry posisi.
 *
 * Singleton object (bukan class instance per-composition) supaya state-nya
 * bertahan walau Workbench() recompose, dan supaya gampang dipanggil dari
 * UpdateBanner.kt tanpa perlu di-pass sebagai parameter berantai.
 */
object UpdateManager {
    var state: UpdateState by mutableStateOf(UpdateState.Idle)
        private set

    /**
     * Alur lengkap: cek manifest -> (kalau ada update & app terdeteksi
     * jalan sebagai instalasi native, bukan `gradlew run`/IDE) -> download
     * -> extract ke staging -> state jadi ReadyToInstall, siap ditawarkan
     * lewat UpdateBanner.
     */
    suspend fun checkAndDownload() {
        state = UpdateState.Checking
        val result = withContext(Dispatchers.IO) {
            val manifest = UpdateChecker.checkForUpdate() ?: return@withContext null
            val installDir = UpdateInstaller.currentInstallDir()
                // Tidak bisa deteksi lokasi instalasi (mis. dijalankan lewat
                // `gradlew run`/IDE saat development) -- diam-diam skip,
                // JANGAN tawarkan update yang tidak bisa dipasang.
                ?: return@withContext null

            val zipFile = UpdateInstaller.downloadZip(manifest.downloadUrl) { downloaded, total ->
                state = UpdateState.Downloading(downloaded, total)
            } ?: return@withContext Pair(manifest, null as File?)

            val stagingDir = UpdateInstaller.extractToStaging(zipFile)
            zipFile.delete()
            Pair(manifest, stagingDir)
        }

        when {
            result == null -> state = UpdateState.Idle
            result.second == null -> state = UpdateState.Failed("Gagal download/ekstrak update")
            else -> state = UpdateState.ReadyToInstall(result.first, result.second!!)
        }
    }

    /** Dipanggil saat user klik "Restart Sekarang" di banner. */
    fun installAndRestart() {
        val current = state as? UpdateState.ReadyToInstall ?: return
        val installDir = UpdateInstaller.currentInstallDir() ?: return
        val exeName = UpdateInstaller.currentExeName() ?: return
        UpdateInstaller.launchHelperAndExit(installDir, current.stagingDir, exeName)
        exitProcess(0)
    }

    /** Dipanggil saat user klik "Lewati versi ini" -- tidak akan ditawarkan lagi. */
    fun skipThisVersion() {
        val current = state as? UpdateState.ReadyToInstall
        if (current != null) {
            UpdatePreferences.setSkippedSha(current.manifest.commitSha)
            current.stagingDir.deleteRecursively()
        }
        state = UpdateState.Idle
    }
}
