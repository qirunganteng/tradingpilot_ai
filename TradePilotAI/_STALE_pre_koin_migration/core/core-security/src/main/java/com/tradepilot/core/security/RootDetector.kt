package com.tradepilot.core.security

import java.io.File

/**
 * Deteksi root sederhana (heuristik file-check). Bukan pengganti
 * library dedicated (mis. RootBeer) — placeholder Fase 0, akan
 * diperkuat di Fase 9 (Security Hardening).
 */
object RootDetector {
    private val suspiciousPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    fun isLikelyRooted(): Boolean = suspiciousPaths.any { File(it).exists() }
}
