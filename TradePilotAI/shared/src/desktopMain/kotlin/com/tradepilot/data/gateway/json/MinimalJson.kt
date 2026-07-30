package com.tradepilot.data.gateway.json

/**
 * Parser/writer JSON manual super-minimal, KHUSUS untuk field flat (bukan
 * nested object) yang dipakai response Worker (AnalysisResult, dst).
 *
 * KENAPA MANUAL (bukan kotlinx.serialization/Moshi): modul network desktop
 * ini masih kecil (2 endpoint: calculate-risk, analyze) dan sengaja
 * dijaga tanpa dependency tambahan (lihat catatan di HttpRiskGatewayRepository).
 * TAPI ini sudah di ambang batas kompleksitas yang wajar untuk pendekatan
 * manual -- kalau nambah field bernested object (bukan cuma array of
 * string), pertimbangkan serius pindah ke kotlinx.serialization.
 *
 * Regex string field-nya menangani escape character standar JSON
 * (\" \\ \n \r \t) supaya field teks bebas seperti "reasoning" (hasil
 * Gemini, bisa berisi tanda kutip/baris baru) tidak salah dipotong.
 */
internal object MinimalJson {

    fun string(json: String, field: String): String? {
        val regex = Regex("\"$field\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val raw = regex.find(json)?.groupValues?.get(1) ?: return null
        return unescape(raw)
    }

    fun double(json: String, field: String): Double? {
        val regex = Regex("\"$field\"\\s*:\\s*(-?[0-9]+(\\.[0-9]+)?)")
        return regex.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun long(json: String, field: String): Long? {
        val regex = Regex("\"$field\"\\s*:\\s*(-?[0-9]+)(?!\\.[0-9])")
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    /** Cuma untuk array of string flat, mis. "method":["ICT","SMC"]. */
    fun stringArray(json: String, field: String): List<String> {
        val arrRegex = Regex("\"$field\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
        val body = arrRegex.find(json)?.groupValues?.get(1) ?: return emptyList()
        val itemRegex = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
        return itemRegex.findAll(body).map { unescape(it.groupValues[1]) }.toList()
    }

    private fun unescape(s: String): String = s
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\")

    fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    /** Builder kecil buat request body -- lihat pemakaian di HttpAIRepository. */
    class ObjectBuilder {
        private val fields = mutableListOf<Pair<String, String>>()

        fun put(key: String, value: String) {
            fields += key to "\"${escape(value)}\""
        }

        fun put(key: String, value: Double) {
            fields += key to value.toString()
        }

        fun put(key: String, value: Boolean) {
            fields += key to value.toString()
        }

        fun putStringArray(key: String, values: List<String>) {
            val joined = values.joinToString(prefix = "[", postfix = "]") { "\"${escape(it)}\"" }
            fields += key to joined
        }

        fun build(): String = fields.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":$v" }
    }
}
