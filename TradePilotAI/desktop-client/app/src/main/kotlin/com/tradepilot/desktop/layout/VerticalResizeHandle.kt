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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors
import java.awt.Cursor

/**
 * Garis pemisah tipis yang bisa di-drag buat resize panel di sebelahnya
 * (kolom Explorer kiri & kolom AI Copilot kanan).
 *
 * FIX BUG #4 (dulu: garis terlalu tebal & warnanya kebiruan/terlalu
 * mencolok -- AppColors.BorderHover dipakai buat state hover, warnanya
 * accent biru bukan abu-abu netral; kursor mouse juga tidak berubah sama
 * sekali saat hover di atas handle, jadi tidak ada isyarat visual "ini bisa
 * di-drag"):
 *  - GARIS yang digambar sekarang cuma 1dp (dulu Dimens.RESIZE_HANDLE_WIDTH_DP
 *    penuh, 4dp, digambar sebagai warna solid) & warnanya abu-abu netral
 *    (AppColors.Border, sedikit lebih terang saat hover) -- BUKAN accent
 *    biru lagi.
 *  - AREA KLIK/DRAG tetap 8dp (lebih lebar dari garis yang kelihatan) supaya
 *    "tetap mudah digunakan" walau garis visualnya tipis -- pola umum resize
 *    handle di editor profesional (VSCode, JetBrains IDE, dst): hit-area
 *    selalu lebih lebar dari elemen visualnya.
 *  - Kursor mouse berubah jadi resize-horizontal (E_RESIZE) saat hover di
 *    atas handle lewat `pointerHoverIcon`, sebagai isyarat visual yang jelas.
 *
 * onDragDeltaPx: delta pixel mentah dari drag gesture (BUKAN dp) -- konversi
 * ke dp dilakukan di pemanggil (Workbench.kt) yang sudah punya akses ke
 * Density lewat LocalDensity, supaya file ini tidak perlu import density.
 */
private val HIT_AREA_WIDTH_DP = 8.dp
private val VISIBLE_LINE_WIDTH_DP = 1.dp
private val RESIZE_CURSOR = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))

@Composable
fun VerticalResizeHandle(
    onDragDeltaPx: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .width(HIT_AREA_WIDTH_DP)
            .fillMaxHeight()
            .pointerHoverIcon(RESIZE_CURSOR)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isHovered = true },
                    onDragEnd = { isHovered = false },
                    onDragCancel = { isHovered = false }
                ) { change, dragAmount ->
                    change.consume()
                    onDragDeltaPx(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(VISIBLE_LINE_WIDTH_DP)
                .fillMaxHeight()
                .background(if (isHovered) AppColors.TextSecondary else AppColors.Border)
        )
    }
}
