// SPDX-License-Identifier: MIT
/*
 * HistoryScreen.kt - Extended history with metrics, raw readings and sensor comparison
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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sidharthify.breathe.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidharthify.breathe.data.HistoricalDataPoint
import com.sidharthify.breathe.data.HistoryMetric
import com.sidharthify.breathe.data.HistoryMode
import com.sidharthify.breathe.data.HistoryState
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.util.COMPARE_MIN_OVERLAP
import com.sidharthify.breathe.util.HistoryPeriodMode
import com.sidharthify.breathe.util.HistorySeries
import com.sidharthify.breathe.util.PM10_BANDS
import com.sidharthify.breathe.util.PM25_BANDS
import com.sidharthify.breathe.util.SeriesHue
import com.sidharthify.breathe.util.US_CATEGORY_SAMPLES
import com.sidharthify.breathe.util.VerdictTone
import com.sidharthify.breathe.util.WEATHER_FILTER_LABELS
import com.sidharthify.breathe.util.activeSeries
import com.sidharthify.breathe.util.aggregate
import com.sidharthify.breathe.util.aqiBandIndex
import com.sidharthify.breathe.util.asAqiText
import com.sidharthify.breathe.util.bandSampleFor
import com.sidharthify.breathe.util.bands
import com.sidharthify.breathe.util.compareKey
import com.sidharthify.breathe.util.compareLabel
import com.sidharthify.breathe.util.displayBucket
import com.sidharthify.breathe.util.filterHistoryByWeather
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.intervalSeconds
import com.sidharthify.breathe.util.pairStats
import com.sidharthify.breathe.util.seriesByTs
import com.sidharthify.breathe.util.seriesStats
import com.sidharthify.breathe.util.verdict
import com.sidharthify.breathe.util.weatherImpactCards
import com.sidharthify.breathe.util.weatherPm25Groups
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val COMPARE_COLORS_DARK =
    listOf(0xFFD95926, 0xFF199E70, 0xFFC98500, 0xFFD55181, 0xFF008300, 0xFF9085E9, 0xFFE66767)
private val COMPARE_COLORS_LIGHT =
    listOf(0xFFEB6834, 0xFF1BAF7A, 0xFFEDA100, 0xFFE87BA4, 0xFF008300, 0xFF6250D6, 0xFFE34948)

private const val GENERIC_RAW_NOTE = "Showing the sensor's uncorrected readings."

data class ChartLine(
    val label: String,
    val color: Color,
    val dashed: Boolean,
    val width: Dp,
    val values: List<Double?>,
)

fun formatValue(
    value: Double?,
    decimals: Int,
): String {
    if (value == null || value.isNaN()) {
        return "--"
    }
    if (decimals == 0) {
        return value.roundToInt().toString()
    }
    return String.format(Locale.getDefault(), "%.${decimals}f", value)
}

fun unitSuffix(unit: String): String {
    if (unit == "US AQI") {
        return ""
    }
    return " $unit"
}

@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
private fun seriesColor(series: HistorySeries): Color {
    val dark = isDarkSurface()
    return when (series.hue) {
        SeriesHue.RAW -> if (dark) Color(0xFFD95926) else Color(0xFFEB6834)
        SeriesHue.SECONDARY -> if (dark) Color(0xFF199E70) else Color(0xFF1BAF7A)
        SeriesHue.PRIMARY -> if (dark) Color(0xFF3987E5) else Color(0xFF2A78D6)
    }
}

private fun timeLabel(
    ts: Long,
    bucket: Long,
): String {
    val pattern =
        if (bucket >= 86400L) {
            "d MMM"
        } else {
            "d MMM HH:00"
        }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ts * 1000))
}

@Composable
fun ExtendedHistoryScreen(
    zoneName: String,
    zoneId: String,
    placeLine: String,
    district: String?,
    historyState: HistoryState,
    comparablePlaces: List<Zone>,
    rawHintHidden: Boolean,
    onBack: () -> Unit,
    onRangeSelected: (String) -> Unit,
    onToggleCustom: () -> Unit,
    onCustomRangeChanged: (String) -> Unit,
    onCustomIntervalChanged: (String) -> Unit,
    onApplyCustom: () -> Unit,
    onShowPeriod: () -> Unit,
    onPeriodModeSelected: (HistoryPeriodMode) -> Unit,
    onPeriodStep: (Long) -> Unit,
    onPeriodPicked: (LocalDate, LocalDate) -> Unit,
    onMetricSelected: (HistoryMetric) -> Unit,
    onModeSelected: (HistoryMode) -> Unit,
    onDownloadCSV: () -> Unit,
    onWeatherFilterSelected: (String) -> Unit,
    onHideRawHint: () -> Unit,
    onToggleCompare: (String) -> Unit,
    onSetCompare: (List<String>) -> Unit,
) {
    val metric = historyState.metric
    val mode = historyState.mode
    val rows = historyState.data
    val weather = historyState.weatherHistory
    val series = remember(metric, mode) { activeSeries(metric, mode) }
    val filtered =
        remember(rows, weather, historyState.weatherFilter) {
            filterHistoryByWeather(rows, weather, historyState.weatherFilter)
        }
    val weatherGroups = remember(rows, weather) { weatherPm25Groups(rows, weather) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth()
                .widthIn(max = 900.dp)
                .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "$zoneName History",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (placeLine.isNotEmpty()) {
                    Text(
                        placeLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryMetric.entries.forEach { option ->
                FilterChip(
                    selected = metric == option,
                    onClick = { onMetricSelected(option) },
                    label = { Text(option.label) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            HistoryMode.entries.forEachIndexed { index, option ->
                ToggleButton(
                    checked = mode == option,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onModeSelected(option)
                        }
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            HistoryMode.entries.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                ) {
                    Text(option.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Column {
            HistoryRangeSelector(
                selectedRange = historyState.selectedRange,
                showCustomInputs = historyState.showCustomInputs,
                customRange = historyState.customRange,
                customInterval = historyState.customInterval,
                periodActive = historyState.periodActive,
                periodMode = historyState.periodMode,
                period = historyState.period,
                onShowPeriod = onShowPeriod,
                onPeriodModeSelected = onPeriodModeSelected,
                onPeriodStep = onPeriodStep,
                onPeriodPicked = onPeriodPicked,
                onRangeSelected = onRangeSelected,
                onToggleCustom = onToggleCustom,
                onCustomRangeChanged = onCustomRangeChanged,
                onCustomIntervalChanged = onCustomIntervalChanged,
                onApplyCustom = onApplyCustom,
            )
        }

        if (!historyState.isLoading && weatherGroups.size >= 2) {
            WeatherFilterChips(
                selectedFilter = historyState.weatherFilter,
                presentConditions = weatherGroups.filterValues { it.second > 0 }.keys,
                onFilterSelected = onWeatherFilterSelected,
            )
        }

        if (mode != HistoryMode.CAL && !historyState.isLoading && historyState.error == null) {
            val message = rawNote(rows, metric, historyState.interval)
            val generic = message == GENERIC_RAW_NOTE
            if (!generic || !rawHintHidden) {
                RawBanner(
                    message = message,
                    showPoints = generic,
                    onHide =
                        if (generic) {
                            onHideRawHint
                        } else {
                            null
                        },
                )
            }
        }

        if (historyState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator()
            }
        } else if (historyState.error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    historyState.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else if (filtered.isEmpty()) {
            Text(
                if (rows.isNotEmpty()) {
                    "No readings for this weather in this range."
                } else {
                    "No readings for this selection."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            HistorySummaryCard(
                points = filtered,
                allRows = rows,
                series = series,
                metric = metric,
                mode = mode,
                interval = historyState.interval,
                weatherFilter = historyState.weatherFilter,
            )

            MainHistoryCard(
                points = filtered,
                series = series,
                metric = metric,
                interval = historyState.interval,
            )

            val calendar =
                remember(filtered, mode) {
                    filtered
                        .map { row ->
                            if (mode == HistoryMode.RAW) {
                                row.copy(pm25 = row.pm25Raw, pm10 = row.pm10Raw)
                            } else {
                                row
                            }
                        }.filter { it.pm25 != null || it.pm10 != null }
                }
            if (calendar.isNotEmpty()) {
                ExtendedDotHistoryChart(
                    data = calendar,
                    showPm25 = true,
                    showPm10 = true,
                )
            }

            if (intervalSeconds(historyState.interval) <= 3600L) {
                HourProfileCard(points = filtered, series = series, metric = metric)
            }

            val key = series.firstOrNull()?.key
            if (key != null) {
                val cards = remember(rows, weather, key) { weatherImpactCards(rows, weather) { it.value(key) } }
                if (cards.isNotEmpty()) {
                    WeatherImpactPanel(
                        cards = cards,
                        title = "Average ${series.first().label} under each kind of weather in this range",
                        unit = metric.unit,
                        decimals = metric.decimals,
                        judged = metric != HistoryMetric.TEMP && metric != HistoryMetric.HUMIDITY,
                    )
                }
            }

            CompareCard(
                zoneName = zoneName,
                zoneId = zoneId,
                district = district,
                base = filtered,
                historyState = historyState,
                places = comparablePlaces,
                onToggleCompare = onToggleCompare,
                onSetCompare = onSetCompare,
            )
        }

        Button(
            onClick = onDownloadCSV,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download CSV", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(LocalNavBarClearance.current))
    }
}

private fun rawNote(
    rows: List<HistoricalDataPoint>,
    metric: HistoryMetric,
    interval: String,
): String {
    val keys = metric.keys.map { "${it}_raw" }
    val first = rows.firstOrNull { row -> keys.any { row.value(it) != null } }
    if (first == null) {
        return "There are no raw readings stored for this range yet. Breathe only started keeping them recently."
    }
    if (rows.isNotEmpty() && first.ts - rows.first().ts > 2 * intervalSeconds(interval)) {
        val day = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(first.ts * 1000))
        return "Raw readings for this sensor start on $day. Earlier parts of the range are blank in raw."
    }
    return GENERIC_RAW_NOTE
}

@Composable
private fun HistoryCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun CardTitle(
    title: String,
    sub: String?,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (!sub.isNullOrEmpty()) {
            Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun valueColor(
    key: String,
    value: Double?,
): Color {
    val surface = MaterialTheme.colorScheme.surface
    if (value == null) {
        return MaterialTheme.colorScheme.onSurface
    }
    return when (key.removeSuffix("_raw")) {
        "us_aqi" -> getAqiColor(value.roundToInt(), true).asAqiText(surface)
        "pm2_5" -> getAqiColor(bandSampleFor(value, PM25_BANDS), true).asAqiText(surface)
        "pm10" -> getAqiColor(bandSampleFor(value, PM10_BANDS), true).asAqiText(surface)
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun HistorySummaryCard(
    points: List<HistoricalDataPoint>,
    allRows: List<HistoricalDataPoint>,
    series: List<HistorySeries>,
    metric: HistoryMetric,
    mode: HistoryMode,
    interval: String,
    weatherFilter: String,
) {
    val unit = unitSuffix(metric.unit)
    val step = intervalSeconds(interval)
    HistoryCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            series.forEach { s ->
                val st = seriesStats(points, s.key)
                Column {
                    if (st.count == 0) {
                        Text(s.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No readings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(
                            "${s.label} average",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${formatValue(st.avg, metric.decimals)}$unit",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = valueColor(s.key, st.avg),
                        )
                        Text(
                            "Peak ${formatValue(st.maxValue, metric.decimals)} on ${timeLabel(st.maxTs ?: 0L, step)} · low ${formatValue(st.minValue, metric.decimals)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        val aqiKey =
            if (mode == HistoryMode.RAW) {
                "us_aqi_raw"
            } else {
                "us_aqi"
            }
        val tally = IntArray(6)
        var tallied = 0
        points.forEach { p ->
            val v = p.value(aqiKey) ?: return@forEach
            tally[aqiBandIndex(v.roundToInt(), true)] += 1
            tallied += 1
        }
        if (tallied > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (mode == HistoryMode.RAW) {
                    "Time in each US AQI category, raw"
                } else {
                    "Time in each US AQI category"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Color.Transparent, RoundedCornerShape(100)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                tally.forEachIndexed { i, n ->
                    if (n > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(n.toFloat())
                                    .fillMaxSize()
                                    .background(getAqiColor(US_CATEGORY_SAMPLES[i], true), RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tally.forEachIndexed { i, n ->
                    if (n > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(10.dp)
                                        .background(getAqiColor(US_CATEGORY_SAMPLES[i], true), RoundedCornerShape(2.dp)),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${getAqiCategory(US_CATEGORY_SAMPLES[i], true).label} ${(n * 100.0 / tallied).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }

        val expected =
            if (allRows.size > 1) {
                ((allRows.last().ts - allRows.first().ts) / step + 1).toInt()
            } else {
                allRows.size
            }
        val coverage =
            if (expected > 0) {
                minOf(100, (allRows.size * 100.0 / expected).roundToInt())
            } else {
                0
            }
        val foot =
            if (weatherFilter == "all") {
                val unitName =
                    if (step <= 3600L) {
                        "hours"
                    } else {
                        "buckets"
                    }
                "$coverage% of $unitName have readings"
            } else {
                "Only ${WEATHER_FILTER_LABELS[weatherFilter]?.lowercase()} hours, ${points.size} of ${allRows.size}"
            }
        Text(foot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MainHistoryCard(
    points: List<HistoricalDataPoint>,
    series: List<HistorySeries>,
    metric: HistoryMetric,
    interval: String,
) {
    val bucket = remember(points, interval) { displayBucket(points, interval) }
    val shown = remember(points, bucket, series) { aggregate(points, bucket, interval, series.map { it.key }) }
    val step =
        when {
            bucket >= 86400L -> "daily averages"
            bucket >= 4 * 3600L -> "4 hour averages"
            else -> "hourly"
        }
    val shaded =
        if (metric.bands() != null) {
            ", shaded by US AQI category"
        } else {
            ""
        }
    val lines =
        series.map { s ->
            ChartLine(
                label = s.label,
                color = seriesColor(s),
                dashed = s.raw,
                width = 2.dp,
                values = shown.map { it.values[s.key] },
            )
        }
    HistoryCard {
        CardTitle(metric.label, "${metric.unit}, $step$shaded")
        HistoryLineChart(
            labels = shown.map { timeLabel(it.ts, bucket) },
            lines = lines,
            bands = metric.bands(),
            beginAtZero = metric != HistoryMetric.TEMP,
            unit = metric.unit,
            decimals = metric.decimals,
            showCategory = metric == HistoryMetric.AQI,
        )
    }
}

@Composable
private fun HourProfileCard(
    points: List<HistoricalDataPoint>,
    series: List<HistorySeries>,
    metric: HistoryMetric,
) {
    val profile =
        remember(points, series) {
            val cal = Calendar.getInstance()
            series.map { s ->
                val sums = DoubleArray(24)
                val counts = IntArray(24)
                points.forEach { p ->
                    val v = p.value(s.key) ?: return@forEach
                    cal.timeInMillis = p.ts * 1000
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    sums[hour] += v
                    counts[hour] += 1
                }
                (0 until 24).map { h ->
                    if (counts[h] > 0) {
                        sums[h] / counts[h]
                    } else {
                        null
                    }
                }
            }
        }
    if (profile.all { line -> line.all { it == null } }) {
        return
    }
    val lines =
        series.mapIndexed { i, s ->
            ChartLine(s.label, seriesColor(s), s.raw, 2.dp, profile[i])
        }
    HistoryCard {
        CardTitle("Hour of day", "Average ${metric.label} at each hour of the day, local time")
        HistoryLineChart(
            labels = (0 until 24).map { h -> "%02d:00".format(h) },
            lines = lines,
            bands = metric.bands(),
            beginAtZero = metric != HistoryMetric.TEMP,
            unit = metric.unit,
            decimals = metric.decimals,
            showCategory = false,
            height = 180.dp,
        )
    }
}

@Composable
private fun compareColor(
    index: Int,
    total: Int,
): Color {
    val dark = isDarkSurface()
    if (total > COMPARE_COLORS_DARK.size) {
        return if (dark) Color(0x73C4C7D0) else Color(0x6652514E)
    }
    val palette =
        if (dark) {
            COMPARE_COLORS_DARK
        } else {
            COMPARE_COLORS_LIGHT
        }
    return Color(palette[index % palette.size])
}

@Composable
private fun CompareCard(
    zoneName: String,
    zoneId: String,
    district: String?,
    base: List<HistoricalDataPoint>,
    historyState: HistoryState,
    places: List<Zone>,
    onToggleCompare: (String) -> Unit,
    onSetCompare: (List<String>) -> Unit,
) {
    val metric = historyState.metric
    val mode = historyState.mode
    val ids = historyState.compareIds
    val label = compareLabel(metric, mode)
    val key = compareKey(metric, mode)
    val nameOf = { id: String -> places.find { it.id == id }?.name ?: id }
    val others =
        ids.associateWith { id ->
            filterHistoryByWeather(historyState.compareRows[id].orEmpty(), historyState.weatherHistory, historyState.weatherFilter)
        }

    HistoryCard {
        CardTitle("Compare sensors", "Overlay $label from other sensors on $zoneName for the same range")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (district != null && places.any { it.district == district }) {
                OutlinedButton(onClick = { onSetCompare(places.filter { it.district == district }.map { it.id }) }) {
                    Text("Same district")
                }
            }
            OutlinedButton(onClick = { onSetCompare(places.map { it.id }) }) {
                Text("All sensors")
            }
            if (ids.isNotEmpty()) {
                OutlinedButton(onClick = { onSetCompare(emptyList()) }) {
                    Text("Clear")
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            places
                .sortedWith(compareBy<Zone>({ it.district ?: "" }, { it.name }))
                .forEach { place ->
                    FilterChip(
                        selected = place.id in ids,
                        onClick = { onToggleCompare(place.id) },
                        label = { Text(place.name) },
                    )
                }
        }

        if (historyState.compareLoading) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (ids.isEmpty()) {
            return@HistoryCard
        }

        val bucket = displayBucket(base, historyState.interval)
        val shownBase = aggregate(base, bucket, historyState.interval, listOf(key))
        val shownOthers = ids.map { id -> aggregate(others[id].orEmpty(), bucket, historyState.interval, listOf(key)) }
        val stamps = (shownBase.map { it.ts } + shownOthers.flatMap { list -> list.map { it.ts } }).distinct().sorted()
        val baseMap = shownBase.associate { it.ts to it.values[key] }
        val otherMaps = shownOthers.map { list -> list.associate { it.ts to it.values[key] } }
        val dark = isDarkSurface()
        val many = ids.size > COMPARE_COLORS_DARK.size
        val lines =
            listOf(
                ChartLine(
                    zoneName,
                    if (dark) Color(0xFF3987E5) else Color(0xFF2A78D6),
                    false,
                    3.dp,
                    stamps.map { baseMap[it] },
                ),
            ) +
                ids.mapIndexed { i, id ->
                    ChartLine(
                        nameOf(id),
                        compareColor(i, ids.size),
                        false,
                        if (many) 1.25.dp else 2.dp,
                        stamps.map { otherMaps[i][it] },
                    )
                }
        HistoryLineChart(
            labels = stamps.map { timeLabel(it, bucket) },
            lines = lines,
            bands = metric.bands(),
            beginAtZero = metric != HistoryMetric.TEMP,
            unit = metric.unit,
            decimals = metric.decimals,
            showCategory = false,
            showLegend = !many,
        )

        val baseSeries = seriesByTs(base, key)
        val results =
            ids
                .map { id ->
                    val stats = pairStats(baseSeries, seriesByTs(others[id].orEmpty(), key))
                    Triple(id, stats, verdict(stats, metric))
                }.sortedByDescending { it.second.r ?: -2.0 }
        val good = results.count { it.third.tone == VerdictTone.GOOD }
        val counted = results.count { it.third.tone != VerdictTone.NONE }
        Text(
            if (counted == 0) {
                "Not enough overlapping $label readings in this range to judge."
            } else {
                val noun =
                    if (counted == 1) {
                        "sensor"
                    } else {
                        "sensors"
                    }
                "$good of $counted $noun track $zoneName closely on $label."
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        val unit = unitSuffix(metric.unit)
        val surface = MaterialTheme.colorScheme.surface
        results.forEach { (id, stats, v) ->
            val toneColor =
                when (v.tone) {
                    VerdictTone.GOOD -> getAqiColor(25, true).asAqiText(surface)
                    VerdictTone.FAIR -> getAqiColor(75, true).asAqiText(surface)
                    VerdictTone.POOR -> getAqiColor(125, true).asAqiText(surface)
                    VerdictTone.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 6.dp)
                            .size(10.dp)
                            .background(compareColor(ids.indexOf(id), ids.size), RoundedCornerShape(100)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            nameOf(id),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(v.label, style = MaterialTheme.typography.labelMedium, color = toneColor)
                    }
                    val r = stats.r?.let { "%.2f".format(it) } ?: "--"
                    val ratio = stats.ratio?.let { "${"%.2f".format(it)}×" } ?: "--"
                    val gap = stats.gap?.let { "${formatValue(it, metric.decimals)}$unit" } ?: "--"
                    Text(
                        "Moves together $r · ratio $ratio · gap $gap · ${stats.n} hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            "Moves together is the correlation. 1 means they rise and fall at exactly the same times, 0 means no link at all. Typical ratio is their reading over $zoneName's, so 1.20× means they usually read 20% higher. Typical gap is how far apart the two readings are on average. A pair can move together closely and still sit at different levels, which usually means one sensor reads high or low rather than the air being different.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (ids.size >= 2) {
            CompareMatrix(
                names = listOf(zoneName) + ids.map(nameOf),
                maps = listOf(baseSeries) + ids.map { seriesByTs(others[it].orEmpty(), key) },
                label = label,
            )
        }
    }
}

@Composable
private fun CompareMatrix(
    names: List<String>,
    maps: List<Map<Long, Double>>,
    label: String,
) {
    val dark = isDarkSurface()
    val cell = 52.dp
    Text("Every pair", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Text(
        "How closely each pair moves together on $label, from 0 to 1. Darker means more consistent.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Column {
            Spacer(modifier = Modifier.height(cell))
            names.forEach { name ->
                Box(
                    modifier = Modifier.width(120.dp).height(cell),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        names.indices.forEach { j ->
            Column {
                Box(
                    modifier = Modifier.width(cell).height(cell).padding(2.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(names[j], style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                names.indices.forEach { i ->
                    val r =
                        if (i == j) {
                            null
                        } else {
                            val s = pairStats(maps[i], maps[j])
                            if (s.n >= COMPARE_MIN_OVERLAP) {
                                s.r
                            } else {
                                null
                            }
                        }
                    val strength = (r ?: 0.0).coerceIn(0.0, 1.0).toFloat()
                    val bg =
                        when {
                            i == j -> MaterialTheme.colorScheme.surfaceContainerHighest
                            r == null -> Color.Transparent
                            dark -> Color(0xFF3987E5).copy(alpha = 0.08f + strength * 0.7f)
                            else -> Color(0xFF2A78D6).copy(alpha = 0.06f + strength * 0.6f)
                        }
                    val fg =
                        if (strength > 0.6f) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    Box(
                        modifier =
                            Modifier
                                .size(cell)
                                .padding(1.dp)
                                .background(bg, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (i != j) {
                            Text(
                                r?.let { "%.2f".format(it) } ?: "--",
                                style = MaterialTheme.typography.labelMedium,
                                color = fg,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryLineChart(
    labels: List<String>,
    lines: List<ChartLine>,
    bands: List<Double>?,
    beginAtZero: Boolean,
    unit: String,
    decimals: Int,
    showCategory: Boolean,
    height: Dp = 220.dp,
    showLegend: Boolean = lines.size > 1,
) {
    if (labels.isEmpty()) {
        return
    }
    var selected by remember(labels, lines) { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val highlight = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val tickPaint =
        remember(tick) {
            Paint().apply {
                isAntiAlias = true
                color = tick.toArgb()
                textSize = with(density) { 10.sp.toPx() }
            }
        }
    val values = lines.flatMap { it.values }.filterNotNull()
    if (values.isEmpty()) {
        Text("No readings to plot.", style = MaterialTheme.typography.bodyMedium, color = tick)
        return
    }
    val rawMin = values.min()
    val rawMax = values.max()
    val yMin =
        if (beginAtZero) {
            0.0
        } else {
            kotlin.math.floor(rawMin - (rawMax - rawMin) * 0.1 - 1)
        }
    val yMax = maxOf(rawMax + (rawMax - yMin) * 0.08, yMin + 1)

    if (showLegend) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(width = 18.dp, height = 8.dp)) {
                        drawLine(
                            color = line.color,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect =
                                if (line.dashed) {
                                    PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                                } else {
                                    null
                                },
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(line.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    val readout =
        selected?.let { i ->
            val parts =
                lines.mapNotNull { line ->
                    val v = line.values.getOrNull(i) ?: return@mapNotNull null
                    var text = "${line.label} ${formatValue(v, decimals)}${unitSuffix(unit)}"
                    if (showCategory) {
                        text += " (${getAqiCategory(v.roundToInt(), true).label})"
                    }
                    text
                }
            "${labels[i]}  ·  ${parts.joinToString("  ·  ").ifEmpty { "no reading" }}"
        } ?: "Drag across the chart for readings"
    Text(readout, style = MaterialTheme.typography.labelMedium, color = tick, maxLines = 3)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(labels) {
                    val gutter = 36.dp.toPx()
                    fun indexAt(x: Float): Int {
                        val w = size.width - gutter
                        val fraction = (x / w).coerceIn(0f, 1f)
                        return (fraction * (labels.size - 1)).roundToInt()
                    }
                    detectTapGestures { offset ->
                        val index = indexAt(offset.x)
                        selected =
                            if (selected == index) {
                                null
                            } else {
                                index
                            }
                    }
                }.pointerInput(labels) {
                    val gutter = 36.dp.toPx()
                    fun indexAt(x: Float): Int {
                        val w = size.width - gutter
                        val fraction = (x / w).coerceIn(0f, 1f)
                        return (fraction * (labels.size - 1)).roundToInt()
                    }
                    detectDragGestures(
                        onDragStart = { offset -> selected = indexAt(offset.x) },
                        onDrag = { change, _ -> selected = indexAt(change.position.x) },
                    )
                },
    ) {
        val gutter = 36.dp.toPx()
        val bottom = 18.dp.toPx()
        val w = size.width - gutter
        val h = size.height - bottom
        fun xAt(i: Int): Float {
            if (labels.size <= 1) {
                return w / 2
            }
            return i.toFloat() / (labels.size - 1) * w
        }
        fun yAt(v: Double): Float = (h - ((v - yMin) / (yMax - yMin)) * h).toFloat()

        if (bands != null) {
            for (i in 0 until bands.size - 1) {
                val top = yAt(bands[i + 1]).coerceAtLeast(0f)
                val bot = yAt(bands[i]).coerceAtMost(h)
                if (bot > top) {
                    drawRect(
                        color = getAqiColor(US_CATEGORY_SAMPLES[i], true).copy(alpha = 0.08f),
                        topLeft = Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(w, bot - top),
                    )
                }
            }
        }

        val steps = 4
        drawIntoCanvas { canvas ->
            for (k in 0..steps) {
                val v = yMin + (yMax - yMin) * k / steps
                val y = yAt(v)
                drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                canvas.nativeCanvas.drawText(formatValue(v, if (yMax - yMin < 10) 1 else 0), w + 6.dp.toPx(), y + 4.dp.toPx(), tickPaint)
            }
            val count = minOf(4, labels.size)
            for (k in 0 until count) {
                val i =
                    if (count == 1) {
                        0
                    } else {
                        (k * (labels.size - 1)) / (count - 1)
                    }
                val text = labels[i]
                val tw = tickPaint.measureText(text)
                val x = (xAt(i) - tw / 2).coerceIn(0f, w - tw)
                canvas.nativeCanvas.drawText(text, x, size.height - 4.dp.toPx(), tickPaint)
            }
        }

        lines.forEach { line ->
            val path = Path()
            var drawing = false
            line.values.forEachIndexed { i, v ->
                if (v == null) {
                    drawing = false
                    return@forEachIndexed
                }
                val x = xAt(i)
                val y = yAt(v)
                val prev = line.values.getOrNull(i - 1)
                val next = line.values.getOrNull(i + 1)
                if (prev == null && next == null) {
                    drawCircle(line.color, radius = line.width.toPx(), center = Offset(x, y))
                }
                if (drawing) {
                    path.lineTo(x, y)
                } else {
                    path.moveTo(x, y)
                    drawing = true
                }
            }
            drawPath(
                path,
                line.color,
                style =
                    Stroke(
                        width = line.width.toPx(),
                        pathEffect =
                            if (line.dashed) {
                                PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                            } else {
                                null
                            },
                    ),
            )
        }

        selected?.let { i ->
            val x = xAt(i)
            drawLine(
                highlight.copy(alpha = 0.5f),
                Offset(x, 0f),
                Offset(x, h),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            lines.forEach { line ->
                val v = line.values.getOrNull(i) ?: return@forEach
                drawCircle(surface, radius = 5.dp.toPx(), center = Offset(x, yAt(v)))
                drawCircle(line.color, radius = 3.5.dp.toPx(), center = Offset(x, yAt(v)))
            }
        }
    }
}
