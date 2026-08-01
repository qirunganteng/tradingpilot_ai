package com.tradepilot.desktop.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens

/**
 * Garis pemisah tipis yang bisa di-drag buat resize panel di sebelahnya
 * (Fase 10 -- sebelumnya lebar SideBar & CopilotPanel di-hardcode, tidak
 * bisa digeser sama sekali).
 *
 * Prioritas 13 (UI Polish/consistent): warna & lebar sekarang dari
 * theme.AppColors / theme.Dimens, bukan Color(0xFF3A3A3A) & 6.dp hardcode
 * lokal seperti sebelumnya -- logic drag-nya sendiri sama persis, tidak
 * diubah.
 *
 * onDragDeltaPx: delta pixel mentah dari drag gesture (BUKAN dp) -- konversi
 * ke dp dilakukan di pemanggil (Main.kt) yang sudah punya akses ke Density
 * lewat LocalDensity, supaya file ini tidak perlu import density segala.
 */
@Composable
fun VerticalResizeHandle(
    onDragDeltaPx: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .width(Dimens.RESIZE_HANDLE_WIDTH_DP.dp)
            .fillMaxHeight()
            .background(if (isHovered) AppColors.BorderHover else AppColors.Border)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isHovered = true },
                    onDragEnd = { isHovered = false },
                    onDragCancel = { isHovered = false }
                ) { change, dragAmount ->
                    change.consume()
                    onDragDeltaPx(dragAmount.x)
                }
            }
    )
}
