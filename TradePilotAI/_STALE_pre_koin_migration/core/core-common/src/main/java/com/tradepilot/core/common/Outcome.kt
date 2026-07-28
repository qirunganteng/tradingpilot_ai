package com.tradepilot.core.common

/**
 * Wrapper hasil operasi I/O di seluruh layer domain/data.
 * Tidak ada exception mentah yang boleh naik langsung ke UI —
 * semua dibungkus di sini (lihat Blueprint bagian 12: Error Handling).
 */
sealed class Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>()
    data class Error(val error: AppError) : Outcome<Nothing>()
    data object Loading : Outcome<Nothing>()
}

sealed class AppError(val message: String) {
    data class NetworkError(val cause: String) : AppError(cause)
    data class AIProviderError(val cause: String) : AppError(cause)
    data class DatabaseError(val cause: String) : AppError(cause)
    data class SecurityError(val cause: String) : AppError(cause)
    data class Unknown(val cause: String) : AppError(cause)
}
