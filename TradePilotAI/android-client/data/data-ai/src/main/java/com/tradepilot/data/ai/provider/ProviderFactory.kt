package com.tradepilot.data.ai.provider

enum class ProviderType { WORKER_GATEWAY, GEMINI, OPENAI, CLAUDE, DEEPSEEK, QWEN }

/**
 * Factory pemilih AIProvider aktif — memenuhi requirement Blueprint versi 0:
 * "Gemini hanyalah Provider. Provider harus dapat diganti tanpa mengubah
 * aplikasi utama."
 *
 * DEFAULT sekarang WORKER_GATEWAY (lewat Cloudflare Worker AI Gateway) —
 * lebih aman karena API key Gemini tidak pernah disimpan di device.
 * GEMINI (panggil langsung dari device) tetap tersedia sebagai fallback
 * bagi yang belum setup Worker.
 */
class ProviderFactory constructor(
    private val workerProvider: WorkerProvider,
    private val geminiProvider: GeminiProvider
    // TODO: tambahkan provider lain saat diimplementasikan, mis. openAIProvider: OpenAIProvider
) {
    fun create(type: ProviderType): AIProvider = when (type) {
        ProviderType.WORKER_GATEWAY -> workerProvider
        ProviderType.GEMINI -> geminiProvider
        else -> throw NotImplementedError("Provider $type belum diimplementasikan")
    }
}
