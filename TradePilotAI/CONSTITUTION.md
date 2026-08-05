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

Client Pertama:

TradePilot AI Super-App (Flutter)

==========================================================

VISION

TradePilot bukan sekadar aplikasi mobile.

TradePilot bukan browser biasa.

TradePilot adalah Super-App & AI Trading Workspace 
Lintas Platform.

Android hanyalah salah satu Target.

Windows Desktop hanyalah salah satu Target.

iOS & Desktop Platform lainnya adalah Target Paralel.

Semua Target memiliki kapabilitas dan konsistensi 
pengalaman pengguna yang sama.

==========================================================

PROJECT GOAL

Membangun Trading Workspace profesional
yang dapat berjalan di berbagai platform
secara responsif, modular, dan berkinerja tinggi.

Target Platform

Windows Desktop (Primary Desktop)

Android (Primary Mobile)

iOS

macOS (future)

Linux (future)

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

Core Framework:
- Framework: Flutter SDK
- State Management: Flutter Riverpod
- HTTP Client: Dio
- WebView Engine: flutter_inappwebview
- Window Management: window_manager
- Split Layout Engine: multi_split_view
- Data Grid: PlutoGrid

==========================================================

MODULAR STRUCTURE (FEATURE-FIRST)

Arsitektur aplikasi WAJIB menggunakan pendekatan Feature-First.

Setiap modul fitur terisolasi dan mandiri, terdiri dari:

1. Trading Workspace
   - Chart TradingView via WebView
   - Real-time Market Data & Orderbook
   - High-performance PlutoGrid untuk Journal & Trade History

2. AI Pilot Workspace
   - Streaming Chat Interface (via Cloudflare AI Gateway)
   - Chart & Technical Pattern Analysis
   - Prompt & Model Management

3. Social & Community
   - Chat Room Trader
   - Signal Sharing & Copy Trade Signals

4. Learning & Entertainment
   - Educational Hub & Paper Trading
   - Lofi Radio / Relaxation Player Service

5. Core Infrastructure
   - Centralized Dio Client & Network Services
   - State Management Providers (Riverpod)
   - Theme Data (Modern Dark Trading Style)
   - Cross-Platform Hardware Abstractions

==========================================================

CLIENT RESPONSIBILITY

Flutter Client TIKAD BOLEH melakukan pemrosesan berat AI.

Tugas Flutter Client:
- Rendering UI secara reaktif & responsif
- Managing Local Application State via Riverpod
- Rendering Webview (TradingView Chart)
- Audio/Media Streaming handling (Lofi Radio)
- Capture Screenshot & Image Picker
- Receiving & Rendering AI Streaming Responses
- Managing Window Decorations (Titlebar, Tray, IPC)

==========================================================

BACKEND STACK

Semua Pemrosesan AI dan Central Data Storage 
WAJIB berjalan di Cloudflare Stack.

Backend Component:
- Cloudflare Workers (API Gateway & Serverless Logic)
- Cloudflare AI Gateway (LLM Router: Gemini, OpenAI, Claude, DeepSeek, Qwen)
- Cloudflare D1 (Relational Database)
- Cloudflare R2 (Object Storage / Screenshots / Media)

==========================================================

PROGRAMMING LANGUAGE & UI FRAMEWORK

Language:
- Dart (Pure Dart + Flutter SDK)

UI Framework:
- Flutter Framework (Multi-platform Native Engine)

==========================================================

WEBVIEW ENGINE

Penyajian chart dan web content HARUS dipisah 
melalui abstraksi WebView yang fleksibel.

Implementation:
- Windows Desktop -> flutter_inappwebview (Edge Chromium Engine)
- Android -> flutter_inappwebview (Android System WebView)
- iOS / macOS -> flutter_inappwebview (WKWebView)

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

SECURITY

- API Key platform/AI TIDAK BOLEH ditanam di Client.
- Authentication & Access Tokens WAJIB dikelola Backend (Cloudflare Workers).
- Environment Variable dikelola dengan secure build configs.

==========================================================

WORKSPACE DESIGN & UX GOAL

Seluruh layout Desktop & Tablet WAJIB menggunakan konsep 
Workbench Resizable Layout.

Layout Design Reference:
Visual Studio Code + TradingView + Professional Trading Terminal

Komponen Utama Layout:
- Activity Bar (Navigation & Switcher)
- Side Bar (Features Tools & Community Channels)
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
├── docs/               # Architecture Blueprint & Prompt History
├── tools/              # Build Scripts & Automation
│
└── app/                # Main Flutter Application
    ├── android/
    ├── ios/
    ├── windows/
    ├── pubspec.yaml
    └── lib/
        ├── core/       # Network (Dio), Theme, Constants, Utils
        └── features/   # Feature-First Modules
            ├── trading_workspace/
            ├── ai_pilot/
            ├── social_community/
            └── learning_lofi/

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