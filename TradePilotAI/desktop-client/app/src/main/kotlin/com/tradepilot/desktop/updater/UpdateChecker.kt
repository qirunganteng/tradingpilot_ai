package com.tradepilot.desktop.updater

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * URL manifest -- selalu di release tag "latest" (lihat konvensi
 * "Publish ke Release latest" di desktop-build.yml: file di release ini
 * SELALU ditimpa versi terbaru, jadi URL-nya tidak pernah berubah).
 */
private const val MANIFEST_URL =
    "https://github.com/qirunganteng/tradingpilot_ai/releases/download/latest/version.json"

/**
 * Cek versi terbaru lewat GitHub Release "latest" -- dipakai BuildInfo.kt
 * (hasil generate Gradle task `generateBuildInfo`, lihat app/build.gradle.kts)
 * sebagai identitas versi lokal, BUKAN semver manual. Alasan: CI project ini
 * push ke tag "latest" yang selalu ditimpa tiap commit ke main (bukan
 * v1.0, v1.1, dst bertingkat), jadi commit SHA adalah identitas versi yang
 * paling murah untuk dijaga -- tidak perlu rajin bump angka versi tiap rilis.
 */
object UpdateChecker {
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Return manifest kalau ADA update yang perlu ditawarkan ke user
     * (SHA remote beda dari SHA lokal, DAN belum pernah di-skip user).
     * Return null untuk semua kasus lain (offline, GitHub down, response
     * tidak valid, atau memang sudah versi terbaru) -- caller tidak perlu
     * bedakan "gagal cek" vs "sudah terbaru", keduanya sama-sama berarti
     * "jangan ganggu user".
     */
    fun checkForUpdate(): UpdateManifest? {
        val manifest = fetchManifest() ?: return null
        if (manifest.commitSha == BuildInfo.COMMIT_SHA) return null
        if (manifest.commitSha == UpdatePreferences.getSkippedSha()) return null
        return manifest
    }

    private fun fetchManifest(): UpdateManifest? {
        return try {
            val request = HttpRequest.newBuilder(URI.create(MANIFEST_URL))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) return null
            UpdateManifest.parse(response.body())
        } catch (e: Exception) {
            null
        }
    }
}
