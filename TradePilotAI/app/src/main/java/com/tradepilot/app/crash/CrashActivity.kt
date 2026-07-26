package com.tradepilot.app.crash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView

/**
 * Activity darurat, SENGAJA ditulis pakai View system biasa (bukan Compose,
 * bukan Hilt) supaya tetap bisa tampil BAHKAN kalau penyebab crash ada di
 * Compose/Hilt/tema itu sendiri. Satu-satunya tujuan: nampilin stack trace
 * lengkap di layar HP supaya bisa langsung di-screenshot, tanpa perlu
 * ADB/USB/PC sama sekali.
 *
 * Dipasang lewat Thread.setDefaultUncaughtExceptionHandler di
 * TradePilotApplication -- lihat file itu untuk detail pemasangannya.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "Tidak ada detail error."

        val textView = TextView(this).apply {
            text = "TradePilot AI - Crash Report\n" +
                "==========================================\n" +
                "Silakan screenshot layar ini dan kirim ke developer.\n" +
                "==========================================\n\n" +
                stackTrace
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            textSize = 11f
            setPadding(24, 48, 24, 48)
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            addView(textView)
        }

        setContentView(scrollView)
    }

    companion object {
        private const val EXTRA_STACK_TRACE = "extra_stack_trace"

        fun start(context: Context, stackTrace: String) {
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra(EXTRA_STACK_TRACE, stackTrace)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}