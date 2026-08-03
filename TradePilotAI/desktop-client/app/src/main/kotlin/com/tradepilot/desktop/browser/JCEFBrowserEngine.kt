package com.tradepilot.desktop.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tradepilot.domain.browser.BrowserEngine
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.Component

/**
 * Implementasi Desktop dari [BrowserEngine] — bungkus [CefBrowser] (JCEF),
 * pasangan dari `WebViewBrowserEngine` di android-client yang membungkus
 * `android.webkit.WebView`. Business logic yang sudah dipakai (BrowserToolbar
 * dkk kalau nanti di-share ke desktop) tidak perlu tahu bedanya.
 *
 * PENTING (browser umum, bukan cuma Exness/TradingView): engine ini TIDAK
 * membatasi domain apa pun -- `loadUrl` menerima URL apa saja (youtube.com,
 * github.com, shopee.co.id, facebook.com, dst) persis seperti browser biasa.
 * Yang tadinya terasa \"cuma bisa ke Exness\" murni karena UI (BrowserBar)
 * belum ada address bar -- sudah ditambah di Main.kt.
 *
 * Field *State di bawah reaktif ke Compose (dipakai BrowserBar) lewat
 * CefLoadHandler (canGoBack/canGoForward/isLoading -- lebih akurat dari
 * browser.canGoBack() yang bisa \"telat\" 1 frame) dan CefDisplayHandler
 * (address & title berubah, termasuk saat user klik link DI DALAM halaman,
 * bukan cuma lewat address bar kita).
 *
 * PERUBAHAN untuk Prioritas 10 & 12 (semua ADITIF -- tidak ada method lama
 * yang dihapus/diubah signature-nya):
 *  - find()/stopFind(): dari interface BrowserEngine (Ctrl+F, Prioritas 10),
 *    diteruskan langsung ke CefBrowser.find(...) (memang sudah tersedia di
 *    JCEF, cuma belum dipakai sebelumnya).
 *  - loadErrorState: diisi oleh CefLoadHandler.onLoadError (method yang
 *    memang sudah ada di CefLoadHandlerAdapter, cuma sebelumnya tidak
 *    di-override) -- dipakai JCEFBrowserView untuk Error Page/Offline Page
 *    (Prioritas 12). Otomatis dikosongkan lagi begitu onLoadingStateChange
 *    bilang loading dimulai (retry/navigasi baru).
 *  - loadingProgressState: CATATAN JUJUR -- CEF/JCEF TIDAK expose event
 *    persentase loading real (onLoadingProgressChange bukan bagian dari
 *    CefLoadHandler standar), jadi ini SIMULASI (naik bertahap selagi
 *    isLoadingState true, langsung ke 100% saat selesai) -- cukup untuk
 *    progress bar visual ala Chrome, TAPI bukan progress byte-per-byte asli.
 */
class JCEFBrowserEngine(
    cefClient: CefClient,
    startUrl: String,
    isIncognito: Boolean = false
) : BrowserEngine {

    /**
     * Disimpan sebagai field (bukan cuma dipakai lewat parameter) supaya
     * bisa di-dispose lewat [JCEFBootstrap.disposeClient] di [dispose] --
     * lihat catatan "BUG DITEMUKAN" di JCEFBootstrap.kt: sejak perbaikan itu,
     * setiap JCEFBrowserEngine adalah pemilik TUNGGAL client-nya sendiri
     * (tidak lagi dibagi ke window lain), jadi dia juga yang bertanggung
     * jawab membersihkannya.
     */
    private val ownedClient: CefClient = cefClient

    /**
     * Prioritas baru (New Incognito Window): kalau isIncognito=true, coba
     * buat browser dengan CefRequestContext TERPISAH lewat
     * `CefRequestContext.createContext(null)`.
     *
     * CATATAN JUJUR (DIPERBARUI setelah audit Cache Manager -- lihat
     * JCEFBootstrap.kt): API JCEF versi ini (146.0.10, sudah diverifikasi
     * lewat `javap`) HANYA punya SATU overload `createContext(handler)` --
     * TIDAK ADA cara mengirim `CefRequestContextSettings` custom (mis.
     * cache_path kosong = in-memory) per-context lewat Java binding ini.
     * Artinya context baru di sini kemungkinan besar TETAP mewarisi
     * `cache_path` GLOBAL yang baru di-set eksplisit di JCEFBootstrap.kt --
     * ISOLASI DISK CACHE untuk incognito TIDAK terjamin lewat jalur ini.
     * Cookie kemungkinan lebih terisolasi (request context terpisah = jar
     * cookie in-memory terpisah per context, ini bagian dari perilaku
     * default CEF), tapi cache HTML/gambar/script tetap berpotensi
     * tersimpan ke disk yang sama.
     *
     * Dibungkus try-catch persis seperti openDevTools() di bawah -- kalau
     * gagal, fallback ke context biasa (SAMA seperti window normal, TIDAK
     * terisolasi) daripada meng-crash seluruh window baru. Belum sempat
     * saya uji interaktif (tidak ada akses jalankan aplikasi Windows dari
     * sini) -- SEBELUM mengandalkan mode ini untuk kerahasiaan sungguhan,
     * WAJIB verifikasi manual (buka window incognito, browsing, tutup,
     * cek folder `~/.tradepilot/jcef-cache` apakah ada jejak baru).
     */
    val browser: CefBrowser = if (isIncognito) {
        try {
            val requestContext = org.cef.browser.CefRequestContext.createContext(null)
            cefClient.createBrowser(startUrl, false, false, requestContext)
        } catch (t: Throwable) {
            println("[JCEFBrowserEngine] Gagal buat request context incognito terisolasi, fallback ke context biasa (TIDAK terisolasi): ${t.message}")
            cefClient.createBrowser(startUrl, false, false)
        }
    } else {
        cefClient.createBrowser(startUrl, false, false)
    }
    val uiComponent: Component get() = browser.uiComponent

    var addressState by mutableStateOf(startUrl)
        private set
    var titleState by mutableStateOf("")
        private set
    var isLoadingState by mutableStateOf(false)
        private set
    var canGoBackState by mutableStateOf(false)
        private set
    var canGoForwardState by mutableStateOf(false)
        private set

    /** Prioritas 12: null = tidak ada error, terisi begitu load gagal. */
    var loadErrorState by mutableStateOf<LoadError?>(null)
        private set

    /** Prioritas 12: 0f..1f, simulasi (lihat catatan class di atas). */
    var loadingProgressState by mutableStateOf(0f)
        private set

    /**
     * Fullscreen -- HTML5 Fullscreen API level HALAMAN (video, tombol
     * fullscreen chart TradingView, dst -- `document.requestFullscreen()`
     * di JS), BEDA dengan fullscreen level APLIKASI (F11/AppFullscreenState,
     * murni state Compose kita, tidak tahu apa-apa soal isi halaman web).
     * Sebelum audit ini, JCEFBrowserEngine tidak tahu sama sekali kapan
     * halaman masuk/keluar HTML5 fullscreen -- dua state fullscreen ini
     * (aplikasi vs halaman) jalan sendiri-sendiri, tidak sinkron: video
     * fullscreen tidak otomatis menyembunyikan chrome aplikasi kita, dan
     * ESC di dalam halaman cuma keluar dari fullscreen HALAMAN (perilaku
     * default Chromium) TANPA ikut keluar dari fullscreen APLIKASI kalau
     * dua-duanya aktif bareng.
     */
    var isPageFullscreenState by mutableStateOf(false)
        private set

    /**
     * Dipasang dari Workbench.kt supaya HTML5 fullscreen (video/chart) ikut
     * men-toggle fullscreen APLIKASI (chrome disembunyikan) -- perilaku yang
     * sama seperti browser sungguhan (Chrome dkk juga otomatis fullscreen
     * seluruh window begitu video di dalamnya masuk fullscreen).
     */
    var onPageFullscreenChange: ((Boolean) -> Unit)? = null

    data class LoadError(
        val failedUrl: String,
        val errorText: String,
        val isOffline: Boolean
    )

    /**
     * Crash Recovery (item FASE 1 yang belum pernah diimplementasikan sama
     * sekali sebelum audit ini). CEF/JCEF TIDAK auto-restart render process
     * yang mati (crash, di-kill OOM oleh OS, dsb) -- browser akan diam
     * menampilkan halaman kosong/beku selamanya kalau tidak ditangani.
     *
     * Sengaja DIPAKAIKAN ULANG mekanisme error page yang SUDAH ADA
     * (loadErrorState -> BrowserErrorPage di JCEFBrowserView.kt) alih-alih
     * bikin UI baru -- FASE 1 melarang perubahan UI, dan crash render
     * process itu sendiri memang sejenis "gagal memuat halaman" dari sudut
     * pandang pengguna, jadi tombol "Coba lagi" yang sudah ada otomatis
     * berfungsi sebagai pemulihan (reload() membuat frame baru di render
     * process yang fresh).
     */
    /**
     * FASE 3 -- External Link & Browser Security. Ditambahkan ke
     * CefRequestHandlerAdapter yang SAMA dengan crash-recovery di atas
     * (BUKAN `cefClient.addRequestHandler(...)` kedua) -- lihat catatan
     * "BUG DITEMUKAN" panjang di JCEFBootstrap.kt: `addXxxHandler` di JCEF
     * HANYA menyimpan pemanggilan PERTAMA per client, jadi handler kedua
     * yang didaftarkan terpisah akan diam-diam DIABAIKAN sepenuhnya.
     *
     * `onBeforeBrowse` (External Link): skema selain http/https/about/data/
     * chrome-error (mis. `mailto:`, `tel:`, `market:`, deep-link aplikasi
     * lain) TIDAK bisa dan TIDAK SEHARUSNYA dicoba dimuat di dalam browser
     * ini -- diteruskan ke handler default OS (`java.awt.Desktop`) persis
     * seperti Chrome yang menanyakan/membuka aplikasi lain untuk skema yang
     * tidak dikenalnya. Return true = batalkan navigasi INI (sudah
     * ditangani di luar), false = lanjutkan seperti biasa.
     *
     * `onCertificateError` (Browser Security) -- DIHAPUS/DIBATALKAN: sempat
     * ditambahkan tapi GAGAL BUILD ("onCertificateError overrides nothing" +
     * "Unresolved reference 'CefSSLInfo'") -- artinya baik nama class
     * `org.cef.network.CefSSLInfo` maupun signature method yang saya tebak
     * TIDAK cocok dengan JCEF versi yang benar-benar dipakai proyek ini
     * (persis risiko yang sudah diperingatkan sebelumnya: method ini beda
     * dengan Download/Dialog Handler yang sempat diverifikasi `jar tf`/
     * `javap` langsung terhadap jar-nya). Daripada menebak lagi tanpa akses
     * ke jar sungguhan, ini SENGAJA tidak diimplementasikan -- kalau
     * dibutuhkan, perlu dikerjakan dari lingkungan yang bisa `javap` kelas
     * `org.cef.handler.CefRequestHandler` di jar JCEF proyek ini langsung
     * untuk dapat signature yang benar-benar cocok.
     */
    init {
        cefClient.addRequestHandler(object : org.cef.handler.CefRequestHandlerAdapter() {
            override fun onRenderProcessTerminated(
                browser: CefBrowser?,
                status: org.cef.handler.CefRequestHandler.TerminationStatus?,
                errorCode: Int,
                errorString: String?
            ) {
                if (browser !== this@JCEFBrowserEngine.browser) return
                isLoadingState = false
                loadErrorState = LoadError(
                    failedUrl = addressState,
                    errorText = "Halaman berhenti merespons (render process berakhir: " +
                        "${status?.name ?: "tidak diketahui"}${if (!errorString.isNullOrBlank()) " -- $errorString" else ""}).",
                    isOffline = false
                )
            }

            override fun onBeforeBrowse(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: org.cef.network.CefRequest?,
                user_gesture: Boolean,
                is_redirect: Boolean
            ): Boolean {
                if (browser !== this@JCEFBrowserEngine.browser) return false
                val url = request?.url ?: return false
                val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
                val isNormalWebScheme = scheme in setOf("http", "https", "about", "data", "file", "chrome-error", "chrome")
                if (isNormalWebScheme) return false
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                    }
                } catch (t: Throwable) {
                    println("[JCEFBrowserEngine] Gagal buka external link '$url' via OS: ${t.message}")
                }
                return true // batalkan navigasi internal -- sudah/coba ditangani via OS di atas
            }
        })
    }

    init {
        cefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onAddressChange(browser: CefBrowser?, frame: CefFrame?, url: String?) {
                if (browser === this@JCEFBrowserEngine.browser && url != null) {
                    addressState = url
                }
            }

            override fun onTitleChange(browser: CefBrowser?, title: String?) {
                if (browser === this@JCEFBrowserEngine.browser && title != null) {
                    titleState = title
                }
            }

            override fun onFullscreenModeChange(browser: CefBrowser?, fullscreen: Boolean) {
                if (browser !== this@JCEFBrowserEngine.browser) return
                isPageFullscreenState = fullscreen
                onPageFullscreenChange?.invoke(fullscreen)
            }
        })

        cefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                if (browser === this@JCEFBrowserEngine.browser) {
                    isLoadingState = isLoading
                    canGoBackState = canGoBack
                    canGoForwardState = canGoForward
                    if (isLoading) {
                        loadErrorState = null // navigasi baru/retry -> bersihkan error lama
                        loadingProgressState = 0.15f // simulasi: langsung kelihatan mulai jalan
                    } else {
                        loadingProgressState = 1f
                    }
                }
            }

            override fun onLoadError(
                browser: CefBrowser?,
                frame: CefFrame?,
                errorCode: org.cef.handler.CefLoadHandler.ErrorCode?,
                errorText: String?,
                failedUrl: String?
            ) {
                if (browser !== this@JCEFBrowserEngine.browser) return
                // ERR_ABORTED terjadi normal kalau user pindah halaman sebelum
                // load lama selesai -- BUKAN error sungguhan, jangan tampilkan
                // Error Page untuk kasus ini.
                if (errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_ABORTED) return
                val offline = errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_INTERNET_DISCONNECTED ||
                    errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_NAME_NOT_RESOLVED ||
                    errorCode == org.cef.handler.CefLoadHandler.ErrorCode.ERR_CONNECTION_TIMED_OUT
                loadErrorState = LoadError(
                    failedUrl = failedUrl ?: addressState,
                    errorText = errorText ?: errorCode?.name ?: "Gagal memuat halaman",
                    isOffline = offline
                )
            }
        })
    }

    /**
     * Browser Event -- `target="_blank"` / `window.open()`.
     *
     * FASE 1 (versi lama, SUDAH DIREVISI di FASE 2 -- lihat di bawah):
     * dulu SEMUA popup dibajak (`onBeforePopup` return `true`) supaya
     * navigasi di tab yang sama, karena waktu itu belum ada tab paralel
     * sungguhan.
     *
     * FASE 2 -- REVISI (item "Popup Window" di checklist Window Manager):
     * keputusan FASE 1 di atas ternyata BERISIKO NYATA untuk aplikasi
     * trading -- broker/OAuth login (Google/Facebook sign-in, verifikasi
     * 2FA, dst) UMUM memakai `window.open()` sungguhan, lalu popup itu
     * berkomunikasi balik ke halaman PEMBUKANYA lewat `window.opener`/
     * `postMessage`. Kalau popup dibajak jadi navigasi TAB YANG SAMA,
     * halaman pemicu (yang skrip-nya sedang menunggu popup itu) malah
     * ter-navigasi PERGI -- flow login/OAuth jadi rusak total, bukan cuma
     * kosmetik.
     *
     * CATATAN JUJUR (API): JCEF versi ini (146.0.10, sudah diverifikasi
     * lewat `javap`) TIDAK mengirim info "window features" (ukuran/posisi
     * yang diminta `window.open(url, name, features)`) lewat
     * `onBeforePopup` -- jadi TIDAK ADA cara di sini membedakan "link biasa
     * target=_blank" vs "popup OAuth sungguhan" secara pasti. Karena
     * salah pilih ke arah "selalu bajak" punya risiko lebih besar (merusak
     * login), default sekarang adalah: BIARKAN CEF/JCEF membuat popup
     * native sungguhan (return `false` = jangan batalkan -- JCEF windowed
     * mode sudah bisa bikin Frame popup sendiri otomatis TANPA butuh kode
     * tambahan apa pun di sini). Konsekuensinya: link `target="_blank"`
     * biasa (bukan OAuth) sekarang buka window kecil terpisah tanpa chrome
     * kita (bukan tab baru) -- trade-off yang disengaja, bukan bug.
     */
    init {
        cefClient.addLifeSpanHandler(object : org.cef.handler.CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser?,
                frame: CefFrame?,
                targetUrl: String?,
                targetFrameName: String?
            ): Boolean {
                // false = jangan batalkan -- biarkan JCEF bikin popup window
                // native sungguhan (lihat catatan di atas kenapa ini yang
                // lebih aman untuk app trading dibanding membajak ke tab).
                return false
            }
        })
    }

    /**
     * Focus Management (item FASE 1 yang belum pernah ditangani sama sekali
     * sebelum audit ini).
     *
     * BUG NYATA: seluruh shortcut keyboard (Ctrl+T, Ctrl+W, Ctrl+F, dst) di
     * KeyboardShortcuts.kt dipasang lewat `Modifier.onPreviewKeyEvent` di
     * root Compose (Workbench.kt) -- itu MURNI mekanisme Compose. Begitu
     * fokus OS pindah ke komponen native ini ([uiComponent], dibungkus
     * SwingPanel -- kondisi NORMAL setiap kali user mengetik/klik apa pun
     * DI DALAM halaman web, yaitu penggunaan paling umum aplikasi ini),
     * Compose sama sekali tidak kebagian key event lagi -- diserap duluan
     * oleh komponen native. Akibatnya: begitu user klik ke dalam halaman
     * (Exness/TradingView/dst), semua shortcut diam sampai user klik balik
     * ke chrome Compose (address bar/tab bar/dst).
     *
     * Fix: pakai hook resmi CEF (`CefKeyboardHandler.onKeyEvent`, dipanggil
     * dari pipeline input CEF sendiri -- BUKAN lewat AWT event queue, jadi
     * tidak kena masalah fokus native/Compose di atas sama sekali) alih-
     * alih workaround AWT KeyEventDispatcher yang jauh lebih rapuh (harus
     * tebak-tebakan lomba dengan routing native HWND CEF).
     *
     * SENGAJA cuma expose hook GENERIK [onNativeKeyDown] di sini (bukan
     * import BrowserShortcutActions dari package UI) -- engine ini tidak
     * boleh tahu apa-apa soal aksi tab/fullscreen/dst, itu melanggar
     * pemisahan layer (Browser Engine vs UI). Pemetaan windows_key_code ->
     * aksi sungguhan ada di `handleBrowserShortcutsNative` (components/
     * KeyboardShortcuts.kt), disambungkan dari Workbench.kt.
     */
    var onNativeKeyDown: ((windowsKeyCode: Int, isCtrl: Boolean, isShift: Boolean, isAlt: Boolean) -> Boolean)? = null

    init {
        cefClient.addKeyboardHandler(object : org.cef.handler.CefKeyboardHandlerAdapter() {
            override fun onKeyEvent(
                browser: CefBrowser?,
                event: org.cef.handler.CefKeyboardHandler.CefKeyEvent?
            ): Boolean {
                if (browser !== this@JCEFBrowserEngine.browser || event == null) return false
                if (event.type != org.cef.handler.CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_KEYDOWN) return false
                val ctrl = (event.modifiers and org.cef.misc.EventFlags.EVENTFLAG_CONTROL_DOWN) != 0
                val shift = (event.modifiers and org.cef.misc.EventFlags.EVENTFLAG_SHIFT_DOWN) != 0
                val alt = (event.modifiers and org.cef.misc.EventFlags.EVENTFLAG_ALT_DOWN) != 0
                return onNativeKeyDown?.invoke(event.windows_key_code, ctrl, shift, alt) ?: false
            }
        })
    }

    /**
     * JavaScript Bridge (item FASE 1 terakhir yang belum ditangani sama
     * sekali sebelum audit ini). SENGAJA cuma infrastruktur/pipa di sini --
     * TIDAK ada logic AI/Trading apa pun ditulis di sini (itu tetap FASE 5/
     * FASE 6, lihat rule "Jangan mengerjakan AI"/"Jangan membuat fitur
     * Trading" untuk FASE 1). `JCEFBrowserEngine` cuma expose:
     *  - [evaluateJavaScript]: arah Kotlin -> JS (sudah dipakai internal di
     *    [exitPageFullscreen], sekarang dipublikasikan supaya reusable).
     *  - [onJsBridgeQuery]: arah JS -> Kotlin, lewat `CefMessageRouter`
     *    (mekanisme resmi CEF untuk ini -- expose fungsi JS `tradePilotQuery(
     *    {request, onSuccess, onFailure})` otomatis ke SETIAP halaman yang
     *    dimuat, diteruskan ke handler Kotlin di sisi sini). Nama fungsi JS
     *    SENGAJA di-custom (bukan default `cefQuery`) untuk mengurangi risiko
     *    bentrok dengan variabel/fungsi yang sudah dipakai halaman pihak
     *    ketiga (TradingView dkk).
     *
     * CATATAN JUJUR: belum sempat diuji interaktif dari sini (tidak ada
     * akses jalankan aplikasi Windows). `CefMessageRouter` adalah mekanisme
     * RESMI JCEF untuk kasus ini (bukan workaround), tapi tolong verifikasi
     * manual sebelum dipakai fitur sungguhan -- contoh test dari DevTools
     * console halaman mana pun: `window.tradePilotQuery({request: "ping",
     * onSuccess: r => console.log(r), onFailure: (c,m) => console.error(c,m)})`
     * lalu pasang `engine.onJsBridgeQuery = { req, cb -> cb.success("pong: $req") }`
     * dari sisi Kotlin, harusnya console.log menampilkan "pong: ping".
     */
    interface JsBridgeCallback {
        fun success(response: String)
        fun failure(errorCode: Int, errorMessage: String)
    }

    var onJsBridgeQuery: ((request: String, callback: JsBridgeCallback) -> Unit)? = null

    private val messageRouter: org.cef.browser.CefMessageRouter = org.cef.browser.CefMessageRouter.create(
        org.cef.browser.CefMessageRouter.CefMessageRouterConfig("tradePilotQuery", "tradePilotQueryCancel")
    ).also { router ->
        router.addHandler(object : org.cef.handler.CefMessageRouterHandlerAdapter() {
            override fun onQuery(
                browser: CefBrowser?,
                frame: CefFrame?,
                queryId: Long,
                request: String?,
                persistent: Boolean,
                callback: org.cef.callback.CefQueryCallback?
            ): Boolean {
                if (browser !== this@JCEFBrowserEngine.browser || callback == null) return false
                val handler = onJsBridgeQuery
                if (handler == null) {
                    // Tidak ada fitur yang pasang handler (normal untuk FASE 1 --
                    // belum ada pemakai) -- gagalkan dengan pesan jelas alih-alih
                    // diam/hang selamanya di sisi JS pemanggil.
                    callback.failure(0, "TradePilot JS Bridge: belum ada handler terpasang.")
                    return true
                }
                handler(request ?: "", object : JsBridgeCallback {
                    override fun success(response: String) = callback.success(response)
                    override fun failure(errorCode: Int, errorMessage: String) = callback.failure(errorCode, errorMessage)
                })
                return true
            }
        }, true)
        cefClient.addMessageRouter(router)
    }

    /** Arah Kotlin -> JS, generik (dipakai internal oleh [exitPageFullscreen], juga reusable untuk kebutuhan lain ke depan). */
    fun evaluateJavaScript(script: String) {
        try {
            browser.executeJavaScript(script, browser.url ?: "", 0)
        } catch (t: Throwable) {
            println("[JCEFBrowserEngine] Gagal evaluateJavaScript: ${t.message}")
        }
    }

    /**
     * FASE 3 -- Download Manager. Sebelum ini `CefDownloadHandler` sama
     * sekali tidak terpasang (dicatat jujur di ExplorerModels.kt/
     * ExplorerPanel.kt lama) -- artinya link download di halaman mana pun
     * TIDAK melakukan apa-apa yang terlihat (CEF butuh handler eksplisit
     * untuk mulai proses unduhan sama sekali, beda dengan link biasa).
     *
     * `onBeforeDownload`: tentukan lokasi simpan = folder Downloads OS user
     * (`~/Downloads`, dibuat kalau belum ada) + nama file yang disarankan
     * situs, dengan dedup otomatis ("file (1).pdf", "file (2).pdf", dst)
     * kalau nama sudah dipakai -- persis kebiasaan Chrome. `showDialog=false`
     * di `Continue()` = langsung unduh diam-diam ke folder itu (default
     * Chrome modern), BUKAN selalu munculkan dialog "Save As" (yang juga
     * merupakan opsi valid, tapi lebih mengganggu untuk workflow trading
     * yang mungkin unduh statement/laporan berkali-kali).
     *
     * `onDownloadUpdated`: progress berjalan, ditulis ke [DownloadStore]
     * yang sudah disambungkan ke panel Downloads (ExplorerPanel.kt).
     */
    init {
        cefClient.addDownloadHandler(object : org.cef.handler.CefDownloadHandlerAdapter() {
            // CATATAN: build gagal sebelumnya karena method ini di versi JCEF
            // proyek ini (jcefmaven) ternyata return Boolean, BEDA dari
            // dokumentasi JCEF upstream (JetBrains/jcef master) yang bilang
            // void -- konfirmasi ini datang LANGSUNG dari pesan error compiler
            // Gradle, sumber paling akurat yang ada. `true` di semua jalur di
            // bawah = "sudah saya tangani sendiri" (konsisten dengan pola
            // return Boolean di onBeforeBrowse/onBeforePopup pada file ini).
            override fun onBeforeDownload(
                browser: CefBrowser?,
                downloadItem: org.cef.callback.CefDownloadItem?,
                suggestedName: String?,
                callback: org.cef.callback.CefBeforeDownloadCallback?
            ): Boolean {
                if (browser !== this@JCEFBrowserEngine.browser || downloadItem == null || callback == null) return true
                try {
                    val downloadsDir = java.io.File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
                    val rawName = suggestedName?.takeIf { it.isNotBlank() } ?: "unduhan"
                    val dotIndex = rawName.lastIndexOf('.')
                    val baseName = if (dotIndex > 0) rawName.substring(0, dotIndex) else rawName
                    val extension = if (dotIndex > 0) rawName.substring(dotIndex) else ""
                    var candidate = java.io.File(downloadsDir, rawName)
                    var counter = 1
                    while (candidate.exists()) {
                        candidate = java.io.File(downloadsDir, "$baseName ($counter)$extension")
                        counter++
                    }
                    com.tradepilot.desktop.explorer.DownloadStore.start(
                        id = downloadItem.id,
                        fileName = candidate.name,
                        url = downloadItem.url ?: "",
                        fullPath = candidate.absolutePath
                    )
                    callback.Continue(candidate.absolutePath, false)
                } catch (t: Throwable) {
                    println("[JCEFBrowserEngine] Gagal mulai unduhan: ${t.message}")
                }
                return true
            }

            override fun onDownloadUpdated(
                browser: CefBrowser?,
                downloadItem: org.cef.callback.CefDownloadItem?,
                callback: org.cef.callback.CefDownloadItemCallback?
            ) {
                if (browser !== this@JCEFBrowserEngine.browser || downloadItem == null) return
                com.tradepilot.desktop.explorer.DownloadStore.update(
                    id = downloadItem.id,
                    progressPercent = downloadItem.percentComplete,
                    isComplete = downloadItem.isComplete,
                    isCanceled = downloadItem.isCanceled
                )
            }
        })
    }

    /**
     * FASE 3 -- Upload (`<input type="file">`). Sebelum ini `CefDialogHandler`
     * sama sekali tidak terpasang -- CATATAN JUJUR: belum sempat diverifikasi
     * interaktif dari sini apakah CEF versi ini punya fallback dialog native
     * bawaan tanpa handler (di beberapa versi CEF ada, di versi lain tidak);
     * yang pasti dengan handler eksplisit di sini, perilakunya PASTI (bukan
     * bergantung fallback yang tidak terverifikasi) -- pakai `JFileChooser`
     * Swing biasa (konsisten dengan SwingPanel yang sudah dipakai membungkus
     * browser ini) dijalankan di Swing EDT lewat `invokeLater` (wajib --
     * callback ini dipanggil dari thread CEF, BUKAN Swing EDT, dan
     * JFileChooser HARUS dibuka dari EDT).
     */
    init {
        cefClient.addDialogHandler(object : org.cef.handler.CefDialogHandler {
            override fun onFileDialog(
                browser: CefBrowser?,
                mode: org.cef.handler.CefDialogHandler.FileDialogMode?,
                title: String?,
                defaultFilePath: String?,
                acceptFilters: java.util.Vector<String>?,
                acceptExtensions: java.util.Vector<String>?,
                acceptDescriptions: java.util.Vector<String>?,
                callback: org.cef.callback.CefFileDialogCallback?
            ): Boolean {
                if (browser !== this@JCEFBrowserEngine.browser || callback == null) return false
                javax.swing.SwingUtilities.invokeLater {
                    try {
                        val chooser = javax.swing.JFileChooser(defaultFilePath?.takeIf { it.isNotBlank() })
                        chooser.dialogTitle = title?.takeIf { it.isNotBlank() } ?: "Pilih file"
                        chooser.isMultiSelectionEnabled =
                            mode == org.cef.handler.CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN_MULTIPLE
                        chooser.fileSelectionMode = if (mode == org.cef.handler.CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN_FOLDER) {
                            javax.swing.JFileChooser.DIRECTORIES_ONLY
                        } else {
                            javax.swing.JFileChooser.FILES_ONLY
                        }
                        val result = if (mode == org.cef.handler.CefDialogHandler.FileDialogMode.FILE_DIALOG_SAVE) {
                            chooser.showSaveDialog(uiComponent as? java.awt.Component)
                        } else {
                            chooser.showOpenDialog(uiComponent as? java.awt.Component)
                        }
                        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                            val paths = if (chooser.isMultiSelectionEnabled) {
                                chooser.selectedFiles.map { it.absolutePath }
                            } else {
                                listOf(chooser.selectedFile.absolutePath)
                            }
                            callback.Continue(java.util.Vector(paths))
                        } else {
                            callback.Cancel()
                        }
                    } catch (t: Throwable) {
                        println("[JCEFBrowserEngine] Gagal buka dialog file: ${t.message}")
                        callback.Cancel()
                    }
                }
                return true // kita yang menangani -- CEF jangan pakai fallback-nya sendiri.
            }
        })
    }

    override val currentUrl: String
        get() = browser.url ?: ""

    override fun loadUrl(url: String) {
        browser.loadURL(normalizeUrl(url))
    }

    override fun goBack() {
        if (browser.canGoBack()) browser.goBack()
    }

    override fun goForward() {
        if (browser.canGoForward()) browser.goForward()
    }

    override fun reload() {
        loadErrorState = null
        browser.reload()
    }

    override fun canGoBack(): Boolean = browser.canGoBack()

    override fun canGoForward(): Boolean = browser.canGoForward()

    override fun find(text: String, forward: Boolean) {
        if (text.isBlank()) return
        // matchCase=false, findNext=true (biar Enter berulang lompat ke hasil
        // berikutnya alih-alih selalu mulai dari awal lagi).
        browser.find(text, forward, false, true)
    }

    override fun stopFind() {
        browser.stopFinding(true)
    }

    /**
     * Prioritas 6 (Browser Menu -- Zoom): CefBrowser memang expose zoom
     * level asli (bukan simulasi), beda dengan progress bar di atas.
     * +/- 0.5 per langkah kira-kira sama dengan Ctrl+Scroll di Chrome.
     */
    fun zoomIn() {
        browser.zoomLevel = (browser.zoomLevel + 0.5).coerceAtMost(5.0)
    }

    fun zoomOut() {
        browser.zoomLevel = (browser.zoomLevel - 0.5).coerceAtLeast(-5.0)
    }

    fun resetZoom() {
        browser.zoomLevel = 0.0
    }

    /**
     * Shortcut ESC "keluar dari fullscreen browser DAN fullscreen aplikasi"
     * sekaligus (permintaan eksplisit) -- dipanggil dari `exitFullscreen`
     * action di Workbench.kt bersamaan dengan menutup fullscreen aplikasi.
     *
     * CATATAN JUJUR: CEF versi ini (lihat audit CefBrowser API di atas)
     * TIDAK expose `exitFullscreen()` native di level CefBrowser/Host --
     * jadi ini lewat `document.exitFullscreen()` (DOM Fullscreen API
     * standar) via JS, best-effort/try-catch. Aman dipanggil kapan pun
     * (termasuk saat halaman TIDAK sedang fullscreen -- `exitFullscreen()`
     * pada dokumen yang tidak fullscreen memang no-op menurut spec, tidak
     * melempar error yang terlihat pengguna).
     */
    fun exitPageFullscreen() {
        try {
            browser.executeJavaScript(
                "if (document.fullscreenElement) { document.exitFullscreen(); }",
                browser.url ?: "",
                0
            )
        } catch (t: Throwable) {
            println("[JCEFBrowserEngine] Gagal exitFullscreen() halaman: ${t.message}")
        }
    }

    /**
     * Prioritas 6 -- Developer Tools. `openDevTools()` tersedia di CefBrowser
     * pada versi JCEF yang umum dipakai, TAPI ini best-effort: kalau build
     * JCEF kamu berbeda dan method-nya tidak ada / melempar exception saat
     * dipanggil, ini gagal diam-diam (di-log) daripada meng-crash seluruh
     * aplikasi -- silakan cek Logcat/console kalau tombol DevTools di menu
     * kelihatan tidak bereaksi.
     */
    fun openDevTools() {
        try {
            browser.openDevTools()
        } catch (t: Throwable) {
            println("[JCEFBrowserEngine] DevTools tidak tersedia di build JCEF ini: ${t.message}")
        }
    }

    /**
     * Browser Menu -- "Print": CefBrowser.print() membuka dialog print
     * native OS untuk halaman yang sedang aktif. Best-effort/try-catch
     * sama seperti openDevTools() -- tergantung dukungan versi JCEF.
     */
    fun print() {
        try {
            browser.print()
        } catch (t: Throwable) {
            println("[JCEFBrowserEngine] Print tidak tersedia di build JCEF ini: ${t.message}")
        }
    }

    /**
     * Browser Menu -- "Clear Browsing Data": hapus SEMUA cookie lewat
     * CefCookieManager global (mempengaruhi semua tab/window yang berbagi
     * CefClient yang sama, bukan cuma tab ini -- itu memang perilaku wajar
     * "Clear Browsing Data" di browser sungguhan). History lokal (HistoryStore)
     * dibersihkan terpisah di Workbench.kt (data itu murni disimpan app kita,
     * bukan lewat CEF). Reload halaman aktif di akhir supaya efeknya (mis.
     * logout dari situs yang session-nya berbasis cookie) langsung kelihatan.
     *
     * GAP DIKETAHUI (Cache Manager, audit FASE 1): method ini HANYA hapus
     * cookie, TIDAK hapus disk cache (HTML/gambar/script yang di-cache CEF
     * di `~/.tradepilot/jcef-cache`, lihat JCEFBootstrap.kt) -- meski nama
     * tombolnya "Clear Browsing Data" yang biasanya di ekspektasi user juga
     * membersihkan cache. Ini BUKAN kelalaian yang dibiarkan diam-diam:
     * sudah diverifikasi lewat `jar tf` bahwa JCEF versi ini (146.0.10)
     * TIDAK expose satu pun API untuk clear HTTP cache secara terprogram
     * (tidak ada class "CefCache" atau sejenisnya di jar API-nya sama
     * sekali -- beda dengan cookie yang punya CefCookieManager). Kalau
     * benar-benar dibutuhkan ke depan, satu-satunya jalan yang kelihatan
     * dari sini adalah hapus manual folder `cache_path` di disk saat SEMUA
     * browser/client sedang tertutup (berisiko kalau CEF masih pegang file
     * handle -- belum aman diimplementasikan tanpa pengujian interaktif).
     */
    fun clearBrowsingData() {
        try {
            org.cef.network.CefCookieManager.getGlobalManager()?.deleteCookies(null, null)
        } catch (t: Throwable) {
            println("[JCEFBrowserEngine] Gagal hapus cookies: ${t.message}")
        }
        reload()
    }

    /**
     * Mitigasi bug #10 ("klik maximize, halaman browser jadi putih kosong") --
     * SwingPanel yang membungkus komponen native JCEF kadang tidak dapat
     * notifikasi resize yang benar dari OS saat window di-maximize/restore
     * lewat WindowPlacement. Dipanggil dari JCEFBrowserView.kt setiap kali
     * window placement/size berubah (lihat window/AppFullscreenState.kt --
     * LocalAppWindowState) untuk memaksa komponen browser revalidate +
     * repaint dirinya sendiri.
     */
    fun notifyResized() {
        val component = uiComponent
        component.invalidate()
        component.validate()
        component.repaint()
    }

    /**
     * Panggil saat Composable-nya dibuang dari komposisi (lihat Main.kt).
     * Selain menutup browser-nya sendiri, sejak perbaikan client-per-engine
     * di JCEFBootstrap (lihat catatan class-nya), engine ini JUGA pemilik
     * tunggal [ownedClient] -- jadi wajib ikut dibersihkan di sini, kalau
     * tidak client (dan handler-handler di atas) menumpuk terus setiap kali
     * user buka lalu tutup window/incognito window dalam satu sesi aplikasi.
     */
    fun dispose() {
        ownedClient.removeMessageRouter(messageRouter)
        messageRouter.dispose()
        browser.close(true)
        JCEFBootstrap.disposeClient(ownedClient)
    }

    companion object {
        /**
         * Bug baru ("browser cuma bisa dipakai buat web yang sudah ada
         * shortcut, selain itu nggak bisa"): root cause-nya fungsi ini dulu
         * HANYA nambahin "https://" di depan input apa pun tanpa skema --
         * jadi ngetik "google" jadi "https://google" (bukan domain valid) ->
         * DNS_PROBE_FINISHED_NXDOMAIN. Shortcut (Exness/TradingView/dst)
         * kebetulan selalu kirim URL LENGKAP jadi tidak kena masalah ini.
         *
         * Fix: tiru heuristik omnibox Chrome -- kalau teksnya TIDAK terlihat
         * seperti alamat web (tidak ada titik, bukan localhost, bukan IP),
         * anggap itu QUERY PENCARIAN dan arahkan ke Google Search, bukan
         * dicoba jadi domain literal.
         */
        private fun looksLikeWebAddress(input: String): Boolean {
            if (input.contains(" ")) return false
            if (input.startsWith("localhost")) return true
            if (Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?$").matches(input)) return true // IPv4 (+port opsional)
            return input.contains(".") // domain.tld
        }

        fun normalizeUrl(input: String): String {
            val trimmed = input.trim()
            val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed) ||
                trimmed.startsWith("about:") || trimmed.startsWith("data:")
            if (hasScheme) return trimmed

            return if (looksLikeWebAddress(trimmed)) {
                "https://$trimmed"
            } else {
                "https://www.google.com/search?q=" +
                    java.net.URLEncoder.encode(trimmed, "UTF-8")
            }
        }
    }
}
