package com.tradepilot.feature.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

object CaptureUtil {

    @Composable
    fun ScreenshotOverlayText(
        text: String,
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier.padding(8.dp)) {
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Unspecified
                )
            }
        }
    }

    @Composable
    fun ScreenshotWatermark(
        symbol: String,
        timeframe: String,
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier.padding(8.dp)) {
            Column {
                Text(
                    text = "TradePilot AI • $symbol [$timeframe]",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }

    fun compressBitmapToByteArray(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(format, quality, stream)
        return stream.toByteArray()
    }
}
