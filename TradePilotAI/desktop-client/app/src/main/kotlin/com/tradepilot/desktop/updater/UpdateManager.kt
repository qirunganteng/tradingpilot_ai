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
    data class Verifying(val manifest: UpdateManifest) : UpdateState
    data class ReadyToInstall(val manifest: UpdateManifest, val stagingDir: File) : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/**
 * Orkestrasi auto-updater:
 *  - Cek update HANYA SEKALI saat app pertama dibuka.
 *  - Kalau ada update: download OTOMATIS di background, verifikasi
 *    checksum SHA-256-nya, extract ke staging.
 *  - Install/restart TETAP wajib klik konfirmasi user.
 *  - Rollback kalau versi baru gagal start ditangani di level helper
 *    script (lihat UpdateInstaller.launchHelperAndExit).
 */
object UpdateManager {
    var state: UpdateState by mutableStateOf(UpdateState.Idle)
        private set

    suspend fun checkAndDownload() {
        state = UpdateState.Checking
        val result = withContext(Dispatchers.IO) {
            val manifest = UpdateChecker.checkForUpdate() ?: return@withContext null
            val installDir = UpdateInstaller.currentInstallDir()
                ?: return@withContext null

            val zipFile = UpdateInstaller.downloadZip(manifest.downloadUrl) { downloaded, total ->
                state = UpdateState.Downloading(downloaded, total)
            } ?: return@withContext Triple(manifest, null as File?, "Gagal download update")

            state = UpdateState.Verifying(manifest)
            if (!UpdateInstaller.verifyChecksum(zipFile, manifest.sha256)) {
                zipFile.delete()
                return@withContext Triple(manifest, null as File?, "Verifikasi file update gagal (checksum tidak cocok) -- file kemungkinan korup/rusak saat diunduh")
            }

            val stagingDir = UpdateInstaller.extractToStaging(zipFile)
            zipFile.delete()
            if (stagingDir == null) {
                Triple(manifest, null as File?, "Gagal ekstrak file update")
            } else {
                Triple(manifest, stagingDir, "")
            }
        }

        when {
            result == null -> state = UpdateState.Idle
            result.second == null -> state = UpdateState.Failed(result.third)
            else -> state = UpdateState.ReadyToInstall(result.first, result.second!!)
        }
    }

    fun installAndRestart() {
        val current = state as? UpdateState.ReadyToInstall ?: return
        val installDir = UpdateInstaller.currentInstallDir() ?: return
        val exeName = UpdateInstaller.currentExeName() ?: return
        UpdateInstaller.launchHelperAndExit(installDir, current.stagingDir, exeName)
        exitProcess(0)
    }

    fun skipThisVersion() {
        val current = state as? UpdateState.ReadyToInstall
        if (current != null) {
            UpdatePreferences.setSkippedSha(current.manifest.commitSha)
            current.stagingDir.deleteRecursively()
        }
        state = UpdateState.Idle
    }
}
