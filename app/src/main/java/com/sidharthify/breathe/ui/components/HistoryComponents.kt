// SPDX-License-Identifier: MIT
/*
 * HistoryComponents.kt - Composable components for the Extended History view
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
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidharthify.breathe.data.HistoricalDataPoint
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.util.HistoryPeriod
import com.sidharthify.breathe.util.HistoryPeriodMode
import com.sidharthify.breathe.util.WEATHER_FILTER_LABELS
import com.sidharthify.breathe.util.WeatherPm25Group
import com.sidharthify.breathe.util.asAqiText
import com.sidharthify.breathe.util.calculateUsAqi
import com.sidharthify.breathe.util.calculateUsAqiPm10
import com.sidharthify.breathe.util.filterHistoryByWeather
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.weatherConditionIconRes
import com.sidharthify.breathe.util.weatherImpactCards
import com.sidharthify.breathe.util.weatherPm25Groups
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import kotlin.math.roundToInt

private val HistoryButtonHeight = 36.dp
private val HistoryButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)

@Composable
fun HistoryRangeSelector(
    selectedRange: String,
    showCustomInputs: Boolean,
    customRange: String,
    customInterval: String,
    periodActive: Boolean,
    periodMode: HistoryPeriodMode,
    period: HistoryPeriod?,
    onShowPeriod: () -> Unit,
    onPeriodModeSelected: (HistoryPeriodMode) -> Unit,
    onPeriodStep: (Long) -> Unit,
    onPeriodPicked: (LocalDate, LocalDate) -> Unit,
    onRangeSelected: (String) -> Unit,
    onToggleCustom: () -> Unit,
    onCustomRangeChanged: (String) -> Unit,
    onCustomIntervalChanged: (String) -> Unit,
    onApplyCustom: () -> Unit,
) {
    val options = listOf("1w" to "1W", "1mo" to "1M", "6mo" to "6M", "date" to "Date", "custom" to "Custom")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (key, label) ->
            val checked =
                when (key) {
                    "custom" -> showCustomInputs
                    "date" -> periodActive
                    else -> selectedRange == key && !showCustomInputs && !periodActive
                }
            ToggleButton(
                checked = checked,
                onCheckedChange = { isChecked ->
                    if (!isChecked) return@ToggleButton
                    if (key == "custom") {
                        if (!showCustomInputs) onToggleCustom()
                    } else if (key == "date") {
                        if (!periodActive) onShowPeriod()
                    } else if (selectedRange != key || showCustomInputs || periodActive) {
                        onRangeSelected(key)
                    }
                },
                modifier = Modifier.weight(1f).height(HistoryButtonHeight),
                shapes = connectedToggleShapes(index, options.size),
                contentPadding = HistoryButtonPadding,
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }

    AnimatedVisibility(
        visible = periodActive && period != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        if (period != null) {
            HistoryPeriodPicker(
                mode = periodMode,
                period = period,
                onModeSelected = onPeriodModeSelected,
                onStep = onPeriodStep,
                onPicked = onPeriodPicked,
            )
        }
    }

    AnimatedVisibility(
        visible = showCustomInputs,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactHistoryTextField(
                    value = customRange,
                    onValueChange = onCustomRangeChanged,
                    placeholder = "Range",
                    modifier = Modifier.weight(1f),
                )
                CompactHistoryTextField(
                    value = customInterval,
                    onValueChange = onCustomIntervalChanged,
                    placeholder = "Interval",
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = onApplyCustom,
                    modifier = Modifier.weight(1f).height(HistoryButtonHeight),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = HistoryButtonPadding,
                ) {
                    Text("Apply", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private object UpToToday : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        !utcTimeMillis.toUtcLocalDate().isAfter(LocalDate.now())

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

@Composable
private fun HistoryPeriodPicker(
    mode: HistoryPeriodMode,
    period: HistoryPeriod,
    onModeSelected: (HistoryPeriodMode) -> Unit,
    onStep: (Long) -> Unit,
    onPicked: (LocalDate, LocalDate) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val modes = HistoryPeriodMode.entries

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                modes.forEachIndexed { index, option ->
                    ToggleButton(
                        checked = mode == option,
                        onCheckedChange = { isChecked ->
                            if (isChecked && mode != option) {
                                onModeSelected(option)
                            }
                        },
                        modifier = Modifier.weight(1f).height(HistoryButtonHeight),
                        shapes = connectedToggleShapes(index, modes.size),
                        contentPadding = HistoryButtonPadding,
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onStep(-1L) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous period",
                    )
                }
                TextButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        period.label(mode),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(
                    onClick = { onStep(1L) },
                    enabled = !period.shifted(mode, 1L).start.isAfter(LocalDate.now()),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next period",
                    )
                }
            }
        }
    }

    if (showDialog && mode == HistoryPeriodMode.DAYS) {
        val state =
            rememberDateRangePickerState(
                initialSelectedStartDateMillis = period.start.toUtcMillis(),
                initialSelectedEndDateMillis = period.end.toUtcMillis(),
                selectableDates = UpToToday,
            )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val startMillis = state.selectedStartDateMillis
                        if (startMillis != null) {
                            val start = startMillis.toUtcLocalDate()
                            val end = state.selectedEndDateMillis?.toUtcLocalDate() ?: start
                            onPicked(start, end)
                        }
                        showDialog = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f))
        }
    } else if (showDialog) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = period.start.toUtcMillis(),
                selectableDates = UpToToday,
            )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val date = millis.toUtcLocalDate()
                            onPicked(date, date)
                        }
                        showDialog = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun WeatherFilterChips(
    selectedFilter: String,
    presentConditions: Set<String>,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        buildList {
            add("all" to "All")
            WEATHER_FILTER_LABELS.forEach { (condition, label) ->
                if (condition in presentConditions) add(condition to label)
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Filter by weather during that time",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { index, (condition, label) ->
                ToggleButton(
                    checked = selectedFilter == condition,
                    onCheckedChange = { isChecked ->
                        if (isChecked && selectedFilter != condition) {
                            onFilterSelected(condition)
                        }
                    },
                    modifier = Modifier.weight(1f).height(HistoryButtonHeight),
                    shapes = connectedToggleShapes(index, options.size),
                    contentPadding = HistoryButtonPadding,
                ) {
                    Icon(
                        painter = painterResource(weatherConditionIconRes(condition)),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun WeatherImpactPanel(
    cards: List<WeatherPm25Group>,
    title: String,
    unit: String,
    decimals: Int,
    judged: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Weather comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            cards.chunked(2).forEachIndexed { index, rowCards ->
                if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowCards.forEach { card ->
                        WeatherImpactStat(
                            card = card,
                            unit = unit,
                            decimals = decimals,
                            judged = judged,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowCards.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherImpactStat(
    card: WeatherPm25Group,
    unit: String,
    decimals: Int,
    judged: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val diffColor =
        when {
            !judged -> colorScheme.onSurfaceVariant
            card.diffPct <= 0 -> getAqiColor(25, true).asAqiText(colorScheme.surface)
            else -> getAqiColor(175, true).asAqiText(colorScheme.surface)
        }
    val diffText =
        when {
            card.diffPct == 0 -> "same as average"
            card.diffPct > 0 -> "+${card.diffPct}% vs average"
            else -> "${card.diffPct}% vs average"
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            "${card.label} · ${card.hours}h",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            "${formatValue(card.avg, decimals)}${unitSuffix(unit)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
        )
        Text(
            diffText,
            style = MaterialTheme.typography.labelSmall,
            color = diffColor,
        )
    }
}

@Composable
private fun CompactHistoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    val textStyle =
        MaterialTheme.typography.labelMedium.merge(
            TextStyle(color = MaterialTheme.colorScheme.onSurface),
        )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(HistoryButtonHeight),
        singleLine = true,
        textStyle = textStyle,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = colors,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = MaterialTheme.shapes.small,
                    )
                },
            )
        },
    )
}

@Composable
private fun connectedToggleShapes(index: Int, count: Int) =
    when (index) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }

private data class DotCell(
    val left: Float,
    val top: Float,
    val w: Float,
    val h: Float,
    val label: String,
)

@Composable
fun ExtendedDotHistoryChart(
    data: List<HistoricalDataPoint>,
    showPm25: Boolean,
    showPm10: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Day by day",
) {
    if (data.isEmpty()) return

    val use25 = showPm25 || (!showPm10)
    val use10 = showPm10 || (!showPm25)

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val emptyCellColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val highlightColorArgb = highlightColor.toArgb()
    val tooltipBgArgb = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()

    val weekdayFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val monthFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dayLabelFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val hourLabelFormatter = remember { SimpleDateFormat("EEE HH:00", Locale.getDefault()) }
    val dayMillis = 24L * 60L * 60L * 1000L

    val grid = remember(data) {
        val cal = Calendar.getInstance()
        val byDay = sortedMapOf<Long, Pair<DoubleArray, DoubleArray>>()
        val counts = HashMap<Long, Pair<IntArray, IntArray>>()
        data.forEach { pt ->
            cal.timeInMillis = pt.ts * 1000L
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val arr = byDay.getOrPut(dayStart) { Pair(DoubleArray(24) { Double.NaN }, DoubleArray(24) { Double.NaN }) }
            val cnt = counts.getOrPut(dayStart) { Pair(IntArray(24), IntArray(24)) }
            pt.pm25?.let { v ->
                arr.first[hour] = if (arr.first[hour].isNaN()) v else (arr.first[hour] * cnt.first[hour] + v) / (cnt.first[hour] + 1)
                cnt.first[hour]++
            }
            pt.pm10?.let { v ->
                arr.second[hour] = if (arr.second[hour].isNaN()) v else (arr.second[hour] * cnt.second[hour] + v) / (cnt.second[hour] + 1)
                cnt.second[hour]++
            }
        }
        byDay
    }
    val days = remember(grid) { grid.keys.toList() }
    val hourly = days.size in 1..10

    val dayAvg = remember(grid) {
        fun avg(arr: DoubleArray): Double {
            var sum = 0.0
            var n = 0
            for (x in arr) if (!x.isNaN()) {
                sum += x
                n++
            }
            return if (n == 0) Double.NaN else sum / n
        }
        grid.mapValues { (_, pair) -> Pair(avg(pair.first), avg(pair.second)) }
    }
    val gridStart = remember(days) {
        if (days.isEmpty()) {
            0L
        } else {
            val cal = Calendar.getInstance()
            cal.timeInMillis = days.first()
            cal.add(Calendar.DAY_OF_MONTH, -(cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }
    val numCols = remember(days, gridStart) {
        if (days.isEmpty()) 0 else (((days.last() - gridStart) / dayMillis) / 7).toInt() + 1
    }

    val density = LocalDensity.current
    val gutterPx = with(density) { 34.dp.toPx() }
    val gapPx = with(density) { 3.dp.toPx() }
    val radiusPx = with(density) { 2.dp.toPx() }
    val minLabelPx = with(density) { 11.dp.toPx() }
    val topLabelPx = with(density) { 8.dp.toPx() }
    val bottomLabelPx = with(density) { 11.dp.toPx() }
    val haptic = LocalHapticFeedback.current

    val axisTextPaint = remember {
        Paint().apply {
            textSize = with(density) { 9.sp.toPx() }
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }
    }
    val tooltipTextPaint = remember {
        Paint().apply {
            textSize = with(density) { 11.sp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
    }
    val tooltipBgPaint = remember {
        Paint().apply {
            setShadowLayer(with(density) { 4.5.dp.toPx() }, 0f, with(density) { 1.5.dp.toPx() }, android.graphics.Color.argb(50, 0, 0, 0))
        }
    }

    var selected by remember { mutableStateOf<DotCell?>(null) }
    val maxCellPx = with(density) { 22.dp.toPx() }
    var gridWidthPx by remember { mutableFloatStateOf(with(density) { 320.dp.toPx() }) }

    fun cellAqi(pm25v: Double, pm10v: Double): Int? {
        var aqi: Int? = null
        if (use25 && !pm25v.isNaN()) aqi = calculateUsAqi(pm25v)
        if (use10 && !pm10v.isNaN()) {
            val a = calculateUsAqiPm10(pm10v)
            if (aqi == null || a > aqi) aqi = a
        }
        return aqi
    }

    fun cellValues(pm25v: Double, pm10v: Double): String {
        val parts = mutableListOf<String>()
        if (use25 && !pm25v.isNaN()) parts.add("PM2.5 ${pm25v.roundToInt()}")
        if (use10 && !pm10v.isNaN()) parts.add("PM10 ${pm10v.roundToInt()}")
        return parts.joinToString("  ·  ")
    }

    fun hitTest(x: Float, y: Float, w: Float, h: Float): DotCell? {
        if (days.isEmpty()) return null
        if (hourly) {
            val rows = days.size
            val cols = 24
            val cellW = ((w - gutterPx - gapPx * (cols - 1)) / cols).coerceIn(1f, maxCellPx)
            val cellH = minOf(cellW * 1.5f, (h - bottomLabelPx - gapPx * (rows - 1)) / rows).coerceAtLeast(1f)
            val col = ((x - gutterPx) / (cellW + gapPx)).toInt()
            val row = (y / (cellH + gapPx)).toInt()
            if (col !in 0 until cols || row !in 0 until rows) return null
            val pair = grid[days[row]] ?: return null
            cellAqi(pair.first[col], pair.second[col]) ?: return null
            val cal = Calendar.getInstance()
            cal.timeInMillis = days[row]
            cal.set(Calendar.HOUR_OF_DAY, col)
            val label = "${cellValues(pair.first[col], pair.second[col])}  ·  ${hourLabelFormatter.format(cal.time)}"
            return DotCell(gutterPx + col * (cellW + gapPx), row * (cellH + gapPx), cellW, cellH, label)
        } else {
            if (numCols == 0) return null
            val cell = minOf(
                (w - gutterPx - gapPx * (numCols - 1)) / numCols,
                (h - topLabelPx - gapPx * 6) / 7f,
            ).coerceAtLeast(1f)
            val col = ((x - gutterPx) / (cell + gapPx)).toInt()
            val row = ((y - topLabelPx) / (cell + gapPx)).toInt()
            if (col !in 0 until numCols || row !in 0 until 7) return null
            val day = gridStart + (col.toLong() * 7 + row) * dayMillis
            val pair = dayAvg[day] ?: return null
            cellAqi(pair.first, pair.second) ?: return null
            val label = "${cellValues(pair.first, pair.second)}  ·  ${dayLabelFormatter.format(Date(day))}"
            return DotCell(gutterPx + col * (cell + gapPx), topLabelPx + row * (cell + gapPx), cell, cell, label)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (hourly) "Each square is one hour, tap for the reading" else "Each square is one day, tap for the reading",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            val canvasHeight =
                with(density) {
                    val widthPx = gridWidthPx
                    if (hourly) {
                        val cellW = ((widthPx - gutterPx - gapPx * 23) / 24f).coerceIn(1f, maxCellPx)
                        (days.size * (cellW * 1.5f + gapPx) + bottomLabelPx + 4f).toDp()
                    } else if (numCols > 0) {
                        val cell = ((widthPx - gutterPx - gapPx * (numCols - 1)) / numCols).coerceIn(1f, 28.dp.toPx())
                        (topLabelPx + 7 * (cell + gapPx)).toDp()
                    } else {
                        0.dp
                    }
                }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { gridWidthPx = it.width.toFloat() }
                    .height(canvasHeight)
                    .pointerInput(days, hourly, use25, use10) {
                        detectTapGestures { offset ->
                            val hit = hitTest(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                            if (hit == selected) {
                                selected = null
                            } else {
                                if (hit != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selected = hit
                            }
                        }
                    },
            ) {
                if (days.isEmpty()) return@Canvas

                if (hourly) {
                    val rows = days.size
                    val cols = 24
                    val cellW = ((size.width - gutterPx - gapPx * (cols - 1)) / cols).coerceIn(1f, maxCellPx)
                    val cellH = minOf(cellW * 1.5f, (size.height - bottomLabelPx - gapPx * (rows - 1)) / rows).coerceAtLeast(1f)

                    days.forEachIndexed { r, day ->
                        val pair = grid[day] ?: return@forEachIndexed
                        val top = r * (cellH + gapPx)
                        for (c in 0 until cols) {
                            val aqi = cellAqi(pair.first[c], pair.second[c])
                            val color = if (aqi == null) emptyCellColor else getAqiColor(aqi, true)
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(gutterPx + c * (cellW + gapPx), top),
                                size = Size(cellW, cellH),
                                cornerRadius = CornerRadius(radiusPx, radiusPx),
                            )
                        }
                    }

                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        axisTextPaint.color = labelColor
                        axisTextPaint.textAlign = Paint.Align.LEFT
                        if (cellH >= minLabelPx) {
                            days.forEachIndexed { r, day ->
                                val top = r * (cellH + gapPx)
                                nativeCanvas.drawText(weekdayFormatter.format(Date(day)), 0f, top + cellH / 2 + 3.dp.toPx(), axisTextPaint)
                            }
                        }
                        axisTextPaint.textAlign = Paint.Align.CENTER
                        val gridBottom = rows * (cellH + gapPx)
                        listOf(0, 6, 12, 18).forEach { c ->
                            val x = gutterPx + c * (cellW + gapPx) + cellW / 2
                            nativeCanvas.drawText("$c", x, gridBottom + 8.dp.toPx(), axisTextPaint)
                        }
                    }
                } else {
                    if (numCols == 0) return@Canvas
                    val cell = minOf(
                        (size.width - gutterPx - gapPx * (numCols - 1)) / numCols,
                        (size.height - topLabelPx - gapPx * 6) / 7f,
                    ).coerceAtLeast(1f)

                    for (col in 0 until numCols) {
                        for (row in 0 until 7) {
                            drawRoundRect(
                                color = emptyCellColor,
                                topLeft = Offset(gutterPx + col * (cell + gapPx), topLabelPx + row * (cell + gapPx)),
                                size = Size(cell, cell),
                                cornerRadius = CornerRadius(radiusPx, radiusPx),
                            )
                        }
                    }

                    dayAvg.forEach { (day, pair) ->
                        val aqi = cellAqi(pair.first, pair.second) ?: return@forEach
                        val daysSince = ((day - gridStart) / dayMillis).toInt()
                        val col = daysSince / 7
                        val row = daysSince % 7
                        if (col in 0 until numCols && row in 0..6) {
                            drawRoundRect(
                                color = getAqiColor(aqi, true),
                                topLeft = Offset(gutterPx + col * (cell + gapPx), topLabelPx + row * (cell + gapPx)),
                                size = Size(cell, cell),
                                cornerRadius = CornerRadius(radiusPx, radiusPx),
                            )
                        }
                    }

                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val cal = Calendar.getInstance()
                        axisTextPaint.color = labelColor
                        axisTextPaint.textAlign = Paint.Align.LEFT
                        if (cell >= minLabelPx) {
                            listOf(1, 3, 5).forEach { row ->
                                cal.timeInMillis = gridStart + row * dayMillis
                                nativeCanvas.drawText(weekdayFormatter.format(cal.time), 0f, topLabelPx + row * (cell + gapPx) + cell / 2 + 3.dp.toPx(), axisTextPaint)
                            }
                        }
                        var lastMonth = ""
                        for (col in 0 until numCols) {
                            cal.timeInMillis = gridStart + col.toLong() * 7 * dayMillis
                            val month = monthFormatter.format(cal.time)
                            if (month != lastMonth) {
                                nativeCanvas.drawText(month, gutterPx + col * (cell + gapPx), 6.dp.toPx(), axisTextPaint)
                                lastMonth = month
                            }
                        }
                    }
                }

                selected?.let { sel ->
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(sel.left, sel.top),
                        size = Size(sel.w, sel.h),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        tooltipTextPaint.color = highlightColorArgb
                        val textW = tooltipTextPaint.measureText(sel.label)
                        val pad = 7.dp.toPx()
                        val boxW = textW + pad * 2
                        val boxH = 23.dp.toPx()
                        var boxX = sel.left + sel.w / 2 - boxW / 2
                        if (boxX < 0f) boxX = 0f
                        if (boxX + boxW > size.width) boxX = size.width - boxW
                        var boxY = sel.top - boxH - 3.dp.toPx()
                        if (boxY < 0f) boxY = sel.top + sel.h + 3.dp.toPx()
                        tooltipBgPaint.color = tooltipBgArgb
                        nativeCanvas.drawRoundRect(boxX, boxY, boxX + boxW, boxY + boxH, 5.dp.toPx(), 5.dp.toPx(), tooltipBgPaint)
                        nativeCanvas.drawText(sel.label, boxX + pad, boxY + boxH - 7.5.dp.toPx(), tooltipTextPaint)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val legendReps = listOf(25, 75, 125, 175, 250, 400)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Good",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    legendReps.forEach { rep ->
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 6.dp)
                                .background(getAqiColor(rep, true), RoundedCornerShape(2.dp)),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Hazardous",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
