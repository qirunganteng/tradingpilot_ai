package com.tradepilot.desktop.theme

import androidx.compose.ui.graphics.Color

/**
 * Satu sumber warna untuk seluruh UI Browser (Prioritas 1 & 13: "jangan sampai
 * muncul area putih lagi", "consistent" di semua panel).
 *
 * Sebelum refactor ini, setiap file (Main.kt, BrowserBar.kt, BrowserTabsBar.kt,
 * VerticalResizeHandle.kt) punya angka Color(0xFF....) sendiri-sendiri yang
 * kebetulan mirip tapi tidak identik (mis. 0xFF1E1E1E vs 0xFF181818 dipakai
 * campur aduk untuk background yang seharusnya sama). Itu bikin title bar,
 * toolbar, dan browser content area kelihatan "beda warna dikit" alih-alih
 * menyatu mulus ala Chrome/VSCode.
 */
object AppColors {
    /** Warna dasar aplikasi -- dipakai title bar, ActivityBar, background utama. */
    val Base = Color(0xFF1E1E1E)

    /** Satu step lebih terang dari Base -- SideBar/Explorer, tab tidak aktif. */
    val Surface = Color(0xFF252526)

    /** Toolbar (BrowserBar) & tab aktif. */
    val SurfaceRaised = Color(0xFF2D2D2D)

    /** Address bar field, browser content placeholder sebelum JCEF siap. */
    val SurfaceSunken = Color(0xFF181818)

    val Border = Color(0xFF3A3A3A)
    val BorderHover = Color(0xFF0A84FF)

    val TextPrimary = Color(0xFFE8E8E8)
    val TextSecondary = Color(0xFF9A9A9A)
    val TextDisabled = Color(0xFF5A5A5A)

    val Accent = Color(0xFF4FC3F7)
    val Danger = Color(0xFFE06C75)
    val Success = Color(0xFF6FCF97)

    /** Warna tombol close di title bar saat hover (merah ala Windows/Chrome). */
    val CloseHover = Color(0xFFE81123)
}
