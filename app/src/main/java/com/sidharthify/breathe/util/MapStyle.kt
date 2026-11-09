// SPDX-License-Identifier: MIT
/*
 * MapStyle.kt - Basemap tiles and boundary colours for the map screen
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


package com.sidharthify.breathe.util

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import kotlin.math.cos
import kotlin.math.sin

// OSM's own tiles, recoloured on the way to the screen. the stock style
// is loud enough that the AQI markers get lost in it, so light mode is
// toned down and dark mode is built to look dark instead of inverted
object MapStyle {
    const val ATTRIBUTION = "© OpenStreetMap contributors"

    // frames every zone from Kathua up to Leh and Kargil on a phone in portrait
    val HOME_CENTER = GeoPoint(33.6, 75.85)
    const val HOME_ZOOM = 8.4

    // the south edge has to sit well below Kathua or the scroll limit pins
    // the view to the top and the southern zones end up under the nav bar
    val SCROLL_LIMIT = BoundingBox(37.5, 81.0, 30.5, 72.0)

    // a plain invert turns forests purple and water orange. inverting and
    // then spinning the hue back round flips only the brightness, so water
    // stays blue and forests stay green, then it gets pulled down and
    // desaturated so the markers stay the loudest thing on the map
    private val DARK_FILTER =
        ColorMatrixColorFilter(
            chain(
                invert(),
                hueRotate(180f),
                saturation(0.35f),
                contrast(0.8f),
                brightness(0.84f),
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 4f,
                        0f, 1f, 0f, 0f, 6f,
                        0f, 0f, 1f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            ),
        )

    // about half the colour and a slight lift, it keeps terrain and roads
    // readable without fighting the markers
    private val LIGHT_FILTER =
        ColorMatrixColorFilter(
            chain(
                saturation(0.45f),
                contrast(0.92f),
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 10f,
                        0f, 1f, 0f, 0f, 10f,
                        0f, 0f, 1f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            ),
        )

    fun tileFilter(isDarkTheme: Boolean): ColorMatrixColorFilter {
        if (isDarkTheme) {
            return DARK_FILTER
        }
        return LIGHT_FILTER
    }

    // the boundary stays black or white so it can't be mistaken for a road,
    // the halo in the opposite colour lifts it off whatever is underneath
    fun boundary(
        isDarkTheme: Boolean,
        density: Float,
    ): IndiaBoundaryOverlay.Style {
        if (isDarkTheme) {
            return IndiaBoundaryOverlay.Style(
                lineColor = 0xD9FFFFFF.toInt(),
                lineWidthPx = 1.6f * density,
                haloColor = 0x59000000,
                haloWidthPx = 4.5f * density,
            )
        }
        return IndiaBoundaryOverlay.Style(
            lineColor = 0xD9202830.toInt(),
            lineWidthPx = 1.6f * density,
            haloColor = 0x99FFFFFF.toInt(),
            haloWidthPx = 4.5f * density,
        )
    }

    // applies the steps left to right
    private fun chain(vararg steps: ColorMatrix): ColorMatrix {
        val result = ColorMatrix()
        steps.forEach { result.postConcat(it) }
        return result
    }

    private fun invert(): ColorMatrix =
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )

    // same weights as the css hue-rotate() filter
    private fun hueRotate(degrees: Float): ColorMatrix {
        val radians = Math.toRadians(degrees.toDouble())
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        return ColorMatrix(
            floatArrayOf(
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    private fun saturation(amount: Float): ColorMatrix = ColorMatrix().apply { setSaturation(amount) }

    // squeezes everything towards mid grey
    private fun contrast(amount: Float): ColorMatrix {
        val offset = 128f * (1f - amount)
        return ColorMatrix(
            floatArrayOf(
                amount, 0f, 0f, 0f, offset,
                0f, amount, 0f, 0f, offset,
                0f, 0f, amount, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    private fun brightness(amount: Float): ColorMatrix = ColorMatrix().apply { setScale(amount, amount, amount, 1f) }
}
