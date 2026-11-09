// SPDX-License-Identifier: MIT
/*
 * MapMarkers.kt - Draws and caches the AQI bubbles shown on the map
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap

/**
 * Renders the round AQI bubbles for the map. Bitmaps are cached by what's
 * drawn on them, so a refresh where most numbers don't change costs nothing,
 * and the paints are made once instead of per marker.
 */
class MapMarkerIcons(context: Context) {
    data class Spec(
        val text: String,
        val fillColor: Int,
        val ringColor: Int,
        val diameterDp: Float,
        val showLiveDot: Boolean,
    )

    private val density = context.resources.displayMetrics.density
    private val cache = LruCache<Spec, Bitmap>(96)

    // room around the circle for the drop shadow
    private val shadowPad = 4f * density

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun get(spec: Spec): Bitmap {
        cache.get(spec)?.let { return it }
        val bitmap = draw(spec)
        cache.put(spec, bitmap)
        return bitmap
    }

    private fun draw(spec: Spec): Bitmap {
        val diameter = spec.diameterDp * density
        val size = (diameter + shadowPad * 2).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val center = size / 2f
        val radius = diameter / 2f - ringPaint.strokeWidth / 2f

        // shadow sits a little low, like the bubble is floating over the map
        fillPaint.color = spec.fillColor
        fillPaint.setShadowLayer(3f * density, 0f, 1f * density, 0x55000000)
        canvas.drawCircle(center, center, radius, fillPaint)
        fillPaint.clearShadowLayer()

        ringPaint.color = spec.ringColor
        canvas.drawCircle(center, center, radius, ringPaint)

        if (spec.text.isNotEmpty()) {
            // three digit readings need a smaller face to fit inside the ring
            val textScale =
                if (spec.text.length >= 3) {
                    0.36f
                } else {
                    0.42f
                }
            textPaint.textSize = diameter * textScale
            textPaint.color = readableTextColor(spec.fillColor)
            val baseline = center - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(spec.text, center, baseline, textPaint)
        }

        if (spec.showLiveDot) {
            val dotRadius = diameter * 0.15f
            val angle = Math.toRadians(-45.0)
            val dotX = center + (radius * kotlin.math.cos(angle)).toFloat()
            val dotY = center + (radius * kotlin.math.sin(angle)).toFloat()
            fillPaint.color = LIVE_GREEN
            canvas.drawCircle(dotX, dotY, dotRadius, fillPaint)
            val oldWidth = ringPaint.strokeWidth
            ringPaint.strokeWidth = 1.5f * density
            canvas.drawCircle(dotX, dotY, dotRadius, ringPaint)
            ringPaint.strokeWidth = oldWidth
        }

        return bitmap
    }

    companion object {
        const val LIVE_GREEN = 0xFF4CAF50.toInt()
        const val NO_DATA_GREY = 0xFF9E9E9E.toInt()

        // dark text on the yellows and greens, white on the reds and purples
        fun readableTextColor(background: Int): Int {
            if (ColorUtils.calculateLuminance(background) > 0.35) {
                return 0xFF1B1B1F.toInt()
            }
            return Color.WHITE
        }
    }
}
