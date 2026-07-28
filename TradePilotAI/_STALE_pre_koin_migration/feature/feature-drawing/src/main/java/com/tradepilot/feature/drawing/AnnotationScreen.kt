package com.tradepilot.feature.drawing

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap

/** Menampilkan hasil akhir screenshot beranotasi (versi 5, poin akhir). */
@Composable
fun AnnotationScreen(annotatedBitmap: Bitmap?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        if (annotatedBitmap != null) {
            Image(bitmap = annotatedBitmap.asImageBitmap(), contentDescription = "Hasil analisa beranotasi")
        } else {
            Text("Belum ada hasil anotasi. Tekan ANALISA terlebih dahulu.")
        }
    }
}
