// SPDX-License-Identifier: MIT
/*
 * AnimationSettings.kt - Data class to manage animation preferences across the app
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.sidharthify.breathe.data

import androidx.compose.runtime.compositionLocalOf

/**
 * Useful for low-end devices or users who prefer reduced motion.
 */
data class AnimationSettings(
    val animationsEnabled: Boolean = true,
    val screenTransitions: Boolean = true,
    val colorTransitions: Boolean = true,
    val numberAnimations: Boolean = true,
    val pulseEffects: Boolean = true,
    val morphingPill: Boolean = true,
    val pressFeedback: Boolean = true,
    val listAnimations: Boolean = true,
) {
    companion object {
        val Disabled =
            AnimationSettings(
                animationsEnabled = false,
                screenTransitions = false,
                colorTransitions = false,
                numberAnimations = false,
                pulseEffects = false,
                morphingPill = false,
                pressFeedback = false,
                listAnimations = false,
            )

        val Reduced =
            AnimationSettings(
                animationsEnabled = true,
                screenTransitions = true,
                colorTransitions = false,
                numberAnimations = false,
                pulseEffects = false,
                morphingPill = false,
                pressFeedback = true,
                listAnimations = false,
            )
    }
}

val LocalAnimationSettings = compositionLocalOf { AnimationSettings() }
