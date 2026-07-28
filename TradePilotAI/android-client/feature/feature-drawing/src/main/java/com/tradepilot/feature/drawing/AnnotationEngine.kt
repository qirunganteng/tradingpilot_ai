package com.tradepilot.feature.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.tradepilot.domain.model.AnalysisResult
import com.tradepilot.domain.model.TradeDirection

/**
 * Draw Engine (Blueprint 0 & versi 5).
 *
 * KETERBATASAN JUJUR: model vision umum seperti Gemini tidak bisa memberikan
 * koordinat piksel presisi di mana Order Block / FVG / BOS berada di chart.
 * Karena itu engine ini TIDAK menggambar garis tepat di titik harga —
 * melainkan menggambar **legend/anotasi ringkasan** di sisi kanan gambar,
 * berwarna sesuai konvensi (hijau=BUY, merah=SELL, kuning=warning/liquidity,
 * biru=info/order block), sambil tetap memakai istilah versi 5 (Trend, Entry,
 * SL, TP, Order Block, Liquidity, FVG, BOS, CHOCH, PDH, PDL, Session).
 *
 * Arsitektur sudah siap untuk upgrade ke overlay presisi di masa depan
 * (lihat Blueprint 17: Future Expansion — layer, zoom, rotate, undo/redo)
 * jika nanti ada model deteksi objek yang mengembalikan bounding box asli.
 */
class AnnotationEngine constructor() {

    fun annotate(source: Bitmap, result: AnalysisResult): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val panelWidth = (output.width * 0.32f).toInt().coerceAtLeast(260)
        val panelLeft = output.width - panelWidth
        val panelPaint = Paint().apply {
            color = Color.argb(200, 20, 20, 20)
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            Rect(panelLeft, 0, output.width, output.height),
            panelPaint
        )

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 28f
            typeface = android.graphics.Typeface.MONOSPACE
        }

        var y = 50f
        val lineHeight = 44f
        val x = panelLeft + 24f

        fun drawLine(label: String, value: String, colorInt: Int) {
            textPaint.color = colorInt
            canvas.drawText("$label: $value", x, y, textPaint)
            y += lineHeight
        }

        val signalColor = when (result.signal) {
            TradeDirection.BUY -> Color.parseColor("#4EC9B0")
            TradeDirection.SELL -> Color.parseColor("#F14C4C")
            TradeDirection.NONE -> Color.parseColor("#DCDCAA")
        }

        drawLine("Pair", result.pair, Color.WHITE)
        drawLine("Trend", result.trend, Color.parseColor("#569CD6"))
        drawLine("Signal", result.signal.name, signalColor)
        drawLine("Entry", result.entry, Color.WHITE)
        drawLine("SL", result.stopLoss, Color.parseColor("#F14C4C"))
        drawLine("TP", result.takeProfit, Color.parseColor("#4EC9B0"))
        drawLine("RR", result.riskReward, Color.WHITE)

        y += 10f
        textPaint.color = Color.parseColor("#DCDCAA")
        textPaint.textSize = 22f
        canvas.drawText("Metode:", x, y, textPaint)
        y += 32f
        result.method.chunked(2).forEach { pair ->
            canvas.drawText(pair.joinToString("  "), x, y, textPaint)
            y += 30f
        }

        return output
    }
}
