package com.tradepilot.desktop.updater

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Download + verifikasi + extract + swap file lewat helper `.bat` terpisah.
 *
 * KENAPA TIDAK REPLACE LANGSUNG: distributable Windows project ini adalah
 * portable folder (`createDistributable`, lihat desktop-build.yml), BUKAN
 * installer MSI. Selama app masih berjalan, OS Windows mengunci
 * "TradePilot AI.exe" dan seluruh .jar di dalam folder app/ -- proses yang
 * sedang jalan tidak bisa menimpa dirinya sendiri. Makanya update baru
 * di-extract ke folder staging terpisah dulu, lalu sebuah helper `.bat`
 * yang berjalan sebagai proses TERPISAH (bukan child yang ikut mati) yang
 * menunggu app ini benar-benar exit, baru menimpa folder instalasi asli --
 * dengan backup + rollback otomatis kalau exe baru gagal start.
 */
object UpdateInstaller {

    private val client: HttpClient = HttpClient.newBuilder().build()

    fun currentInstallDir(): File? = currentExeFile()?.parentFile

    fun currentExeName(): String? = currentExeFile()?.name

    private fun currentExeFile(): File? {
        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        val file = File(command)
        return if (file.name.endsWith(".exe", ignoreCase = true)) file else null
    }

    /** Download zip ke temp file. Return null kalau gagal (mis. offline). */
    fun downloadZip(url: String, onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit): File? {
        var tempZip: File? = null
        return try {
            val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() !in 200..299) return null
            val totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
            tempZip = Files.createTempFile("tradepilot-update-", ".zip").toFile()
            response.body().use { input ->
                tempZip.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, totalBytes)
                    }
                }
            }
            tempZip
        } catch (e: Exception) {
            tempZip?.delete()
            null
        }
    }

    /**
     * Verifikasi: hitung SHA-256 file yang sudah di-download, bandingkan
     * dengan yang tercatat di version.json (diisi CI saat build, lihat
     * desktop-build.yml). Kalau manifest tidak punya sha256 (mis. build
     * lama sebelum field ini ada), verifikasi DILEWATI (bukan gagal) --
     * supaya tidak mem-brick auto-update untuk transisi ke skema baru ini.
     * Kalau manifest PUNYA sha256 tapi tidak cocok -> file dianggap
     * korup/berubah di tengah jalan, JANGAN dipasang.
     */
    fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedSha256, ignoreCase = true)
    }

    /** Extract zip ke folder staging baru di temp dir. Return null kalau gagal. */
    fun extractToStaging(zipFile: File): File? {
        return try {
            val stagingDir = Files.createTempDirectory("tradepilot-update-staging-").toFile()
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(stagingDir, entry.name)
                    val normalizedStaging = stagingDir.canonicalPath + File.separator
                    if (!outFile.canonicalPath.startsWith(normalizedStaging)) {
                        throw SecurityException("Entry zip mencurigakan: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out -> zip.copyTo(out) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            stagingDir
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Tulis helper batch script + jalankan sebagai proses terpisah
     * (detached, TIDAK ikut mati saat app exit).
     *
     * Alur helper (dengan ROLLBACK, bukan cuma swap satu arah):
     *  1. Tunggu proses app lama (PID) benar-benar exit.
     *  2. BACKUP: mirror INSTALL_DIR ke BACKUP_DIR (bukan cuma swap
     *     langsung) -- supaya ada yang bisa dikembalikan kalau versi baru
     *     ternyata gagal jalan.
     *  3. SWAP: mirror STAGING_DIR ke INSTALL_DIR (install versi baru).
     *  4. START versi baru, tunggu sebentar, CEK apakah prosesnya benar-
     *     benar hidup (bukan langsung crash startup).
     *  5a. Kalau hidup -> bersihkan STAGING_DIR & BACKUP_DIR, selesai.
     *  5b. Kalau TIDAK terdeteksi hidup dalam waktu tunggu -> ROLLBACK:
     *     mirror BACKUP_DIR balik ke INSTALL_DIR, start ulang versi LAMA,
     *     supaya user tidak ditinggal dengan instalasi yang rusak.
     *
     * `cmd /c <path>` TANPA "start" -- ProcessBuilder Windows mengutip
     * ulang tiap elemen array, dan argumen "start"-related rawan ke-quote
     * dobel sehingga command gagal jalan TANPA exception di sisi Java
     * (silent failure -- ini penyebab auto-update sebelumnya tidak pernah
     * benar-benar terpasang meski proses download terlihat sukses).
     */
    fun launchHelperAndExit(installDir: File, stagingDir: File, exeName: String) {
        val pid = ProcessHandle.current().pid()
        val backupDir = File(System.getProperty("java.io.tmpdir"), "tradepilot-update-backup")
        val helperScript = File(System.getProperty("java.io.tmpdir"), "tradepilot-updater-helper.bat")
        helperScript.writeText(
            """
            @echo off
            set "PID=$pid"
            set "INSTALL_DIR=${installDir.absolutePath}"
            set "STAGING_DIR=${stagingDir.absolutePath}"
            set "BACKUP_DIR=${backupDir.absolutePath}"
            set "EXE_NAME=$exeName"

            :waitloop
            tasklist /FI "PID eq %PID%" 2>NUL | find /I "%PID%" >NUL
            if "%ERRORLEVEL%"=="0" (
                timeout /t 1 /nobreak >NUL
                goto waitloop
            )

            REM Jeda ekstra supaya OS benar-benar melepas file lock setelah proses exit.
            timeout /t 2 /nobreak >NUL

            REM Backup instalasi lama sebelum ditimpa -- dasar buat rollback.
            robocopy "%INSTALL_DIR%" "%BACKUP_DIR%" /MIR /IS /IT /R:3 /W:1 >NUL

            REM Pasang versi baru.
            robocopy "%STAGING_DIR%" "%INSTALL_DIR%" /MIR /IS /IT /R:5 /W:2 >NUL

            start "" "%INSTALL_DIR%\%EXE_NAME%"

            REM Beri waktu app baru untuk benar-benar hidup sebelum dianggap sukses.
            timeout /t 4 /nobreak >NUL

            tasklist /FI "IMAGENAME eq %EXE_NAME%" 2>NUL | find /I "%EXE_NAME%" >NUL
            if "%ERRORLEVEL%"=="0" (
                REM Sukses -- bersihkan staging & backup.
                rmdir /S /Q "%STAGING_DIR%" >NUL 2>&1
                rmdir /S /Q "%BACKUP_DIR%" >NUL 2>&1
            ) else (
                REM Versi baru tidak terdeteksi hidup -- ROLLBACK ke backup.
                robocopy "%BACKUP_DIR%" "%INSTALL_DIR%" /MIR /IS /IT /R:5 /W:2 >NUL
                start "" "%INSTALL_DIR%\%EXE_NAME%"
                rmdir /S /Q "%STAGING_DIR%" >NUL 2>&1
                rmdir /S /Q "%BACKUP_DIR%" >NUL 2>&1
            )

            (goto) 2>NUL & del "%~f0"
            """.trimIndent()
        )
        ProcessBuilder("cmd.exe", "/c", helperScript.absolutePath)
            .directory(installDir)
            .start()
    }
}
