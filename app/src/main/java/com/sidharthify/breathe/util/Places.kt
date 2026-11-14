// SPDX-License-Identifier: MIT
/*
 * Places.kt - Sensor and district helpers for per sensor zones
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

import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.HistoryPoint
import com.sidharthify.breathe.data.SensorReadings
import com.sidharthify.breathe.data.Zone

val DIVISION_ORDER = listOf("Jammu", "Kashmir", "Ladakh")

data class RawPoint(
    val title: String,
    val text: String,
)

val RAW_EXPLAINER_POINTS =
    listOf(
        RawPoint(
            "PM2.5 and PM10",
            "The laser counter's own numbers, before AirGradient's correction. The correction mostly lowers PM2.5, more so in humid air, so raw usually reads higher.",
        ),
        RawPoint(
            "Temperature and humidity",
            "Read inside the sensor's case, which warms up in the sun. Raw temperature runs a few degrees high in the afternoon and raw humidity runs low.",
        ),
        RawPoint(
            "AQI, advice and charts",
            "The index, its colour, the health advice, the cigarette count and every chart are worked out again from the raw numbers.",
        ),
        RawPoint(
            "Satellite estimates",
            "Places without a sensor have no raw readings, so they don't change.",
        ),
        RawPoint(
            "History",
            "Raw history only goes back to the day Breathe started keeping it. Hours before that show as gaps.",
        ),
        RawPoint(
            "Which one to trust",
            "Calibrated is closer to what a reference monitor would show. Raw is useful for checking the sensor itself or comparing it with another device.",
        ),
    )

val Zone.isEstimate: Boolean
    get() {
        if (kind != null) {
            return kind == "estimate"
        }
        return !provider.equals("airgradient", ignoreCase = true)
    }

val AqiResponse.isEstimate: Boolean
    get() {
        if (kind != null) {
            return kind == "estimate"
        }
        return source?.contains("airgradient", ignoreCase = true) != true
    }

val AqiResponse.isSensorOffline: Boolean
    get() = !isEstimate && source?.startsWith("openmeteo") == true

fun placeLine(
    district: String?,
    division: String?,
): String {
    if (district.isNullOrBlank()) {
        return ""
    }
    if (division.isNullOrBlank()) {
        return "Part of the $district district"
    }
    return "Part of the $district district in $division"
}

fun divisionRank(division: String?): Int {
    val index = DIVISION_ORDER.indexOf(division)
    if (index == -1) {
        return DIVISION_ORDER.size
    }
    return index
}

fun districtAverages(
    legacy: List<Zone>,
    sensors: List<Zone>,
): List<Zone> =
    legacy.mapNotNull { zone ->
        val members = sensors.filter { !it.isEstimate && it.legacyZone == zone.id }
        if (members.size < 2) {
            null
        } else {
            zone.copy(
                kind = "district",
                district = members.first().district,
                division = members.first().division,
            )
        }
    }

fun migratePinnedIds(
    pinned: List<String>,
    zones: List<Zone>,
): List<String> {
    if (zones.isEmpty()) {
        return pinned
    }
    val known = zones.map { it.id }.toSet()
    val next = mutableListOf<String>()
    pinned.forEach { id ->
        if (id in known) {
            if (id !in next) {
                next += id
            }
            return@forEach
        }
        val replacements = zones.filter { it.legacyZone == id }
        if (replacements.isEmpty()) {
            if (id !in next) {
                next += id
            }
            return@forEach
        }
        replacements.forEach { zone ->
            if (zone.id !in next) {
                next += zone.id
            }
        }
    }
    return next
}

fun AqiResponse.asRaw(): AqiResponse {
    val r = raw ?: return this
    val merged = (concentrations ?: emptyMap()).toMutableMap()
    r.pm25?.let { merged["pm2_5"] = it }
    r.pm10?.let { merged["pm10"] = it }
    val rawHistory = history.orEmpty().any { it.pm25Raw != null }
    val shownHistory =
        if (rawHistory) {
            history.orEmpty().mapNotNull { point -> point.asRaw() }
        } else {
            history
        }
    val averages =
        if (!averages24hRaw.isNullOrEmpty()) {
            averages24hRaw
        } else {
            averages24h
        }
    return copy(
        nAqi = r.nAqi ?: nAqi,
        usAqi = r.usAqi ?: usAqi,
        mainPollutant = r.mainPollutant ?: mainPollutant,
        usMainPollutant = r.usMainPollutant ?: r.mainPollutant ?: usMainPollutant,
        concentrations = merged,
        averages24h = averages,
        history = shownHistory,
        readings =
            SensorReadings(
                pm25 = r.pm25,
                pm10 = r.pm10,
                temp = r.temp,
                humidity = r.humidity,
            ),
        showingRaw = true,
    )
}

private fun HistoryPoint.asRaw(): HistoryPoint? {
    if (pm25Raw == null) {
        return null
    }
    return copy(
        aqi = aqiRaw ?: aqi,
        usAqi = usAqiRaw ?: usAqi,
        pm25 = pm25Raw,
        pm10 = pm10Raw ?: pm10,
    )
}
