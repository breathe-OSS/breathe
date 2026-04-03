// SPDX-License-Identifier: MIT
/*
 * MorphingPill.kt - Composable component for a morphing pill shape with animation
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 Suvesh Moza <hellosuvesh@gmail.com>
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

package com.sidharthify.breathe.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.sidharthify.breathe.data.LocalAnimationSettings

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingPill(
    isSelected: Boolean,
    from: RoundedPolygon,
    to: RoundedPolygon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animationSettings = LocalAnimationSettings.current
    
    val morph =
        remember(from, to) {
            Morph(from.normalized(), to.normalized())
        }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isSelected, animationSettings.morphingPill) {
        if (isSelected) {
            if (animationSettings.morphingPill) {
                progress.snapTo(0f)
                kotlinx.coroutines.delay(120L)
                progress.animateTo(
                    1f,
                    animationSpec = tween(300),
                )
            } else {
                progress.snapTo(1f)
            }
        } else {
            progress.snapTo(0f)
        }
    }

    val path = remember { Path() }
    val matrix = remember { Matrix() }

    Box(
        modifier =
            modifier
                .drawWithContent {
                    val p =
                        morph.toPath(
                            progress = progress.value,
                            path = path,
                            startAngle = 0,
                        )

                    processPath(
                        path = p,
                        size = size,
                        scaleFactor = 1f,
                        scaleMatrix = matrix,
                    )

                    drawPath(path, color = color)
                },
    )
}

private fun processPath(
    path: Path,
    size: Size,
    scaleFactor: Float,
    scaleMatrix: Matrix = Matrix(),
): Path {
    scaleMatrix.reset()
    scaleMatrix.apply { scale(x = size.width * scaleFactor, y = size.height * scaleFactor) }
    path.transform(scaleMatrix)
    path.translate(size.center - path.getBounds().center)
    return path
}
