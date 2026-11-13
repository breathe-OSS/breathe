// SPDX-License-Identifier: MIT
/*
 * AqiScale.kt - US EPA and NAQI breakpoints, colours, categories and display AQI
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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.HistoryPoint
import com.sidharthify.breathe.data.NodeHistoryPoint
import com.sidharthify.breathe.data.NodeReading
import kotlin.math.roundToInt

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

// the bright end of the scale (greens and yellows) is unreadable as text on a
// light surface, so text there gets a darker shade of the same colour
fun Color.asAqiText(surface: Color): Color {
    if (surface.luminance() < 0.5f) {
        return this
    }
    if (luminance() < 0.3f) {
        return this
    }
    return lerp(this, Color.Black, 0.5f)
}

// text sitting on an AQI colour, dark on the greens and yellows and white on
// the reds and purples
fun Color.onAqiColor(): Color {
    if (luminance() > 0.35f) {
        return Color(0xFF1B1B1F)
    }
    return Color.White
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

// EPA truncates pm10 to whole µg/m³ before picking the band
fun calculateUsAqiPm10(pm10: Double): Int {
    val c = pm10.toInt().toDouble()
    return when {
        c <= 54.0 -> linearInterp(c, 0.0, 54.0, 0, 50)
        c <= 154.0 -> linearInterp(c, 55.0, 154.0, 51, 100)
        c <= 254.0 -> linearInterp(c, 155.0, 254.0, 101, 150)
        c <= 354.0 -> linearInterp(c, 255.0, 354.0, 151, 200)
        c <= 424.0 -> linearInterp(c, 355.0, 424.0, 201, 300)
        c <= 504.0 -> linearInterp(c, 425.0, 504.0, 301, 400)
        else -> linearInterp(c, 505.0, 604.0, 401, 500)
    }
}

// the EPA formula, a straight line between the two ends of whichever band c is in
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

// the backend sends pm2.5 under either key depending on the provider
val AqiResponse.pm25: Double
    get() {
        return concentrations?.get("pm2.5")
            ?: concentrations?.get("pm2_5")
            ?: 0.0
    }

// us_aqi can be missing on a zone, so fall back to working it out from pm2.5
fun AqiResponse.displayAqi(useNaqi: Boolean): Int {
    if (useNaqi) {
        return nAqi
    }
    val fallback =
        if (pm25 > 0) {
            calculateUsAqi(pm25)
        } else {
            0
        }
    return usAqi ?: fallback
}

fun NodeReading.displayAqi(useNaqi: Boolean): Int? {
    if (useNaqi) {
        return aqi
    }
    return usAqi ?: aqi
}

fun HistoryPoint.displayAqi(useNaqi: Boolean): Int {
    if (useNaqi) {
        return aqi
    }
    return usAqi ?: aqi
}

fun NodeHistoryPoint.displayAqi(useNaqi: Boolean): Int {
    if (useNaqi) {
        return aqi
    }
    return usAqi ?: aqi
}

fun calculateNaqiPm25(pm25: Double): Int {
    val c = pm25.roundToInt().toDouble()
    return when {
        c <= 30.0 -> linearInterp(c, 0.0, 30.0, 0, 50)
        c <= 60.0 -> linearInterp(c, 31.0, 60.0, 51, 100)
        c <= 90.0 -> linearInterp(c, 61.0, 90.0, 101, 200)
        c <= 120.0 -> linearInterp(c, 91.0, 120.0, 201, 300)
        c <= 250.0 -> linearInterp(c, 121.0, 250.0, 301, 400)
        else -> minOf(linearInterp(c, 251.0, 380.0, 401, 500), 500)
    }
}

fun pm25ToAqi(
    pm25: Double,
    useNaqi: Boolean,
): Int {
    if (useNaqi) {
        return calculateNaqiPm25(pm25)
    }
    return calculateUsAqi(pm25)
}

fun aqiBandIndex(
    aqi: Int,
    isUsStandard: Boolean,
): Int {
    val limits =
        if (isUsStandard) {
            listOf(50, 100, 150, 200, 300)
        } else {
            listOf(50, 100, 200, 300, 400)
        }
    limits.forEachIndexed { index, limit ->
        if (aqi <= limit) {
            return index
        }
    }
    return limits.size
}

data class HealthAdvice(
    val title: String,
    val text: String,
)

private val HEALTH_ADVICE =
    listOf(
        HealthAdvice("Good time to be outside", "The air is clean. Open the windows and enjoy it."),
        HealthAdvice("Fine for most people", "If you're unusually sensitive to pollution, go easier on long outdoor workouts."),
        HealthAdvice(
            "Sensitive groups take care",
            "Kids, older adults and anyone with asthma or heart disease should cut back on long or heavy exertion outside.",
        ),
        HealthAdvice("Cut back outdoors", "Everyone should ease off heavy exertion outside. Sensitive groups should keep outdoor time short."),
        HealthAdvice(
            "Avoid exertion outside",
            "Keep windows shut and run a purifier if you have one. Wear an N95 if you need to be out for long.",
        ),
        HealthAdvice("Stay indoors", "Keep windows and doors shut. If you have to go out, wear a well fitted N95."),
    )

fun getHealthAdvice(
    aqi: Int,
    isUsStandard: Boolean,
): HealthAdvice = HEALTH_ADVICE[aqiBandIndex(aqi, isUsStandard)]
