// SPDX-License-Identifier: MIT
/*
 * Expressive.kt - Squish on press, the app wide stand in for a ripple
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


package com.sidharthify.breathe.ui

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.LocalAnimationSettings

// shrink by a fixed 32dp at most so big cards dont collapse in on themselves,
// but never past 90% so small buttons still read as pressed
private fun Modifier.squish(isPressed: () -> Boolean): Modifier =
    composed {
        val animationSettings = LocalAnimationSettings.current
        val maxSquishPx = with(LocalDensity.current) { 32.dp.toPx() }
        var longestSidePx by remember { mutableIntStateOf(0) }
        val pressedScale =
            if (longestSidePx > 0) {
                (1f - maxSquishPx / longestSidePx).coerceAtLeast(0.90f)
            } else {
                0.90f
            }
        val scale by animateFloatAsState(
            targetValue =
                if (isPressed() && animationSettings.pressFeedback) {
                    pressedScale
                } else {
                    1f
                },
            animationSpec =
                if (animationSettings.pressFeedback) {
                    spring(dampingRatio = 0.4f, stiffness = 400f)
                } else {
                    spring(stiffness = 10000f)
                },
            label = "Squish",
        )

        this
            .onSizeChanged { longestSidePx = maxOf(it.width, it.height) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    }

// the squish replaces the ripple, so this is a normal clickable with the
// indication swapped out. still shows up as a button to talkback
fun Modifier.expressiveClickable(
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier =
    composed {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        this
            .squish { isPressed }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = role,
                onClickLabel = onClickLabel,
                onClick = onClick,
            )
    }

// just the feel, for cards that dont do anything when tapped. it never
// consumes the touch, so scrolling and any clickable children still work
fun Modifier.pressSquish(): Modifier =
    composed {
        var isPressed by remember { mutableStateOf(false) }
        this
            .squish { isPressed }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
    }

// an infinite transition keeps asking for frames even when its two ends are
// the same value, so when the effect is off we hand back a plain state and
// never start one
@Composable
fun loopingFloat(
    enabled: Boolean,
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String,
): State<Float> {
    if (!enabled) {
        return remember { mutableFloatStateOf(initialValue) }
    }
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(initialValue, targetValue, animationSpec, label)
}
