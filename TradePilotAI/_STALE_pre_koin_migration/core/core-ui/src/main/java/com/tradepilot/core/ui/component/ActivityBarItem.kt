package com.tradepilot.core.ui.component

/**
 * Model item Activity Bar (sidebar ikon vertikal kiri, mirip VS Code).
 * Screen nyata (Browser, AI Analysis, Journal, dst) mendaftarkan diri
 * lewat model ini agar Activity Bar tetap generik & reusable.
 */
data class ActivityBarItem(
    val route: String,
    val iconContentDescriptionRes: Int,
    val labelRes: Int
)
