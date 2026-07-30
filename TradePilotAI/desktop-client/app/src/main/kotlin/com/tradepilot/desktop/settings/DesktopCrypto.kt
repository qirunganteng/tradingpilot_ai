package com.tradepilot.desktop.settings

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enkripsi token AI Gateway di desktop-client (Fase 9).
 *
 * JUJUR SOAL BATASANNYA: ini AES-256-GCM dengan kunci yang di-generate
 * sekali lalu disimpan sebagai FILE terpisah (desktop-client.key) dengan
 * permission dibatasi cuma owner yang bisa baca. Ini BUKAN setara dengan
 * SecureKeyStore (Tink + Android Keystore, hardware-backed) di
 * core-security android-client -- itu kunci-nya tidak pernah keluar dari
 * secure hardware element. Di sini kunci tetap berupa file biasa di disk,
 * "cuma" dilindungi permission OS.
 *
 * Kenapa bukan Windows DPAPI / macOS Keychain / Secret Service Linux
 * (yang jauh lebih aman, setara Android Keystore): itu butuh implementasi
 * BERBEDA per-OS (JNI/native binding atau library tambahan besar), scope-nya
 * di luar satu iterasi ini. TODO Fase 10 kalau mau upgrade ke situ.
 *
 * Tetap jauh lebih baik dari plain text (Fase 8) -- orang yang copy-paste
 * isi desktop-client.properties saja (tanpa file .key-nya, dan tanpa akses
 * user account yang sama) tidak akan dapat token asli.
 */
internal object DesktopCrypto {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BYTES = 32 // AES-256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".tradepilot").apply { mkdirs() }
    }
    private val keyFile: File by lazy { File(configDir, "desktop-client.key") }

    private val secretKey: SecretKeySpec by lazy { loadOrCreateKey() }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Simpan sebagai satu string: base64(iv) + ":" + base64(ciphertext),
        // supaya gampang disimpan sebagai satu value properties biasa.
        return "${Base64.getEncoder().encodeToString(iv)}:${Base64.getEncoder().encodeToString(cipherText)}"
    }

    /** Return null kalau input kosong/rusak/dienkripsi pakai key lain (mis. file .key hilang/ganti). */
    fun decrypt(encoded: String): String? {
        if (encoded.isBlank()) return null
        return try {
            val (ivPart, cipherPart) = encoded.split(":", limit = 2)
            val iv = Base64.getDecoder().decode(ivPart)
            val cipherText = Base64.getDecoder().decode(cipherPart)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadOrCreateKey(): SecretKeySpec {
        if (!keyFile.exists()) {
            val raw = ByteArray(KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
            keyFile.writeBytes(raw)
            restrictToOwnerOnly(keyFile)
            return SecretKeySpec(raw, "AES")
        }
        return SecretKeySpec(keyFile.readBytes(), "AES")
    }

    /**
     * Batasi file cuma bisa dibaca/ditulis oleh owner (setara chmod 600).
     * setReadable/setWritable(_, ownerOnly=true) portable di Windows & POSIX
     * lewat java.io.File biasa -- tidak butuh API khusus per-OS seperti
     * java.nio POSIX permissions (yang gagal di Windows).
     */
    private fun restrictToOwnerOnly(file: File) {
        try {
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setReadable(true, true)
            file.setWritable(true, true)
        } catch (e: Exception) {
            // Kalau filesystem tidak support (jarang), diamkan -- lebih baik
            // key tetap ada & jalan daripada aplikasi crash cuma gara-gara
            // pengetatan permission gagal.
        }
    }
}
