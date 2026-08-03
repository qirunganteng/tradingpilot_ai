package com.tradepilot.desktop.browser

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Browser Testing (item terakhir FASE 1 -- lihat docs/TESTING.md untuk
 * cakupan & batasan). HANYA menguji [JCEFBrowserEngine.normalizeUrl] --
 * satu-satunya bagian dari JCEFBrowserEngine yang murni fungsi (companion
 * object, tidak menyentuh CefBrowser/CefClient sama sekali), jadi bisa
 * dijalankan `./gradlew :app:test` biasa TANPA perlu native binary JCEF
 * ter-download/ter-load (yang butuh koneksi internet & konteks AWT/window
 * sungguhan -- tidak realistis untuk unit test murni).
 *
 * Sisa method JCEFBrowserEngine (loadUrl, goBack, clearBrowsingData, dst)
 * SEMUA butuh CefBrowser/CefClient nyata -- ini domain integration test
 * (butuh JCEF native ter-load, window sungguhan), BUKAN unit test, dan
 * TIDAK dikerjakan di sini (lihat checklist manual di docs/TESTING.md).
 */
class NormalizeUrlTest {

    // -- Sudah punya skema -> dikembalikan apa adanya --------------------

    @Test
    fun `https url dikembalikan apa adanya`() {
        assertEquals("https://google.com", JCEFBrowserEngine.normalizeUrl("https://google.com"))
    }

    @Test
    fun `http url dikembalikan apa adanya`() {
        assertEquals("http://example.com", JCEFBrowserEngine.normalizeUrl("http://example.com"))
    }

    @Test
    fun `about scheme dikembalikan apa adanya`() {
        assertEquals("about:blank", JCEFBrowserEngine.normalizeUrl("about:blank"))
    }

    @Test
    fun `data scheme dikembalikan apa adanya`() {
        assertEquals("data:text/plain,hello", JCEFBrowserEngine.normalizeUrl("data:text/plain,hello"))
    }

    @Test
    fun `skema custom (mis ftp) tetap dikembalikan apa adanya`() {
        assertEquals("ftp://files.example.com", JCEFBrowserEngine.normalizeUrl("ftp://files.example.com"))
    }

    // -- Terlihat seperti alamat web tanpa skema -> ditambah https:// ----

    @Test
    fun `domain polos ditambah https`() {
        assertEquals("https://google.com", JCEFBrowserEngine.normalizeUrl("google.com"))
    }

    @Test
    fun `subdomain ditambah https (kasus nyata Exness)`() {
        assertEquals("https://my.exness.com/webtrading", JCEFBrowserEngine.normalizeUrl("my.exness.com/webtrading"))
    }

    @Test
    fun `localhost ditambah https`() {
        assertEquals("https://localhost", JCEFBrowserEngine.normalizeUrl("localhost"))
    }

    @Test
    fun `localhost dengan port ditambah https`() {
        assertEquals("https://localhost:3000", JCEFBrowserEngine.normalizeUrl("localhost:3000"))
    }

    @Test
    fun `alamat IPv4 ditambah https`() {
        assertEquals("https://192.168.1.1", JCEFBrowserEngine.normalizeUrl("192.168.1.1"))
    }

    @Test
    fun `alamat IPv4 dengan port ditambah https`() {
        assertEquals("https://127.0.0.1:8080", JCEFBrowserEngine.normalizeUrl("127.0.0.1:8080"))
    }

    @Test
    fun `whitespace di pinggir di-trim sebelum diproses`() {
        assertEquals("https://google.com", JCEFBrowserEngine.normalizeUrl("  google.com  "))
    }

    // -- BUG YANG PERNAH ADA & sudah diperbaiki (lihat komentar di kode) --
    // Ketik kata biasa (bukan alamat) dulu jadi "https://kata" yang bukan
    // domain valid -> DNS_PROBE_FINISHED_NXDOMAIN. Sekarang harus diarahkan
    // ke pencarian Google, BUKAN dicoba jadi domain literal.

    @Test
    fun `kata tanpa titik diarahkan ke pencarian Google, bukan jadi domain literal`() {
        val result = JCEFBrowserEngine.normalizeUrl("google")
        assertEquals("https://www.google.com/search?q=google", result)
    }

    @Test
    fun `frasa dengan spasi diarahkan ke pencarian Google`() {
        val result = JCEFBrowserEngine.normalizeUrl("cara belajar trading forex")
        assertEquals(
            "https://www.google.com/search?q=cara+belajar+trading+forex",
            result
        )
    }

    @Test
    fun `query pencarian di-URL-encode dengan benar (karakter spesial)`() {
        val result = JCEFBrowserEngine.normalizeUrl("harga emas & perak?")
        assertEquals(
            "https://www.google.com/search?q=harga+emas+%26+perak%3F",
            result
        )
    }
}
