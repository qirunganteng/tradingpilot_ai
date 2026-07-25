package com.tradepilot.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Data numerik trading (harga, lot, RR) WAJIB pakai font monospace
 * agar digit sejajar dan mudah dibaca cepat — sesuai preferensi UI
 * "ala VS Code". Teks umum tetap pakai default sans-serif Material.
 */
val MonospaceFontFamily = FontFamily.Monospace

val TradePilotTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

/** Style khusus untuk angka trading: harga, lot, SL/TP, RR. */
val NumericDataStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp
)
