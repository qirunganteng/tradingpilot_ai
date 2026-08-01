package com.tradepilot.desktop.updater

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.zip.ZipInputStream

/**
 * Download + extract + swap file lewat helper `.bat` terpisah.
 *
 * KENAPA TIDAK REPLACE LANGSUNG: distributable Windows project ini adalah
 * portable folder (`createDistributable`, lihat desktop-build.yml), BUKAN
 * installer MSI. Selama app masih berjalan, OS Windows mengunci
 * "TradePilot AI.exe" dan seluruh .jar di dalam folder app/ -- proses yang
 * sedang jalan tidak bisa menimpa dirinya sendiri. Makanya update baru
 * di-extract ke folder staging terpisah dulu, lalu sebuah helper `.bat`
 * yang berjalan sebagai proses TERPISAH (bukan child yang ikut mati) yang
 * menunggu app ini benar-benar exit, baru menimpa folder instalasi asli.
 */
object UpdateInstaller {

    private val client: HttpClient = HttpClient.newBuilder().build()

    /**
     * Root folder instalasi = folder tempat "TradePilot AI.exe" berada.
     * Return null kalau app TIDAK sedang berjalan sebagai native launcher
     * jpackage (mis. dijalankan lewat `gradlew run` / IDE saat development)
     * -- dalam kasus itu auto-update memang tidak bisa/tidak perlu jalan.
     */
    fun currentInstallDir(): File? = currentExeFile()?.parentFile

    fun currentExeName(): String? = currentExeFile()?.name

    private fun currentExeFile(): File? {
        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        val file = File(command)
        return if (file.name.endsWith(".exe", ignoreCase = true)) file else null
    }

    /** Download zip ke temp file. Return null kalau gagal (mis. offline). */
    fun downloadZip(url: String, onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit): File? {
        // tempZip dideklarasikan di luar try supaya bisa dibersihkan di catch
        // kalau exception terjadi DI TENGAH penulisan (mis. koneksi putus
        // separuh jalan) -- sebelumnya file separuh-jadi ini tidak pernah
        // dihapus dan menumpuk di temp dir tiap kali download gagal.
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

    /** Extract zip ke folder staging baru di temp dir. Return null kalau gagal. */
    fun extractToStaging(zipFile: File): File? {
        return try {
            val stagingDir = Files.createTempDirectory("tradepilot-update-staging-").toFile()
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(stagingDir, entry.name)
                    // Cegah Zip Slip -- pastikan hasil extract tidak bisa keluar
                    // dari stagingDir walau nama entry berisi "../".
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
     * (detached, TIDAK ikut mati saat app exit). Helper inilah yang
     * melakukan swap folder instalasi setelah app benar-benar tertutup
     * (supaya file tidak "locked" oleh proses yang masih jalan).
     *
     * FIX BUG (auto-update tidak jalan): versi sebelumnya memanggil
     * `cmd /c start "" /min <path>` lewat ProcessBuilder array-of-args --
     * ProcessBuilder di Windows MENGUTIP ULANG setiap elemen array secara
     * otomatis kalau elemen itu "terlihat perlu di-quote", dan elemen
     * `"\"\""` (dua karakter tanda kutip literal) rawan KEQUOTE DOBEL saat
     * direkonstruksi jadi satu command line untuk CreateProcess -- hasilnya
     * command line yang dikirim ke `start` bisa jadi rusak/beda dari yang
     * dimaksud, dan `start` gagal menjalankan helper TANPA melempar
     * exception apa pun di sisi Java (makanya "gagal diam-diam" -- inilah
     * kemungkinan besar penyebab auto-update belum pernah benar-benar
     * jalan meski proses check+download+extract di atas semuanya sukses).
     *
     * Fix: HILANGKAN `start` dari rantai perintah sama sekali -- langsung
     * `cmd /c <path-helper>` TANPA argumen tambahan yang rawan quote. Ini
     * kehilangan efek "sembunyikan window cmd" yang tadinya dikasih `start
     * /min` (jadi akan ada jendela cmd hitam yang muncul sebentar ~2-3
     * detik saat proses swap file berjalan), TAPI jauh lebih bisa
     * diandalkan -- tidak ada lagi celah salah-quote karena cuma SATU
     * argumen path (yang di-quote otomatis oleh ProcessBuilder dengan
     * benar kalau mengandung spasi, tanpa ambiguitas apa pun).
     */
    fun launchHelperAndExit(installDir: File, stagingDir: File, exeName: String) {
        val pid = ProcessHandle.current().pid()
        val helperScript = File(System.getProperty("java.io.tmpdir"), "tradepilot-updater-helper.bat")
        helperScript.writeText(
            """
            @echo off
            set "PID=$pid"
            set "INSTALL_DIR=${installDir.absolutePath}"
            set "STAGING_DIR=${stagingDir.absolutePath}"
            set "EXE_NAME=$exeName"

            :waitloop
            tasklist /FI "PID eq %PID%" 2>NUL | find /I "%PID%" >NUL
            if "%ERRORLEVEL%"=="0" (
                timeout /t 1 /nobreak >NUL
                goto waitloop
            )

            REM Jeda ekstra supaya OS benar-benar melepas file lock setelah proses exit.
            timeout /t 2 /nobreak >NUL

            REM /MIR = mirror exact (hapus file di INSTALL_DIR yang sudah
            REM tidak ada di STAGING_DIR, mis. jar lama yang namanya berubah)
            REM -- /E saja cuma menambah/menimpa dan bisa menyisakan file usang.
            robocopy "%STAGING_DIR%" "%INSTALL_DIR%" /MIR /IS /IT /R:5 /W:2 >NUL

            start "" "%INSTALL_DIR%\%EXE_NAME%"

            rmdir /S /Q "%STAGING_DIR%" >NUL 2>&1
            (goto) 2>NUL & del "%~f0"
            """.trimIndent()
        )
        ProcessBuilder("cmd.exe", "/c", helperScript.absolutePath)
            .directory(installDir)
            .start()
    }
}
