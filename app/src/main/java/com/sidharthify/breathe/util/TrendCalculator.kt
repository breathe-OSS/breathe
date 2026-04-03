// SPDX-License-Identifier: MIT
/*
 * TrendCalculator.kt - Utility function to calculate AQI change over the past hour based on historical data
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 Aditya Choudhary <empirea99@gmail.com>
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

import com.sidharthify.breathe.data.HistoryPoint
import kotlin.math.abs

fun calculateChange1h(
    history: List<HistoryPoint>?,
    currentAqi: Int,
    isNaqi: Boolean,
): Int? {
    if (history.isNullOrEmpty()) return null

    val nowSec = System.currentTimeMillis() / 1000L
    val targetTs = nowSec - 3600L
    val toleranceSec = 1800L

    var bestPoint: HistoryPoint? = null
    var bestDiff = Long.MAX_VALUE

    for (point in history) {
        val diff = abs(point.ts - targetTs)
        if (diff <= toleranceSec && diff < bestDiff) {
            bestDiff = diff
            bestPoint = point
        }
    }

    bestPoint ?: return null

    val pastAqi = if (isNaqi) bestPoint.aqi else (bestPoint.usAqi ?: bestPoint.aqi)
    return currentAqi - pastAqi
}
