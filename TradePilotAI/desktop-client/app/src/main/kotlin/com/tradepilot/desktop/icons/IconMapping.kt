package com.tradepilot.desktop.icons

/**
 * CATATAN (bukan file kode fungsional): folder Icons/ ini di Android biasanya
 * berisi file .xml vector drawable. Project desktop-client TIDAK pakai itu --
 * semua icon di refactor ini datang dari `androidx.compose.material.icons.Icons`
 * (Material Icons Extended, sudah jadi dependency Compose Desktop bawaan),
 * jadi tidak ada file drawable yang perlu di-generate atau di-import manual.
 *
 * Daftar icon yang dipakai di seluruh refactor UI ini (biar gampang dicari
 * kalau mau ganti/audit konsistensi ke depan):
 *
 * Window/CustomTitleBar.kt   : Remove, CropSquare, FilterNone, Close
 * ActivityBar/ActivityBar.kt : Folder, History, Bookmark, Download,
 *                              Workspaces, SmartToy, Settings
 * Toolbar/BrowserBar.kt      : ArrowBack, ArrowForward, Refresh, Send,
 *                              Fullscreen, FullscreenExit, Menu
 * AddressBar/AddressBar.kt   : Bookmark, BookmarkBorder
 * TabBar/BrowserTabsBar.kt   : Add, Close, PushPin, VolumeOff
 * BrowserMenu/BrowserMenu.kt : Add, OpenInNew, VisibilityOff, History,
 *                              Bookmark, Download, Tab, Print, ZoomIn,
 *                              ZoomOut, YoutubeSearchedFor, Search, Code,
 *                              Settings, DeleteSweep, ExitToApp
 * BrowserPanel/JCEFBrowserView.kt : CloudOff, ErrorOutline, Refresh
 * Components/FindBar.kt      : KeyboardArrowUp, KeyboardArrowDown, Close
 *
 * Kalau kamu ingin identitas visual custom (bukan Material default) --
 * mis. logo TradePilot AI sendiri di title bar -- itu satu-satunya kasus
 * yang benar-benar butuh file gambar asli (svg/png), ditaruh sebagai
 * Painter/`painterResource` dari folder resources Compose Desktop (lihat
 * catatan di Resources/NOT_APPLICABLE.md).
 */
object IconMappingNotes
