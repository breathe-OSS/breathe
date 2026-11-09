// SPDX-License-Identifier: MIT
/*
 * WeatherHistory.kt - Join weather-history buckets with PM samples for filters and impact cards
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

package com.sidharthify.breathe.util

import com.sidharthify.breathe.R
import com.sidharthify.breathe.data.HistoricalDataPoint
import com.sidharthify.breathe.data.WeatherHistory
import com.sidharthify.breathe.data.WeatherHistoryPoint
import kotlin.math.roundToInt

val WEATHER_FILTER_LABELS =
    linkedMapOf(
        "rain" to "Rain",
        "snow" to "Snow",
        "fog" to "Fog",
        "cloudy" to "Cloudy",
        "clear" to "Clear",
    )

fun weatherConditionIconRes(condition: String): Int =
    when (condition.lowercase()) {
        "clear" -> R.drawable.clear_day_24px
        "rain" -> R.drawable.rainy_24px
        "thunderstorm" -> R.drawable.thunderstorm_24px
        "fog", "smog" -> R.drawable.foggy_24px
        "all" -> R.drawable.rounded_filter_list_24
        else -> R.drawable.cloud_24px
    }

data class WeatherPm25Group(
    val condition: String,
    val label: String,
    val avg: Double,
    val hours: Int,
    val diffPct: Int,
)

private fun conditionForTs(
    ts: Long,
    interval: Long,
    anchor: Long,
    lookup: Map<Long, WeatherHistoryPoint>,
): String? {
    val step = interval.coerceAtLeast(1L)
    val bucket = anchor + Math.floorDiv(ts - anchor, step) * step
    return lookup[bucket]?.condition
}

fun matchesWeatherFilter(condition: String?, filter: String): Boolean {
    if (filter == "all") return true
    if (condition == null) return false
    if (filter == "rain") return condition == "rain" || condition == "thunderstorm"
    return condition == filter
}

fun filterHistoryByWeather(
    data: List<HistoricalDataPoint>,
    weather: WeatherHistory?,
    filter: String,
): List<HistoricalDataPoint> {
    if (filter == "all") return data
    if (weather == null || weather.points.isEmpty()) return emptyList()
    val lookup = weather.points.associateBy { it.ts }
    val interval = weather.interval
    return data.filter { matchesWeatherFilter(conditionForTs(it.ts, interval, weather.anchor, lookup), filter) }
}

fun weatherPm25Groups(
    data: List<HistoricalDataPoint>,
    weather: WeatherHistory?,
): Map<String, Pair<Double, Int>> {
    if (weather == null || weather.points.isEmpty()) return emptyMap()
    val lookup = weather.points.associateBy { it.ts }
    val interval = weather.interval
    val groups = linkedMapOf<String, Pair<Double, Int>>()
    for (point in data) {
        val pm25 = point.pm25 ?: continue
        var condition = conditionForTs(point.ts, interval, weather.anchor, lookup) ?: continue
        if (condition == "thunderstorm") condition = "rain"
        val current = groups[condition] ?: (0.0 to 0)
        groups[condition] = (current.first + pm25) to (current.second + 1)
    }
    return groups
}

fun weatherImpactCards(
    data: List<HistoricalDataPoint>,
    weather: WeatherHistory?,
): List<WeatherPm25Group> {
    val groups = weatherPm25Groups(data, weather)
    if (groups.isEmpty()) return emptyList()
    val totalSum = groups.values.sumOf { it.first }
    val totalCount = groups.values.sumOf { it.second }
    if (totalCount == 0) return emptyList()
    val overallAvg = totalSum / totalCount
    val interval = weather?.interval ?: 3600L

    val cards =
        WEATHER_FILTER_LABELS.mapNotNull { (condition, label) ->
            val group = groups[condition] ?: return@mapNotNull null
            if (group.second < 3) return@mapNotNull null
            val avg = group.first / group.second
            val hours = ((group.second * interval) / 3600.0).roundToInt()
            val diffPct =
                if (overallAvg == 0.0) {
                    0
                } else {
                    (((avg - overallAvg) / overallAvg) * 100).roundToInt()
                }
            WeatherPm25Group(
                condition = condition,
                label = label,
                avg = avg,
                hours = hours,
                diffPct = diffPct,
            )
        }
    return if (cards.size >= 2) cards else emptyList()
}
