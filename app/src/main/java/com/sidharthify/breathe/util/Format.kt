// SPDX-License-Identifier: MIT
/*
 * Format.kt - Pollutant names with proper subscripts and short relative times
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
