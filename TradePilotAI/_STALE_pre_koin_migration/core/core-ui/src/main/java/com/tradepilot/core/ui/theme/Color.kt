package com.tradepilot.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet warna terinspirasi tema editor gelap (mis. VS Code Dark+).
 * Dipakai konsisten di seluruh app: Activity Bar, Bottom Panel, Status Bar.
 */

// Base surface — mirip editor background #1E1E1E
val EditorBackground = Color(0xFF1E1E1E)
val EditorSurface = Color(0xFF252526)      // sidebar / activity bar
val EditorSurfaceVariant = Color(0xFF2D2D30) // panel bawah (terminal-like)
val EditorBorder = Color(0xFF3C3C3C)

// Teks
val EditorTextPrimary = Color(0xFFD4D4D4)
val EditorTextSecondary = Color(0xFF9D9D9D)

// Aksen fungsional — konsisten dengan warna anotasi chart (versi 5)
val SignalBuy = Color(0xFF4EC9B0)      // hijau kebiruan khas VS Code
val SignalSell = Color(0xFFF14C4C)     // merah
val SignalWarning = Color(0xFFDCDCAA)  // kuning (liquidity / warning)
val SignalInfo = Color(0xFF569CD6)     // biru (order block / info)

// Accent utama (mirip warna "activity" biru VS Code)
val AccentPrimary = Color(0xFF007ACC)
val AccentPrimaryVariant = Color(0xFF005A9E)
