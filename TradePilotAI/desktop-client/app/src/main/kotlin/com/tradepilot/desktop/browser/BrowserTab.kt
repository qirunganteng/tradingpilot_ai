package com.tradepilot.desktop.browser

/**
 * Model 1 tab browser (Fase 10: multi-tab, jadi PARALEL sungguhan sejak
 * FASE 2 -- lihat TabbedBrowserHost.kt).
 *
 * FASE 2 -- UPDATE (menggantikan "BATASAN JUJUR" versi lama di sini yang
 * bilang "SATU JCEFBrowserEngine dipakai bergantian oleh semua tab"):
 * sekarang tiap tab BENAR-BENAR punya JCEFBrowserEngine (CefBrowser +
 * CefClient) sendiri-sendiri, persis Chrome sungguhan. Scroll position/
 * form yang belum disubmit/state JS di tab lain TIDAK lagi hilang saat
 * pindah tab -- semua tab yang terbuka tetap "hidup" penuh di background.
 * Trade-off yang SEKARANG berlaku (bukan lagi trade-off versi lama):
 * N tab terbuka = N instance browser penuh di RAM (belum ada tab
 * discarding/suspend untuk hemat memori -- kandidat optimisasi performa
 * ke depan, bukan blocker fungsional).
 *
 * Prioritas 11 (Multi Tab) menambah 2 field vs versi lama:
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
