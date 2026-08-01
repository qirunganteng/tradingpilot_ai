package com.tradepilot.desktop.updater

/**
 * Bentuk file `version.json` yang di-upload CI ke GitHub Release "latest"
 * (lihat step "Generate version manifest" di .github/workflows/desktop-build.yml).
 *
 * Parser JSON manual super-minimal, pola yang sama seperti
 * shared/data/gateway/json/MinimalJson.kt -- TIDAK di-reuse langsung karena
 * itu `internal` di module :shared (tidak visible dari :app), dan
 * version.json cuma 4 field flat, jadi duplikasi kecil ini lebih murah
 * daripada mengubah visibility MinimalJson lintas modul.
 */
data class UpdateManifest(
    val commitSha: String,
    val downloadUrl: String,
    val builtAt: String,
    val notes: String
) {
    companion object {
        fun parse(json: String): UpdateManifest? {
            val sha = field(json, "commitSha")?.takeIf { it.isNotBlank() } ?: return null
            val url = field(json, "downloadUrl")?.takeIf { it.isNotBlank() } ?: return null
            val builtAt = field(json, "builtAt") ?: ""
            val notes = field(json, "notes") ?: ""
            return UpdateManifest(commitSha = sha, downloadUrl = url, builtAt = builtAt, notes = notes)
        }

        private fun field(json: String, name: String): String? {
            val regex = Regex("\"$name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
            val raw = regex.find(json)?.groupValues?.get(1) ?: return null
            return raw
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\")
        }
    }
}
