package com.tradepilot.desktop.theme

import androidx.compose.animation.core.tween

/**
 * Prioritas 13 (UI Polish - Smooth Animation, Consistent): satu tempat buat
 * durasi/easing animasi supaya ActivityBar, Fullscreen reveal, panel
 * transitions, dst tidak punya angka durasi yang beda-beda sembarangan.
 */
object AppAnimations {
    val FastTween = tween<Float>(durationMillis = 100)
    val MediumTween = tween<Float>(durationMillis = 180)
    val SlowTween = tween<Float>(durationMillis = 280)
}
