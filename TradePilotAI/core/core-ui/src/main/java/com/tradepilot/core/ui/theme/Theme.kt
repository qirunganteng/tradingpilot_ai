package com.tradepilot.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Dark mode adalah DEFAULT (bukan opsional) — sesuai requirement versi 0 & 1.
 * Skema warna mengikuti palet "editor" di Color.kt.
 */
private val TradePilotDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = EditorTextPrimary,
    secondary = AccentPrimaryVariant,
    background = EditorBackground,
    onBackground = EditorTextPrimary,
    surface = EditorSurface,
    onSurface = EditorTextPrimary,
    surfaceVariant = EditorSurfaceVariant,
    error = SignalSell,
    outline = EditorBorder,
)

@Composable
fun TradePilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TradePilotDarkColorScheme,
        typography = TradePilotTypography,
        content = content
    )
}
