package com.tradepilot.desktop.copilot

import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import java.awt.Component
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.imageio.ImageIO

/**
 * Padanan desktop dari ScreenCapture.kt (feature-screenshot, android-client),
 * pipeline yang sama: capture -> downscale -> compress JPEG.
 *
 * KENAPA java.awt.Robot (screen capture), BUKAN render-ke-bitmap langsung:
 * desktop-client pakai JCEF mode "windowed" (lihat JCEFBootstrap.kt) --
 * browser-nya jendela native OS yang di-embed lewat SwingPanel, bukan
 * dirender ke buffer Java yang bisa di-screenshot langsung (itu perlu mode
 * OSR/off-screen-rendering, arsitektur JCEF yang beda & lebih berat resource,
 * belum dipakai). Robot.createScreenCapture() ambil pixel LAYAR SUNGGUHAN di
 * posisi komponennya -- konsekuensinya: window HARUS terlihat & tidak
 * ketutupan window lain saat capture, kalau tidak hasilnya salah/hitam.
 * Batasan ini didokumentasikan jujur di CopilotPanel, bukan disembunyikan.
 */
object DesktopChartCapture {

    sealed class CaptureError(message: String) : Exception(message) {
        class ComponentNotReady : CaptureError("Browser belum siap / belum ter-layout (lebar atau tinggi 0).")
        class RobotUnavailable(cause: Throwable) :
            CaptureError("Tidak bisa akses screen capture (java.awt.Robot): ${cause.message}")
    }

    /** Pipeline lengkap: screenshot komponen -> downscale -> JPEG bytes. */
    fun captureCompressed(component: Component, maxWidth: Int = 1280, quality: Float = 0.8f): Result<ByteArray> {
        val raw = captureComponent(component).getOrElse { return Result.failure(it) }
        val scaled = downscaleIfNeeded(raw, maxWidth)
        return Result.success(compressToJpeg(scaled, quality))
    }

    private fun captureComponent(component: Component): Result<BufferedImage> {
        if (component.width <= 0 || component.height <= 0) {
            return Result.failure(CaptureError.ComponentNotReady())
        }
        return try {
            val topLeft = component.locationOnScreen
            val bounds = Rectangle(topLeft.x, topLeft.y, component.width, component.height)
            val robot = Robot()
            Result.success(robot.createScreenCapture(bounds))
        } catch (e: Exception) {
            Result.failure(CaptureError.RobotUnavailable(e))
        }
    }

    private fun downscaleIfNeeded(image: BufferedImage, maxWidth: Int): BufferedImage {
        if (image.width <= maxWidth) return image
        val ratio = maxWidth.toDouble() / image.width
        val targetHeight = (image.height * ratio).toInt().coerceAtLeast(1)

        val scaled = BufferedImage(maxWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val g: Graphics2D = scaled.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(image, 0, 0, maxWidth, targetHeight, null)
        } finally {
            g.dispose()
        }
        return scaled
    }

    private fun compressToJpeg(image: RenderedImage, quality: Float): ByteArray {
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        val stream = ByteArrayOutputStream()

        if (!writers.hasNext()) {
            // Fallback tanpa kontrol quality kalau environment tidak punya JPEG writer
            // (jarang terjadi di JDK standar, tapi jangan sampai crash kalau iya).
            ImageIO.write(image, "jpg", stream)
            return stream.toByteArray()
        }

        val writer = writers.next()
        val params = writer.defaultWriteParam.apply {
            compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality
        }
        ImageIO.createImageOutputStream(stream).use { ios ->
            writer.output = ios
            writer.write(null, javax.imageio.IIOImage(image, null, null), params)
        }
        writer.dispose()
        return stream.toByteArray()
    }
}
