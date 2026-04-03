// SPDX-License-Identifier: MIT
/*
 * Utils.kt - Utility functions for AQI calculations, formatting, and other common tasks across the app
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
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import kotlin.math.roundToInt

fun createBlobIcon(
    context: Context,
    color: Int,
): Drawable {
    val radius = 32f
    val bitmap = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true }

    paint.color = color
    canvas.drawCircle(radius, radius, radius, paint)

    return BitmapDrawable(context.resources, bitmap)
}

fun formatPollutantName(key: String): String =
    when (key.lowercase()) {
        "pm2_5", "pm2.5" -> "PM2.5"
        "pm10" -> "PM10"
        "no2" -> "NO2"
        "so2" -> "SO2"
        "co" -> "CO"
        "ch4" -> "CH4"
        else -> key.uppercase()
    }

@Composable
fun PollutantText(
    rawKey: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    val annotatedString = remember(rawKey) {
        buildAnnotatedString {
            when (rawKey.lowercase()) {
                "pm2.5", "pm2_5" -> {
                    append("PM")
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append("2.5")
                    }
                }
                "pm10" -> {
                    append("PM")
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append("10")
                    }
                }
                "no2" -> {
                    append("NO")
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append("2")
                    }
                }
                "so2" -> {
                    append("SO")
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append("2")
                    }
                }
                "ch4" -> {
                    append("CH")
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append("4")
                    }
                }
                "o3" -> {
                    append("O")
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append("3")
                    }
                }
                else -> {
                    append(formatPollutantName(rawKey))
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = style,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight
    )
}

fun getAqiColor(
    aqi: Int,
    isUsStandard: Boolean = false,
): Color {
    if (isUsStandard) {
        return when (aqi) {
            in 0..50 -> Color(0xFF00E400)
            in 51..100 -> Color(0xFFFFFF00)
            in 101..150 -> Color(0xFFFF7E00)
            in 151..200 -> Color(0xFFFF0000)
            in 201..300 -> Color(0xFF8F3F97)
            else -> Color(0xFF7E0023)
        }
    }

    // NAQI (Indian) Colors
    return when (aqi) {
        in 0..50 -> Color(0xFF55A84F)
        in 51..100 -> Color(0xFFA3C853)
        in 101..200 -> Color(0xFFFDD74B)
        in 201..300 -> Color(0xFFFB9A34)
        in 301..400 -> Color(0xFFE93F33)
        else -> Color(0xFFAF2D24)
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        else -> ">1d ago"
    }
}

fun calculateCigarettes(pm25: Double): Double {
    // 22 µg/m³ ≈ 1 cigarette
    val cigs = pm25 / 22.0
    return (cigs * 10).roundToInt() / 10.0
}

fun calculateUsAqi(pm25: Double): Int {
    // US EPA Breakpoints (2024)
    return when {
        pm25 <= 9.0 -> linearInterp(pm25, 0.0, 9.0, 0, 50)
        pm25 <= 35.4 -> linearInterp(pm25, 9.1, 35.4, 51, 100)
        pm25 <= 55.4 -> linearInterp(pm25, 35.5, 55.4, 101, 150)
        pm25 <= 125.4 -> linearInterp(pm25, 55.5, 125.4, 151, 200)
        pm25 <= 225.4 -> linearInterp(pm25, 125.5, 225.4, 201, 300)
        pm25 <= 325.4 -> linearInterp(pm25, 225.5, 325.4, 301, 400)
        else -> linearInterp(pm25, 325.5, 500.0, 401, 500)
    }
}

private fun linearInterp(
    c: Double,
    cLow: Double,
    cHigh: Double,
    iLow: Int,
    iHigh: Int,
): Int = (((iHigh - iLow) / (cHigh - cLow)) * (c - cLow) + iLow).roundToInt()

data class AqiCategory(
    val label: String,
)

fun getAqiCategory(
    aqi: Int,
    isUsStandard: Boolean = false,
): AqiCategory {
    if (isUsStandard) {
        return when (aqi) {
            in 0..50 -> AqiCategory("Good")
            in 51..100 -> AqiCategory("Moderate")
            in 101..150 -> AqiCategory("Unhealthy for Sensitive Groups")
            in 151..200 -> AqiCategory("Unhealthy")
            in 201..300 -> AqiCategory("Very Unhealthy")
            else -> AqiCategory("Hazardous")
        }
    }

    // NAQI (Indian) Categories
    return when (aqi) {
        in 0..50 -> AqiCategory("Good")
        in 51..100 -> AqiCategory("Satisfactory")
        in 101..200 -> AqiCategory("Moderate")
        in 201..300 -> AqiCategory("Poor")
        in 301..400 -> AqiCategory("Very Poor")
        else -> AqiCategory("Severe")
    }
}