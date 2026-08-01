package com.tradepilot.desktop.theme

/**
 * Prioritas 4 (Compact Toolbar) & 5 (AI Panel): angka lama vs baru didaftar
 * eksplisit di sini supaya kelihatan jelas berapa persen pengurangannya, dan
 * supaya ToolBar/AddressBar/TabBar semua konsisten -- bukan tiap file
 * menentukan tinggi sendiri-sendiri (itu penyebab awal toolbar "terlalu
 * tinggi" dan tidak seragam dengan TabBar).
 */
object Dimens {
    // --- Title bar (baru, Prioritas 1) ---
    const val TITLE_BAR_HEIGHT_DP = 32

    // --- Tab bar ---
    // Lama: 36dp row height, tab width 168dp, padding horizontal 10dp.
    // Baru: -25% tinggi (36 -> 28), tab sedikit lebih ramping (168 -> 152).
    const val TAB_BAR_HEIGHT_DP = 28
    const val TAB_WIDTH_DP = 152
    const val TAB_PADDING_H_DP = 8

    // --- Toolbar / address bar ---
    // Lama: Row padding vertical 6dp (efektif tinggi toolbar ~52-56dp karena
    // OutlinedTextField default M3 tanpa height override ~56dp), quick-link
    // row padding vertical 4dp.
    // Baru: -25% -> padding vertical 4dp, address field dipaksa 32dp (bukan
    // 56dp bawaan M3) supaya benar-benar ramping ala Chrome, quick-link row
    // padding vertical 2dp.
    const val TOOLBAR_PADDING_H_DP = 8
    const val TOOLBAR_PADDING_V_DP = 4
    const val ADDRESS_FIELD_HEIGHT_DP = 32
    const val QUICK_LINK_ROW_PADDING_V_DP = 2
    const val QUICK_LINK_CHIP_HEIGHT_DP = 26

    // --- Activity bar ---
    const val ACTIVITY_BAR_WIDTH_DP = 48 // lama 56dp, -14% biar makin ramping

    // --- AI Panel (Prioritas 5) ---
    // CATATAN JUJUR: file CopilotPanel.kt (isi Balance/Risk/Entry/SL/TP) TIDAK
    // ada di paket file ini (lihat README root & README AIWorkspace/), jadi
    // angka di bawah ini adalah TARGET yang harus dipakai kalau kamu terapkan
    // langsung ke CopilotPanel.kt di project asli -- bukan sudah diterapkan
    // di sini.
    const val AI_PANEL_FIELD_HEIGHT_DP = 32 // lama biasanya 56dp default M3
    const val AI_PANEL_VERTICAL_SPACING_DP = 6 // lama biasanya 12-16dp
    const val AI_PANEL_BUTTON_HEIGHT_DP = 34

    const val RESIZE_HANDLE_WIDTH_DP = 4 // lama 6dp
}
