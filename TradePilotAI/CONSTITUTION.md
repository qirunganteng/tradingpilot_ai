==========================================================
TRADEPILOT AI PLATFORM CONSTITUTION
==========================================================

STATUS

Dokumen ini adalah KONSTITUSI PROJECT.

Seluruh Blueprint, Prompt, Roadmap,
Task, Feature, Module,
dan Source Code

WAJIB mengikuti dokumen ini.

Jika terdapat konflik,

maka dokumen ini yang menjadi acuan utama.

==========================================================

PROJECT NAME

TradePilot AI Platform

Client Utama:

TradePilot AI Super-App & AI Browser (Flutter)

==========================================================

VISION

TradePilot bukan sekadar aplikasi mobile.

TradePilot bukan browser biasa.

TradePilot adalah Super-App, Native Cross-Platform Browser, 
& AI Trading Workspace Lintas Platform.

Android hanyalah salah satu Target Primary Mobile.

Windows Desktop hanyalah salah satu Target Primary Desktop.

iOS & Desktop Platform lainnya adalah Target Paralel.

Semua Target memiliki kapabilitas dan konsistensi 
pengalaman pengguna yang sama.

==========================================================

PROJECT GOAL

Membangun AI Trading Browser & Workspace profesional
yang berjalan di berbagai platform secara responsif,
modular, dan berkinerja tinggi.

Target Platform:
- Windows Desktop (Primary Desktop)
- Android (Primary Mobile)
- iOS
- macOS (future)
- Linux (future)

==========================================================

CORE PRINCIPLE

Single Codebase.

Feature-First Modular Architecture.

Shared Business Logic & UI Components.

Backend First (Cloudflare Stack).

Thin & Reactive Client.

==========================================================

ARCHITECTURE & TECH STACK

TradePilot Frontend dibangun menggunakan FLUTTER.

Core Framework & Libraries:
- Framework: Flutter SDK
- State Management: Flutter Riverpod
- HTTP Client: Dio
- WebView Engine: flutter_inappwebview
- Window Management: window_manager
- Split Layout Engine: multi_split_view
- Data Grid Engine: PlutoGrid

==========================================================

MODULAR STRUCTURE & MASTER FEATURE MAPPING (FEATURE-FIRST)

Arsitektur aplikasi WAJIB menggunakan pendekatan Feature-First.
Setiap fitur dieksekusi berdasarkan skala prioritas (★1 - ★5).

1. BROWSER CORE & WORKSPACE ENGINE MODULE
   - Multi Tab (★★★★★) & Vertical Tabs (★★★★★)[cite: 2]
   - Tab Groups (★★★★★) & Workspace Management (★★★★★)[cite: 2]
   - Split View Layout (★★★★★) & Session Manager (★★★★★)[cite: 2]
   - Bookmarks (★★★★☆) & History (★★★★★)[cite: 2]
   - Downloads (★★★★☆) & Fullscreen Mode (★★★★☆)[cite: 2]
   - Incognito Mode (★★★★☆) & Security (Password & Cookie Manager) (★★★★☆)[cite: 2]
   - Permissions & Native Media Utilities (PDF Viewer, Screenshot, Screen Recorder) (★★★★☆)[cite: 2]
   - Workspace & Data Sync (★★★★☆)[cite: 2]
   - Secure Broker Login (★★★★★) & Multi Broker Workspace (★★★★★)
     [lihat SECURITY & PRIVACY -> Trading-Specific Security]

2. AI PILOT & ANALYTICS MODULE
   - AI Chat (★★★★★) & Screen Understanding (★★★★☆)[cite: 2]
   - Chart Analysis (★★★★★) & Pattern Detection (★★★★★)[cite: 2]
   - Market Scanner (★★★★★) & Market Sentiment Engine (★★★★★)[cite: 2]
   - Pair Recommendation (★★★★★) & Risk Analysis (★★★★★)[cite: 2]
   - Smart News (★★★★★) & Economic Calendar AI (★★★★★)[cite: 2]
   - Trading Journal AI & Trade Review (★★★★☆)[cite: 2]
   - AI Coach (★★★★☆), AI Memory (★★★☆☆), & Voice Assistant (★★★★☆)[cite: 2]
   - AI Watchlist (★★★☆☆)[cite: 2]
   - Chart Snapshot AI Analysis, capture chart aktif untuk dianalisis
     AI (★★★★★)
   - Browser-AI Context Awareness, AI memahami konten/tab yang
     sedang aktif tanpa user copy-paste manual (★★★★★)

3. TRADING TERMINAL & CALCULATORS MODULE
   - Dashboard (★★★★★) & Watchlist (★★★★★)[cite: 2]
   - Price Alerts (★★★★★) & News Alerts (★★★★★)[cite: 2]
   - Journal (★★★★★) & Performance Analytics (★★★★★)[cite: 2]
   - Trade Replay (★★★★☆) & Strategy Notes (★★★★☆)[cite: 2]
   - Trading Calculators Suite:
     * Lot Size (★★★★★)[cite: 2]
     * Risk/Reward (★★★★★)[cite: 2]
     * Pip Calculator (★★★★★)[cite: 2]
     * Margin Calculator (★★★★★)[cite: 2]
     * Profit Calculator (★★★★★)[cite: 2]
     * Drawdown Calculator (★★★★★)[cite: 2]
     * Compounding Calculator (★★★★☆)[cite: 2]
     * Swap Calculator (★★★★☆)[cite: 2]
     * Currency Converter (★★★☆☆)[cite: 2]

4. SOCIAL, COMMUNITY & LEARNING MODULE
   - Trader Chat Rooms (★★★★★) & Chart Sharing (★★★★★)[cite: 2]
   - Mentor System (★★★★★) & Leaderboard (★★★★★)[cite: 2]
   - Workspace Sharing (★★★★★)[cite: 2]
   - Learning Academy Suite (Beginner ★★★★★, Intermediate ★★★★★, Advanced ★★★★★)[cite: 2]
   - Paper Trading Simulator & Relaxation Player Service (Lofi)

5. CORE INFRASTRUCTURE MODULE
   - Centralized Network Services (Dio + Interceptors)
   - Global State Providers (Riverpod)
   - Theme System (Modern Dark Trading Style)
   - Cross-Platform Hardware Abstractions

==========================================================

CLIENT RESPONSIBILITY

Flutter Client TIDAK BOLEH melakukan pemrosesan berat AI.

Tugas Flutter Client:
- Rendering UI secara reaktif & responsif
- Managing Local Application State via Riverpod
- Rendering Multi-Tab WebView (TradingView Chart & Browsing)
- Audio/Media Streaming handling (Lofi Radio)
- Capture Screenshot & Media Recording
- Receiving & Rendering AI Streaming Responses
- Managing Window Decorations (Titlebar, Tray, Split Panels)

==========================================================

BACKEND STACK

Semua Pemrosesan AI, Synchronization, dan Central Data Storage 
WAJIB berjalan di Cloudflare Stack.

Backend Component:
- Cloudflare Workers (API Gateway, Auth & Serverless Logic)
- Cloudflare AI Gateway (LLM Router: Gemini, OpenAI, Claude, DeepSeek, Qwen)
- Cloudflare D1 (Relational Database: User, History, Session, Bookmarks)
- Cloudflare R2 (Object Storage: Screenshots, Media, Workspace Exports)

==========================================================

PROGRAMMING LANGUAGE & UI FRAMEWORK

Language:
- Dart (Pure Dart + Flutter SDK)

UI Framework:
- Flutter Framework (Multi-platform Native Engine)

==========================================================

WEBVIEW ENGINE

Penyajian chart, browser core, dan web content HARUS dipisah 
melalui abstraksi WebView yang fleksibel dan stabil.

Implementation:
- Windows Desktop -> flutter_inappwebview (Edge Chromium Engine)
- Android -> flutter_inappwebview (Android System WebView)
- iOS / macOS -> flutter_inappwebview (WKWebView)

==========================================================

BROWSER ENGINE CORE CAPABILITIES

Browser Core Module (lib/features/browser_core/) WAJIB memenuhi
kapabilitas engine berikut, diwariskan dari native rendering engine
(Chromium/Edge/WebKit) melalui flutter_inappwebview. Client TIDAK
BOLEH menambah dependency WebView kedua di luar abstraksi ini.

Rendering & Networking:
- Modern Rendering Engine (HTML5/CSS3/JS) (★★★★★)
- GPU Acceleration untuk rendering cepat (★★★★★)
- WebGL WAJIB aktif untuk kebutuhan Trading Charts (★★★★★)
- HTTP/2 & HTTP/3 untuk networking cepat (★★★★★)
- WebSocket WAJIB stabil untuk data realtime (★★★★★)

Stability & Isolation:
- Multi Process, setiap tab terisolasi (★★★★★)
- Sandbox Isolation per proses tab (★★★★★)
- Crash Recovery, tab WAJIB bisa direstore otomatis (★★★★★)
- Memory Optimization, efisiensi RAM per tab (★★★★★)

Catatan: Kapabilitas di atas bergantung pada native engine
per-platform (lihat WEBVIEW ENGINE). Jika suatu platform tidak
mendukung salah satu kapabilitas, WAJIB didokumentasikan sebagai
Known Limitation di docs/, bukan dilewati diam-diam.

==========================================================

AI GATEWAY

AI tidak boleh berjalan lokal di Client.

Semua request AI harus melewati Cloudflare AI Gateway.

Gateway mendukung:
- Gemini
- OpenAI
- Claude
- DeepSeek
- Qwen

Provider AI dapat diganti atau ditambahkan dari backend 
tanpa perlu rilis ulang Client.

==========================================================

DATABASE & CACHE

Cloud Database:
- Cloudflare D1

Cloud Storage:
- Cloudflare R2

Local Cache:
- Hive / SharedPreferences / Isar (Sesuai kebutuhan fitur)

==========================================================

SECURITY & PRIVACY

Prinsip Dasar:
- API Key platform/AI TIDAK BOLEH ditanam di Client.
- Authentication & Access Tokens WAJIB dikelola Backend (Cloudflare Workers).
- Environment Variable dikelola dengan secure build configs.

Connection & Transport Security (WAJIB, non-negotiable):
- HTTPS Only, force HTTPS di seluruh navigasi (★★★★★)
- TLS 1.3 untuk seluruh koneksi terenkripsi (★★★★★)
- Certificate Validation, verifikasi sertifikat setiap koneksi (★★★★★)
- Safe Browsing, peringatan otomatis untuk phishing/malicious site (★★★★★)
- Site Isolation, isolasi proses per-situs (★★★★★)
- Certificate Pinning WAJIB untuk melindungi domain layanan sendiri
  (Cloudflare Workers/backend TradePilot) (★★★★☆)

Permission & Access Control:
- Permission Manager WAJIB mengatur akses Camera/Mic/Location secara
  eksplisit per situs, bukan izin global (★★★★★)

Privacy Features:
- Incognito Mode, tidak menyimpan history/cache sama sekali (★★★★★)
- Tracking Protection, blokir tracker pihak ketiga secara default (★★★★☆)
- Fingerprint Protection, mengurangi browser fingerprinting (★★★★☆)
- DNS over HTTPS, enkripsi query DNS (★★★★☆)

Trading-Specific Security (prioritas tertinggi, menyangkut dana user):
- Secure Broker Login, kredensial broker WAJIB dienkripsi & tidak
  pernah melewati AI Gateway atau logging pihak ketiga (★★★★★)
- Multi Broker Workspace, sesi tiap broker terisolasi satu sama lain,
  tidak boleh terjadi cross-session leakage (★★★★★)

==========================================================

PERFORMANCE OPTIMIZATION

Browser Core WAJIB menerapkan strategi optimisasi berikut agar
tetap ringan meski multi-tab, multi-broker, dan AI Chat berjalan
bersamaan:

- Smart Cache untuk loading halaman lebih cepat (★★★★★)
- Tab Sleeping, tab tidak aktif WAJIB di-suspend untuk hemat RAM
  (★★★★★)
- Memory Saver, reduksi penggunaan memori secara agresif saat
  banyak tab terbuka (★★★★★)
- Battery Saver, penurunan konsumsi daya di perangkat mobile/laptop
  (★★★★☆)

Target performa mengikuti Success Metrics di PRD (Time to First
Chart < 2 detik, Tab Switching Latency < 100ms). Fitur di atas
TIDAK BOLEH mengorbankan kapabilitas WebGL/HTTP realtime yang
dibutuhkan Trading Chart.

==========================================================

WORKSPACE DESIGN & UX GOAL

Seluruh layout Desktop & Tablet WAJIB menggunakan konsep 
Workbench Resizable Split Layout.

Layout Design Reference:
Visual Studio Code + TradingView + Professional Trading Terminal

Komponen Utama Layout:
- Activity Bar (Navigation & Switcher)
- Side Bar (Features Tools, Browser Tabs, & Community Channels)
- Main Workspace (Resizable Split Panels: Chart, AI Chat, Orderbook)
- Bottom Status Bar (Lofi Player Status, System Metrics)

==========================================================

CODE REUSE & QUALITY RULE

Target:
Minimal 95% Codebase (UI, State Management, Business Logic) 
digunakan bersama secara utuh antara Windows, Android, dan iOS.

==========================================================

NEW FEATURE RULE

Sebelum membuat fitur baru, WAJIB menjawab:
1. Apakah fitur ini sudah terisolasi di dalam folder feature-nya?
2. Apakah UI dan State (Riverpod Provider) dipisahkan secara rapi?
3. Apakah fitur ini tidak mengganggu kompatibilitas lintas platform (Windows/Mobile)?
4. Apakah pemrosesan data berat diwakilkan ke Cloudflare Backend?

Jika jawabannya TIDAK, maka arsitektur fitur HARUS diperbaiki.

==========================================================

PROJECT STRUCTURE

tradepilot-platform/
│
├── backend/            # Cloudflare Workers, D1, R2, AI Gateway
├── docs/               # Architecture Blueprint & CONSTITUTION
├── tools/              # Build Scripts & Automation
│
└── app/                # Main Flutter Application
    ├── android/
    ├── ios/
    ├── windows/
    ├── pubspec.yaml
    └── lib/
        ├── core/       # Network (Dio), Theme, Constants, Utils
        │   └── security/   # Encryption, Auth Token Handling, Secure Storage
        └── features/   # Feature-First Modules
            ├── browser_core/
            ├── trading_terminal/
            ├── ai_pilot/
            ├── calculators/
            ├── social_community/
            └── learning_hub/

==========================================================

CLIENT ARCHITECTURE LAYERS

Setiap fitur di lib/features/ WAJIB terpetakan ke salah satu layer
berikut. Layer ini BUKAN folder baru, melainkan tanggung jawab
logis di dalam struktur Feature-First yang sudah ditetapkan di
PROJECT STRUCTURE:

- UI Layer -> Widgets/Layout di setiap feature/*/presentation/
- Browser Layer -> Engine/Tab/Session, tinggal di features/browser_core/
- AI Layer -> Analysis/Chat, tinggal di features/ai_pilot/
- Trading Layer -> Dashboard/Journal, tinggal di features/trading_terminal/
  dan features/calculators/
- Security Layer -> Encryption/Auth, tinggal di lib/core/security/
  (WAJIB dipisah dari lib/core/network/, tidak boleh dicampur
  dengan Dio Interceptors)
- Cloud Layer -> Sync/API, tinggal di lib/core/network/ yang
  berkomunikasi ke backend/ (Cloudflare Workers)

Fitur yang tidak bisa dipetakan ke salah satu layer di atas WAJIB
dipertanyakan ulang melalui NEW FEATURE RULE sebelum dikerjakan.

==========================================================

ABSOLUTE RULE

Jangan pernah memilih solusi/library Flutter 
yang HANYA mendukung Android tetapi merusak Windows Desktop (atau sebaliknya).

Selalu prioritaskan package yang memiliki dukungan 
Cross-Platform (Desktop + Mobile) yang stabil.

==========================================================

MISSION

TradePilot bukan sekadar aplikasi trading.

TradePilot adalah AI Trading Super-App Workspace 
lintas platform yang membantu trader menganalisis, 
berkomunikasi, belajar, dan mengambil keputusan 
secara lebih percaya diri.

==========================================================