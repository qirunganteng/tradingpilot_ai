package com.tradepilot.desktop.duplicate.tabbar

/**
 * Model 1 tab browser (Fase 10: multi-tab).
 *
 * BATASAN JUJUR: ini BUKAN tab Chrome sungguhan yang masing-masing punya
 * proses Chromium sendiri (itu berarti N tab = N instance JCEF penuh di
 * RAM -- berat & butuh refactor lifecycle JCEFBrowserEngine yang jauh
 * lebih besar). Implementasi v1 ini: SATU JCEFBrowserEngine dipakai
 * bergantian oleh semua tab -- pindah tab = engine.loadUrl(tab.url) ke URL
 * terakhir tab itu. Konsekuensinya: scroll position / form yang belum
 * disubmit / state JS di tab lain HILANG saat kamu pindah tab lalu balik
 * lagi (session login/cookies tetap aman karena itu disimpan per-domain
 * oleh browser, bukan per-tab). Kalau ke depan butuh tab yang benar-benar
 * paralel (mis. mantau beberapa chart real-time sekaligus), itu Fase 11
 * terpisah -- beri tahu saya kalau itu prioritasnya.
 *
 * Prioritas 11 (Multi Tab) menambah 2 field baru vs versi lama:
 *  - isPinned: tab pin tidak ikut kena "Close Other Tabs" & selalu tampil
 *    duluan di TabsBar (urutan pin di-enforce di BrowserTabsBar, bukan di
 *    model ini).
 *  - isMuted: CATATAN JUJUR -- CefBrowser (JCEF) tidak expose kontrol mute
 *    audio per-browser secara langsung di API publik yang dipakai
 *    JCEFBrowserEngine.kt versi ini, jadi isMuted di sini murni state UI
 *    (ikon berubah, tapi TIDAK benar-benar membisukan audio halaman).
 *    Ditandai jelas supaya tidak terkesan pura-pura berfungsi.
 */
data class BrowserTab(
    val id: String,
    var title: String,
    var url: String,
    var isPinned: Boolean = false,
    var isMuted: Boolean = false
)
