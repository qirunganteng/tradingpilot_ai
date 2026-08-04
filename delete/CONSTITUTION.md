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

Client pertama:

TradePilot AI Browser

==========================================================

VISION

TradePilot bukan aplikasi Android.

TradePilot bukan browser biasa.

TradePilot adalah Platform Trading Workspace
berbasis AI.

Android hanyalah salah satu Client.

Desktop hanyalah salah satu Client.

Semua Client memiliki kemampuan yang sama.

==========================================================

PROJECT GOAL

Membangun Trading Workspace profesional
yang dapat berjalan di berbagai platform
dengan pengalaman pengguna yang konsisten.

Target Platform

Android

Windows

Linux (future)

macOS (future)

==========================================================

CORE PRINCIPLE

Single Platform.

Multiple Clients.

Shared Business Logic.

Backend First.

Client Thin.

==========================================================

ARCHITECTURE

TradePilot terdiri dari

1.

Shared Module

2.

Platform Client

3.

Backend

==========================================================

SHARED MODULE

Semua Business Logic

WAJIB berada pada Shared Module.

Contoh

Risk Engine

Trading Logic

Journal

Repository

Use Case

AI Client

API Client

Validation

Formatter

Configuration

Model

DTO

Parser

Strategy

Rule Engine

==========================================================

PLATFORM CLIENT

Platform Client

TIDAK BOLEH

menyimpan Business Logic.

Platform Client hanya bertugas

Rendering UI

Navigation

Platform Integration

Browser Engine

Notification

Permission

Window Management

File Picker

Camera

Clipboard

Screenshot

==========================================================

BACKEND

Semua AI

WAJIB

berjalan di Backend.

Backend terdiri dari

Cloudflare Worker

AI Gateway

Cloudflare D1

Cloudflare R2

OCR

Image Processor

Risk Service

Analytics Service

Future Services

==========================================================

PROGRAMMING LANGUAGE

Business Logic

WAJIB

menggunakan Kotlin.

Shared Module

WAJIB

Pure Kotlin.

Tidak boleh bergantung pada Android SDK.

==========================================================

UI FRAMEWORK

Android

Compose

Desktop

Compose Multiplatform

Future Platform

Compose Multiplatform

==========================================================

BROWSER ENGINE

Browser Engine

HARUS

dipisahkan melalui abstraction.

Buat interface

BrowserEngine

Implementasi

Android

↓

Android WebView

Desktop

↓

JCEF

Linux

↓

JCEF

macOS

↓

JCEF

Business Logic

TIDAK BOLEH

bergantung pada Browser tertentu.

==========================================================

AI

AI

tidak boleh berada di Client.

Semua AI

harus melewati

AI Gateway.

Gateway mendukung

Gemini

OpenAI

Claude

DeepSeek

Qwen

Provider dapat diganti

tanpa update Client.

==========================================================

DATABASE

Cloud Database

↓

Cloudflare D1

Storage

↓

Cloudflare R2

Local Cache

↓

Room Database

==========================================================

SECURITY

API Key

TIDAK BOLEH

ditanam di Client.

Token

WAJIB

dikelola Backend.

==========================================================

CLIENT RESPONSIBILITY

Client hanya bertugas

Render UI

Render Browser

Render AI Result

Capture Screenshot

Send Request

Receive Response

==========================================================

WORKSPACE

Seluruh Client

WAJIB

menggunakan konsep

Workbench.

Bukan Dashboard.

Layout terdiri dari

Activity Bar

Side Bar

Workspace

AI Workspace

==========================================================

DESIGN GOAL

Visual Studio Code

+

TradingView

+

Professional Trading Terminal

==========================================================

CODE REUSE

Target

Minimal

90%

Business Logic

digunakan bersama

antara Android

dan Desktop.

==========================================================

NEW FEATURE RULE

Sebelum membuat fitur baru

WAJIB menjawab

Apakah fitur ini reusable?

Apakah fitur ini platform independent?

Apakah Business Logic berada di Shared Module?

Apakah UI dipisahkan dari Logic?

Apakah Backend tetap menjadi pusat sistem?

Jika jawabannya tidak,

maka desain harus diperbaiki.

==========================================================

PROJECT STRUCTURE

tradepilot-platform

│

├── shared

│

├── backend

│

├── android-client

│

├── desktop-client

│

├── docs

│

└── tools

==========================================================

FUTURE

Linux

macOS

Web Client

Plugin

Marketplace

Extension

AI Marketplace

harus dapat ditambahkan

tanpa mengubah fondasi.

==========================================================

ABSOLUTE RULE

Jangan pernah memilih solusi
yang hanya menguntungkan Android.

Jangan pernah memilih solusi
yang membuat Desktop
harus ditulis ulang.

Selalu pilih solusi
yang dapat digunakan
oleh seluruh Platform.

==========================================================

MISSION

TradePilot bukan sekadar Browser.

TradePilot adalah AI Trading Workspace
lintas platform
yang membantu trader belajar,
menganalisis,
dan mengambil keputusan
secara lebih percaya diri.

==========================================================
