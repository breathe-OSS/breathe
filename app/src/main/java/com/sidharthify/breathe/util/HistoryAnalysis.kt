// SPDX-License-Identifier: MIT
/*
 * HistoryAnalysis.kt - Bucketing, summaries and sensor comparison for extended history
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

import com.sidharthify.breathe.data.HistoricalDataPoint
import com.sidharthify.breathe.data.HistoryMetric
import com.sidharthify.breathe.data.HistoryMode
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.sqrt

const val COMPARE_MIN_OVERLAP = 24

val US_AQI_BANDS = listOf(0.0, 50.0, 100.0, 150.0, 200.0, 300.0, 500.0)
val NAQI_BANDS = listOf(0.0, 50.0, 100.0, 200.0, 300.0, 400.0, 500.0)
val PM25_BANDS = listOf(0.0, 9.0, 35.4, 55.4, 125.4, 225.4, 325.4)
val PM10_BANDS = listOf(0.0, 54.0, 154.0, 254.0, 354.0, 424.0, 604.0)
val US_CATEGORY_SAMPLES = listOf(25, 75, 125, 175, 250, 400)
val INDIA_CATEGORY_SAMPLES = listOf(25, 75, 150, 250, 350, 450)

fun categorySample(
    index: Int,
    usScale: Boolean,
): Int {
    if (usScale) {
        return US_CATEGORY_SAMPLES[index]
    }
    return INDIA_CATEGORY_SAMPLES[index]
}

enum class SeriesHue {
    PRIMARY,
    SECONDARY,
    RAW,
}

data class HistorySeries(
    val key: String,
    val label: String,
    val raw: Boolean,
    val hue: SeriesHue,
)

data class Bucketed(
    val ts: Long,
    val values: Map<String, Double?>,
)

data class SeriesStats(
    val avg: Double?,
    val maxValue: Double?,
    val maxTs: Long?,
    val minValue: Double?,
    val count: Int,
)

data class PairStats(
    val n: Int,
    val r: Double?,
    val ratio: Double?,
    val gap: Double?,
)

enum class VerdictTone {
    GOOD,
    FAIR,
    POOR,
    NONE,
}

data class Verdict(
    val label: String,
    val tone: VerdictTone,
)

fun HistoryMetric.bands(): List<Double>? =
    when (this) {
        HistoryMetric.AQI -> US_AQI_BANDS
        HistoryMetric.NAQI -> NAQI_BANDS
        HistoryMetric.PM25 -> PM25_BANDS
        HistoryMetric.PM10 -> PM10_BANDS
        else -> null
    }

fun activeSeries(
    metric: HistoryMetric,
    mode: HistoryMode,
): List<HistorySeries> {
    val out = mutableListOf<HistorySeries>()
    metric.keys.forEachIndexed { i, key ->
        val hue =
            if (i == 0) {
                SeriesHue.PRIMARY
            } else {
                SeriesHue.SECONDARY
            }
        if (mode != HistoryMode.RAW) {
            out += HistorySeries(key, metric.names[i], false, hue)
        }
        if (mode != HistoryMode.CAL) {
            val rawHue =
                if (metric.keys.size == 1 && mode == HistoryMode.BOTH) {
                    SeriesHue.RAW
                } else {
                    hue
                }
            out += HistorySeries("${key}_raw", "${metric.names[i]} raw", true, rawHue)
        }
    }
    return out
}

fun compareKey(
    metric: HistoryMetric,
    mode: HistoryMode,
): String {
    val base = metric.keys.first()
    if (mode == HistoryMode.RAW) {
        return "${base}_raw"
    }
    return base
}

fun compareLabel(
    metric: HistoryMetric,
    mode: HistoryMode,
): String {
    val name = metric.names.first()
    if (mode == HistoryMode.RAW) {
        return "$name raw"
    }
    return name
}

fun intervalSeconds(token: String): Long {
    val match = Regex("^(\\d+)\\s*(mo|m|h|d|w)$").find(token.trim().lowercase()) ?: return 3600L
    val n = match.groupValues[1].toLong()
    val unit =
        when (match.groupValues[2]) {
            "m" -> 60L
            "h" -> 3600L
            "d" -> 86400L
            "w" -> 604800L
            else -> 2592000L
        }
    return n * unit
}

fun displayBucket(
    points: List<HistoricalDataPoint>,
    interval: String,
): Long {
    val base = intervalSeconds(interval)
    if (points.size < 2) {
        return base
    }
    val span = points.last().ts - points.first().ts
    if (span > 62 * 86400L) {
        return maxOf(base, 86400L)
    }
    if (span > 14 * 86400L) {
        return maxOf(base, 4 * 3600L)
    }
    return base
}

fun aggregate(
    points: List<HistoricalDataPoint>,
    bucket: Long,
    interval: String,
    keys: List<String>,
): List<Bucketed> {
    if (bucket <= intervalSeconds(interval)) {
        return points.map { p -> Bucketed(p.ts, keys.associateWith { p.value(it) }) }
    }
    val offset = TimeZone.getDefault().rawOffset / 1000L
    val out = mutableListOf<Bucketed>()
    var currentTs: Long? = null
    val sums = HashMap<String, Double>()
    val counts = HashMap<String, Int>()
    fun flush() {
        val ts = currentTs ?: return
        out += Bucketed(ts, keys.associateWith { k -> counts[k]?.let { c -> sums.getValue(k) / c } })
        sums.clear()
        counts.clear()
    }
    points.forEach { p ->
        val start = Math.floorDiv(p.ts + offset, bucket) * bucket - offset
        if (currentTs != start) {
            flush()
            currentTs = start
        }
        keys.forEach { k ->
            val v = p.value(k)
            if (v != null) {
                sums[k] = (sums[k] ?: 0.0) + v
                counts[k] = (counts[k] ?: 0) + 1
            }
        }
    }
    flush()
    return out
}

fun seriesStats(
    points: List<HistoricalDataPoint>,
    key: String,
): SeriesStats {
    var sum = 0.0
    var count = 0
    var maxV: Double? = null
    var maxTs: Long? = null
    var minV: Double? = null
    points.forEach { p ->
        val v = p.value(key) ?: return@forEach
        sum += v
        count += 1
        if (maxV == null || v > maxV!!) {
            maxV = v
            maxTs = p.ts
        }
        if (minV == null || v < minV!!) {
            minV = v
        }
    }
    val avg =
        if (count > 0) {
            sum / count
        } else {
            null
        }
    return SeriesStats(avg, maxV, maxTs, minV, count)
}

fun bandSampleFor(
    value: Double,
    bands: List<Double>,
): Int {
    val found = bands.indexOfFirst { value < it }
    val index =
        if (found == -1) {
            5
        } else {
            (found - 1).coerceIn(0, 5)
        }
    return US_CATEGORY_SAMPLES[index]
}

fun seriesByTs(
    points: List<HistoricalDataPoint>,
    key: String,
): Map<Long, Double> {
    val out = LinkedHashMap<Long, Double>()
    points.forEach { p ->
        p.value(key)?.let { out[p.ts] = it }
    }
    return out
}

fun pairStats(
    a: Map<Long, Double>,
    b: Map<Long, Double>,
): PairStats {
    val xs = mutableListOf<Double>()
    val ys = mutableListOf<Double>()
    a.forEach { (ts, v) ->
        val w = b[ts]
        if (w != null) {
            xs += v
            ys += w
        }
    }
    val n = xs.size
    if (n < 2) {
        return PairStats(n, null, null, null)
    }
    val mx = xs.average()
    val my = ys.average()
    var sxy = 0.0
    var sxx = 0.0
    var syy = 0.0
    var gap = 0.0
    val ratios = mutableListOf<Double>()
    for (i in 0 until n) {
        sxy += (xs[i] - mx) * (ys[i] - my)
        sxx += (xs[i] - mx) * (xs[i] - mx)
        syy += (ys[i] - my) * (ys[i] - my)
        gap += abs(ys[i] - xs[i])
        if (xs[i] > 0) {
            ratios += ys[i] / xs[i]
        }
    }
    ratios.sort()
    val ratio =
        if (ratios.isNotEmpty()) {
            ratios[ratios.size / 2]
        } else {
            null
        }
    val r =
        if (sxx > 0 && syy > 0) {
            sxy / sqrt(sxx * syy)
        } else {
            null
        }
    return PairStats(n, r, ratio, gap / n)
}

fun verdict(
    stats: PairStats,
    metric: HistoryMetric,
): Verdict {
    val r = stats.r
    if (stats.n < COMPARE_MIN_OVERLAP || r == null) {
        return Verdict("Not enough overlap", VerdictTone.NONE)
    }
    val levelMatches =
        if (metric == HistoryMetric.TEMP || metric == HistoryMetric.HUMIDITY) {
            val limit =
                if (metric == HistoryMetric.TEMP) {
                    2.0
                } else {
                    8.0
                }
            stats.gap != null && stats.gap <= limit
        } else {
            stats.ratio != null && abs(stats.ratio - 1) <= 0.15
        }
    if (r >= 0.9 && levelMatches) {
        return Verdict("Very consistent", VerdictTone.GOOD)
    }
    if (r >= 0.9) {
        return Verdict("Same pattern, different level", VerdictTone.FAIR)
    }
    if (r >= 0.75) {
        return Verdict("Mostly consistent", VerdictTone.FAIR)
    }
    if (r >= 0.5) {
        return Verdict("Loosely related", VerdictTone.POOR)
    }
    return Verdict("Inconsistent", VerdictTone.POOR)
}
