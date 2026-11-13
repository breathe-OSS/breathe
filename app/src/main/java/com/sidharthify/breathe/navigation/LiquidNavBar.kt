// SPDX-License-Identifier: MIT
/*
 * LiquidNavBar.kt - Floating toolbar tabs with a stretchy, draggable selection blob
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

package com.sidharthify.breathe.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.AnimationSettings
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val SNAPPY = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
private val LAGGING = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
private val FOLLOW = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

@Composable
fun LiquidNavBar(
    screens: List<AppScreen>,
    current: AppScreen,
    animationSettings: AnimationSettings,
    vertical: Boolean,
    onSelect: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = screens.size
    val target = screens.indexOf(current).coerceAtLeast(0).toFloat()
    val start = remember { Animatable(target) }
    val end = remember { Animatable(target) }
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val select by rememberUpdatedState(onSelect)
    val animate = animationSettings.animationsEnabled && animationSettings.morphingPill
    val bulge by animateFloatAsState(
        targetValue =
            if (pressed && animationSettings.pressFeedback) {
                1f
            } else {
                0f
            },
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "NavBulge",
    )

    LaunchedEffect(target, animate) {
        if (dragging) {
            return@LaunchedEffect
        }
        if (!animate) {
            start.snapTo(target)
            end.snapTo(target)
            return@LaunchedEffect
        }
        val forward = target > start.value
        launch {
            start.animateTo(target, if (forward) LAGGING else SNAPPY)
        }
        launch {
            end.animateTo(target, if (forward) SNAPPY else LAGGING)
        }
    }

    val blob = MaterialTheme.colorScheme.secondaryContainer

    fun slotAt(position: Float, length: Float): Float = (position / length * count - 0.5f).coerceIn(0f, (count - 1).toFloat())

    Box(
        modifier =
            modifier
                .pointerInput(count, vertical) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        pressed = true
                        val length =
                            if (vertical) {
                                size.height.toFloat()
                            } else {
                                size.width.toFloat()
                            }
                        fun along(o: Offset): Float =
                            if (vertical) {
                                o.y
                            } else {
                                o.x
                            }
                        var position = along(down.position)
                        var moved = 0f
                        var lastSlot = slotAt(position, length).roundToInt()
                        drag(down.id) { change ->
                            val delta = along(change.positionChange())
                            moved += abs(delta)
                            position += delta
                            if (moved > 12f) {
                                dragging = true
                                val slot = slotAt(position, length)
                                scope.launch { start.animateTo(slot, FOLLOW) }
                                scope.launch { end.animateTo(slot, FOLLOW) }
                                val rounded = slot.roundToInt()
                                if (rounded != lastSlot) {
                                    lastSlot = rounded
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            }
                            change.consume()
                        }
                        pressed = false
                        val chosen = slotAt(position, length).roundToInt()
                        val wasDragging = dragging
                        dragging = false
                        val next = screens[chosen]
                        if (wasDragging || next != current) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                        if (next == current && wasDragging) {
                            scope.launch { start.animateTo(chosen.toFloat(), SNAPPY) }
                            scope.launch { end.animateTo(chosen.toFloat(), SNAPPY) }
                        }
                        select(next)
                    }
                },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val slot =
                if (vertical) {
                    size.height / count
                } else {
                    size.width / count
                }
            val across =
                if (vertical) {
                    size.width
                } else {
                    size.height
                }
            val lo = minOf(start.value, end.value)
            val hi = maxOf(start.value, end.value)
            val stretch = (hi - lo).coerceIn(0f, 1.5f)
            val baseLong = slot * 0.82f
            val baseCross = across * 0.86f
            val grow = 1f + bulge * 0.08f
            val long = (baseLong + (hi - lo) * slot) * grow
            val cross = baseCross * (1f - stretch * 0.12f) * (1f + bulge * 0.06f)
            val center = ((lo + hi) / 2f + 0.5f) * slot
            val radius = CornerRadius(cross / 2f, cross / 2f)
            if (vertical) {
                drawRoundRect(
                    color = blob,
                    topLeft = Offset((size.width - cross) / 2f, center - long / 2f),
                    size = Size(cross, long),
                    cornerRadius = radius,
                )
            } else {
                drawRoundRect(
                    color = blob,
                    topLeft = Offset(center - long / 2f, (size.height - cross) / 2f),
                    size = Size(long, cross),
                    cornerRadius = radius,
                )
            }
        }

        val items: @Composable (AppScreen, Modifier) -> Unit = { screen, itemModifier ->
            val index = screens.indexOf(screen)
            val near = 1f - abs((start.value + end.value) / 2f - index).coerceIn(0f, 1f)
            val isSelected = screen == current
            val tint by animateColorAsState(
                targetValue =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                animationSpec = tween(if (animationSettings.colorTransitions) 180 else 0),
                label = "NavTint",
            )
            Box(
                modifier =
                    itemModifier.semantics {
                        selected = isSelected
                        role = Role.Tab
                        onClick(label = screen.label) {
                            select(screen)
                            true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isSelected) screen.iconFilled else screen.iconOutlined,
                    contentDescription = screen.label,
                    tint = tint,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                val s = 1f + near * 0.12f + if (isSelected) bulge * 0.08f else 0f
                                scaleX = s
                                scaleY = s
                            },
                )
            }
        }

        if (vertical) {
            Column(modifier = Modifier.fillMaxSize()) {
                screens.forEach { screen ->
                    items(screen, Modifier.fillMaxWidth().weight(1f))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                screens.forEach { screen ->
                    items(screen, Modifier.fillMaxHeight().weight(1f))
                }
            }
        }
    }
}
