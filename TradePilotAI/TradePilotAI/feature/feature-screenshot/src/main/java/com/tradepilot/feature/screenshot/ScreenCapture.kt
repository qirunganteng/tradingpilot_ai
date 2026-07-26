package com.tradepilot.feature.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.webkit.WebView
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenCapture @Inject constructor() {

    fun captureView(view: View): Bitmap? {
        if (view.width <= 0 || view.height <= 0) return null
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun captureCompressed(webView: WebView, quality: Int = 80): ByteArray? {
        val bitmap = captureView(webView) ?: return null
        return CaptureUtil.compressBitmapToByteArray(bitmap, Bitmap.CompressFormat.JPEG, quality)
    }
}
