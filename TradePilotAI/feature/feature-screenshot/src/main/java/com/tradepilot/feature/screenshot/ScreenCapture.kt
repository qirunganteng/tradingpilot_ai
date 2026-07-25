package com.tradepilot.feature.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Module Screenshot (Blueprint 0): capture, crop, compress.
 *
 * Catatan teknis: capture dilakukan lewat `View.draw(Canvas)` pada WebView,
 * BUKAN lewat MediaProjection/screen-record — jadi tidak butuh izin
 * "record screen" yang mencolok, dan tetap bekerja walau app tidak
 * dalam fokus penuh layar (asal WebView sudah selesai render).
 */
class ScreenCapture @Inject constructor() {

    /**
     * Ambil bitmap penuh dari sebuah View (dipakai untuk capture area WebView).
     *
     * CATATAN OPTIMASI/RELIABILITAS: WebView hardware-accelerated (default sejak
     * Android modern) kadang menghasilkan bitmap kosong/hitam saat di-capture
     * langsung lewat View.draw(Canvas), karena kontennya di-render di layer GPU
     * terpisah. Trik standar: paksa View.LAYER_TYPE_SOFTWARE sesaat sebelum
     * capture, lalu kembalikan ke LAYER_TYPE_HARDWARE setelahnya supaya WebView
     * tetap smooth saat dipakai scroll/pan chart seperti biasa.
     */
    fun captureView(view: View): Bitmap {
        val originalLayerType = view.layerType
        if (originalLayerType != View.LAYER_TYPE_SOFTWARE) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        } finally {
            if (originalLayerType != View.LAYER_TYPE_SOFTWARE) {
                view.setLayerType(originalLayerType, null)
            }
        }
    }

    /** Crop opsional (mis. buang toolbar) sebelum dikirim ke AI. */
    fun crop(bitmap: Bitmap, top: Int = 0, bottom: Int = bitmap.height): Bitmap {
        val safeBottom = bottom.coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, 0, top, bitmap.width, safeBottom - top)
    }

    /**
     * OPTIMASI PERFORMA: chart screenshot dari device modern bisa 1440x3200px+.
     * Gemini tidak butuh resolusi setinggi itu untuk membaca candle/level harga,
     * dan payload besar memperlambat upload + menambah pemakaian data user.
     * Downscale ke lebar maksimum sebelum compress — mengurangi ukuran file
     * ~60-80% pada device resolusi tinggi tanpa menurunkan kualitas analisa AI
     * secara berarti (garis harga & candle tetap terbaca jelas pada 1280px).
     */
    fun downscaleIfNeeded(bitmap: Bitmap, maxWidth: Int = 1280): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val targetHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)
    }

    /**
     * Compress ke JPEG. Gemini Free API punya batas ukuran payload,
     * jadi kompresi wajib sebelum dikirim (Blueprint: "Module Compress").
     */
    fun compressToJpeg(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /** Pipeline lengkap capture -> downscale -> compress, dipakai AppRootViewModel & Copilot. */
    fun captureCompressed(view: View, maxWidth: Int = 1280, quality: Int = 80): ByteArray {
        val raw = captureView(view)
        val scaled = downscaleIfNeeded(raw, maxWidth)
        return compressToJpeg(scaled, quality)
    }
}
