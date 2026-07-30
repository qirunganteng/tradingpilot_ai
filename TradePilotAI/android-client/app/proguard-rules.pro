# TradePilot AI - Proguard rules (Fase 9 akan melengkapi)
# Jangan obfuscate model yang di-parse Moshi (data-ai) agar JSON mapping tetap benar.
-keepclassmembers class com.tradepilot.domain.model.** { *; }
-keep class com.tradepilot.core.database.entity.** { *; }

# === Tink (dibawa androidx.security-crypto -> core-security) ===
# R8 gagal ("Missing class") karena Tink mereferensikan anotasi dari
# com.google.errorprone.annotations, javax.annotation, dan
# org.checkerframework yang cuma dipakai saat compile time -- library-nya
# tidak membawa kelas-kelas itu sebagai dependency runtime, jadi R8 tidak
# bisa menemukannya. Ini masalah umum & dikenal luas untuk kombinasi
# androidx.security-crypto + R8 full mode (bukan bug di kode kita).
# Aman di-dontwarn karena semuanya cuma anotasi (CanIgnoreReturnValue,
# CheckReturnValue, Immutable, RestrictedApi, Nullable, GuardedBy) --
# tidak ada efek di runtime kalau hilang.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.checker.**

# Tink pakai reflection (registry pattern) buat cari implementasi primitive
# crypto (AEAD, KeysetHandle, dst) -- kalau di-obfuscate/di-strip, gagal di
# RUNTIME (bukan cuma warning compile time kayak di atas), jadi harus di-keep.
-keep class com.google.crypto.tink.** { *; }
-keep interface com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
