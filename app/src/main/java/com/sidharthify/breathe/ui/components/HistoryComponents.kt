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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.HistoricalDataPoint
import com.sidharthify.breathe.data.HistoricalStats
import com.sidharthify.breathe.data.HistoryState
import com.sidharthify.breathe.util.WEATHER_FILTER_LABELS
import com.sidharthify.breathe.util.WeatherPm25Group
import com.sidharthify.breathe.util.calculateUsAqi
import com.sidharthify.breathe.util.calculateUsAqiPm10
import com.sidharthify.breathe.util.filterHistoryByWeather
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.weatherConditionIconRes
import com.sidharthify.breathe.util.weatherImpactCards
import com.sidharthify.breathe.util.weatherPm25Groups
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val HistoryButtonHeight = 36.dp
private val HistoryButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)

@Composable
fun ExtendedHistoryScreen(
    zoneName: String,
    historyState: HistoryState,
    nodeKeys: List<String>,
    onBack: () -> Unit,
    onRangeSelected: (String) -> Unit,
    onToggleCustom: () -> Unit,
    onCustomRangeChanged: (String) -> Unit,
    onCustomIntervalChanged: (String) -> Unit,
    onApplyCustom: () -> Unit,
    onSensorSelected: (String) -> Unit,
    onTogglePm25: () -> Unit,
    onTogglePm10: () -> Unit,
    onDownloadCSV: () -> Unit,
    onWeatherFilterSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
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
            Text(
                "$zoneName History",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Controls row: sensor selector + PM checkboxes
        HistoryControlsBar(
            selectedSensor = historyState.selectedSensor,
            nodeKeys = nodeKeys,
            showPm25 = historyState.showPm25,
            showPm10 = historyState.showPm10,
            onSensorSelected = onSensorSelected,
            onTogglePm25 = onTogglePm25,
            onTogglePm10 = onTogglePm10,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Range selector
        HistoryRangeSelector(
            selectedRange = historyState.selectedRange,
            showCustomInputs = historyState.showCustomInputs,
            customRange = historyState.customRange,
            customInterval = historyState.customInterval,
            onRangeSelected = onRangeSelected,
            onToggleCustom = onToggleCustom,
            onCustomRangeChanged = onCustomRangeChanged,
            onCustomIntervalChanged = onCustomIntervalChanged,
            onApplyCustom = onApplyCustom,
        )

        val weatherHistory = historyState.weatherHistory
        val weatherGroups =
            remember(historyState.data, weatherHistory) {
                weatherPm25Groups(historyState.data, weatherHistory)
            }
        val filteredData =
            remember(historyState.data, weatherHistory, historyState.weatherFilter) {
                filterHistoryByWeather(
                    historyState.data,
                    weatherHistory,
                    historyState.weatherFilter,
                )
            }
        val impactCards =
            remember(historyState.data, weatherHistory) {
                weatherImpactCards(historyState.data, weatherHistory)
            }

        if (!historyState.isLoading &&
            weatherHistory != null &&
            weatherHistory.points.isNotEmpty()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            WeatherFilterChips(
                selectedFilter = historyState.weatherFilter,
                presentConditions = weatherGroups.filterValues { it.second > 0 }.keys,
                onFilterSelected = onWeatherFilterSelected,
            )
            if (impactCards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                WeatherImpactPanel(cards = impactCards)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats panel
        if (historyState.stats != null) {
            HistoryStatsPanel(stats = historyState.stats)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Chart
        if (historyState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
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
        } else if (filteredData.isNotEmpty()) {
            ExtendedHistoryPager(
                data = filteredData,
                showPm25 = historyState.showPm25,
                showPm10 = historyState.showPm10,
            )
        } else if (historyState.data.isNotEmpty()) {
            Text(
                "No samples for this weather",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDownloadCSV,
            modifier = Modifier.fillMaxWidth().height(HistoryButtonHeight),
            contentPadding = HistoryButtonPadding,
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download CSV", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(160.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryControlsBar(
    selectedSensor: String,
    nodeKeys: List<String>,
    showPm25: Boolean,
    showPm10: Boolean,
    onSensorSelected: (String) -> Unit,
    onTogglePm25: () -> Unit,
    onTogglePm10: () -> Unit,
) {
    var sensorExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (nodeKeys.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = sensorExpanded,
                onExpandedChange = { sensorExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = if (selectedSensor == "zone") "Zone Average" else selectedSensor,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sensorExpanded) },
                    modifier =
                        Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = sensorExpanded,
                    onDismissRequest = { sensorExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Zone Average") },
                        onClick = {
                            onSensorSelected("zone")
                            sensorExpanded = false
                        },
                    )
                    nodeKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key) },
                            onClick = {
                                onSensorSelected(key)
                                sensorExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ToggleButton(
                checked = showPm25,
                onCheckedChange = { checked -> if (checked != showPm25) onTogglePm25() },
                modifier = Modifier.weight(1f).height(HistoryButtonHeight),
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                contentPadding = HistoryButtonPadding,
            ) {
                Text("PM2.5", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            ToggleButton(
                checked = showPm10,
                onCheckedChange = { checked -> if (checked != showPm10) onTogglePm10() },
                modifier = Modifier.weight(1f).height(HistoryButtonHeight),
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                contentPadding = HistoryButtonPadding,
            ) {
                Text("PM10", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
fun HistoryRangeSelector(
    selectedRange: String,
    showCustomInputs: Boolean,
    customRange: String,
    customInterval: String,
    onRangeSelected: (String) -> Unit,
    onToggleCustom: () -> Unit,
    onCustomRangeChanged: (String) -> Unit,
    onCustomIntervalChanged: (String) -> Unit,
    onApplyCustom: () -> Unit,
) {
    val options = listOf("1w" to "1W", "1mo" to "1M", "6mo" to "6M", "custom" to "Custom")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (key, label) ->
            val checked =
                if (key == "custom") {
                    showCustomInputs
                } else {
                    selectedRange == key && !showCustomInputs
                }
            ToggleButton(
                checked = checked,
                onCheckedChange = { isChecked ->
                    if (!isChecked) return@ToggleButton
                    if (key == "custom") {
                        if (!showCustomInputs) onToggleCustom()
                    } else if (selectedRange != key || showCustomInputs) {
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
                "Average PM2.5 by weather condition",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
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
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val diffColor = if (card.diffPct <= 0) colorScheme.primary else colorScheme.tertiary
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
            "${card.label} (${card.hours}h of data)",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            "${"%.1f".format(card.avg)} µg/m³",
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
fun HistoryStatsPanel(stats: HistoricalStats) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatItem("Max PM2.5", stats.maxPm25, Modifier.weight(1f))
                StatItem("Min PM2.5", stats.minPm25, Modifier.weight(1f))
                StatItem("Avg PM2.5", stats.avgPm25, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatItem("Max PM10", stats.maxPm10, Modifier.weight(1f))
                StatItem("Min PM10", stats.minPm10, Modifier.weight(1f))
                StatItem("Avg PM10", stats.avgPm10, Modifier.weight(1f))
            }
        }
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

@Composable
private fun StatItem(label: String, value: Double?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "--",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ExtendedHistoryChart(
    modifier: Modifier = Modifier,
    data: List<HistoricalDataPoint>,
    showPm25: Boolean = true,
    showPm10: Boolean = true,
) {
    if (data.isEmpty()) return

    val pm25Color = Color(0xFFA8C7FA)
    val pm10Color = Color(0xFFD8B4FE)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val highlightColorArgb = highlightColor.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceColorArgb = surfaceColor.toArgb()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val pm25Path = remember { Path() }
    val pm10Path = remember { Path() }
    val timeFormatter = remember { SimpleDateFormat("d/M HH:00", Locale.getDefault()) }

    val axisTextPaint = remember {
        Paint().apply {
            textSize = 28f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }
    }

    val tooltipTextPaint = remember {
        Paint().apply {
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
    }

    val tooltipBgPaint = remember {
        Paint().apply {
            setShadowLayer(12f, 0f, 4f, android.graphics.Color.argb(50, 0, 0, 0))
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Extended History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (showPm25) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(pm25Color, RoundedCornerShape(2.dp)),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PM2.5", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (showPm10) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(pm10Color, RoundedCornerShape(2.dp)),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PM10", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val labelWidth = with(density) { 40.dp.toPx() }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(data) {
                        detectTapGestures(
                            onPress = { offset ->
                                val graphWidth = size.width.toFloat() - labelWidth
                                val touchX = (offset.x - labelWidth).coerceAtLeast(0f)
                                val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                val index = (fraction * (data.size - 1)).roundToInt()
                                selectedIndex = index
                                tryAwaitRelease()
                                selectedIndex = null
                            },
                        )
                    }
                    .pointerInput(data) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val graphWidth = size.width.toFloat() - labelWidth
                                val touchX = (offset.x - labelWidth).coerceAtLeast(0f)
                                val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                val index = (fraction * (data.size - 1)).roundToInt()
                                if (index != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = index
                                }
                            },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null },
                            onHorizontalDrag = { change, _ ->
                                val graphWidth = size.width.toFloat() - labelWidth
                                val touchX = (change.position.x - labelWidth).coerceAtLeast(0f)
                                val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                val index = (fraction * (data.size - 1)).roundToInt()
                                if (index != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = index
                                }
                            },
                        )
                    },
            ) {
                val width = size.width - labelWidth
                val height = size.height

                // Calculate max value across all visible datasets
                var maxVal = 0.0
                data.forEach { pt ->
                    if (showPm25 && pt.pm25 != null && pt.pm25 > maxVal) maxVal = pt.pm25
                    if (showPm10 && pt.pm10 != null && pt.pm10 > maxVal) maxVal = pt.pm10
                }
                if (maxVal < 10.0) maxVal = 10.0

                fun getX(index: Int): Float = labelWidth + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * width
                fun getY(value: Double): Float = height - ((value / maxVal) * height).toFloat()

                // Draw PM2.5 line
                if (showPm25) {
                    pm25Path.rewind()
                    var started = false
                    data.forEachIndexed { i, pt ->
                        val v = pt.pm25 ?: return@forEachIndexed
                        val x = getX(i)
                        val y = getY(v)
                        if (!started) { pm25Path.moveTo(x, y); started = true }
                        else {
                            val prevX = getX(i - 1)
                            val prevY = getY(data[i - 1].pm25 ?: v)
                            val cx = prevX + (x - prevX) / 2
                            pm25Path.cubicTo(cx, prevY, cx, y, x, y)
                        }
                    }
                    // Fill
                    val fillPath25 = Path().apply {
                        addPath(pm25Path)
                        lineTo(getX(data.size - 1), height)
                        lineTo(labelWidth, height)
                        close()
                    }
                    drawPath(fillPath25, Brush.verticalGradient(listOf(pm25Color.copy(alpha = 0.3f), pm25Color.copy(alpha = 0f))))
                    drawPath(pm25Path, pm25Color, style = Stroke(width = 3.dp.toPx()))
                }

                // Draw PM10 line
                if (showPm10) {
                    pm10Path.rewind()
                    var started = false
                    data.forEachIndexed { i, pt ->
                        val v = pt.pm10 ?: return@forEachIndexed
                        val x = getX(i)
                        val y = getY(v)
                        if (!started) { pm10Path.moveTo(x, y); started = true }
                        else {
                            val prevX = getX(i - 1)
                            val prevY = getY(data[i - 1].pm10 ?: v)
                            val cx = prevX + (x - prevX) / 2
                            pm10Path.cubicTo(cx, prevY, cx, y, x, y)
                        }
                    }
                    val fillPath10 = Path().apply {
                        addPath(pm10Path)
                        lineTo(getX(data.size - 1), height)
                        lineTo(labelWidth, height)
                        close()
                    }
                    drawPath(fillPath10, Brush.verticalGradient(listOf(pm10Color.copy(alpha = 0.3f), pm10Color.copy(alpha = 0f))))
                    drawPath(pm10Path, pm10Color, style = Stroke(width = 3.dp.toPx()))
                }

                // Axis labels
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    axisTextPaint.color = labelColor
                    axisTextPaint.textAlign = Paint.Align.LEFT

                    nativeCanvas.drawText("${maxVal.toInt()}", 0f, 28f, axisTextPaint)
                    nativeCanvas.drawText("${(maxVal / 2).toInt()}", 0f, height / 2 + 10f, axisTextPaint)
                    nativeCanvas.drawText("0", 0f, height - 6f, axisTextPaint)

                    // Time labels
                    if (selectedIndex == null && data.size > 2) {
                        axisTextPaint.textAlign = Paint.Align.CENTER
                        val indices = listOf(0, data.size / 2, data.size - 1)
                        indices.forEach { i ->
                            if (i < data.size) {
                                val date = Date(data[i].ts * 1000)
                                val label = timeFormatter.format(date)
                                axisTextPaint.textAlign = when (i) {
                                    0 -> Paint.Align.LEFT
                                    data.size - 1 -> Paint.Align.RIGHT
                                    else -> Paint.Align.CENTER
                                }
                                nativeCanvas.drawText(label, getX(i), height + 40f, axisTextPaint)
                            }
                        }
                    }

                    // Tooltip
                    selectedIndex?.let { idx ->
                        if (idx in data.indices) {
                            val pt = data[idx]
                            val x = getX(idx)
                            val timeStr = timeFormatter.format(Date(pt.ts * 1000))

                            // Vertical line
                            drawLine(
                                color = highlightColor.copy(alpha = 0.5f),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                            )

                            val parts = mutableListOf<String>()
                            if (showPm25 && pt.pm25 != null) {
                                parts.add("PM2.5: ${String.format(Locale.getDefault(), "%.1f", pt.pm25)}")
                            }
                            if (showPm10 && pt.pm10 != null) {
                                parts.add("PM10: ${String.format(Locale.getDefault(),"%.1f", pt.pm10)}")
                            }
                            val label = "${parts.joinToString("  ")} @ $timeStr"

                            tooltipTextPaint.color = highlightColorArgb
                            val textWidth = tooltipTextPaint.measureText(label)
                            val padding = 16f
                            val boxWidth = textWidth + padding * 2
                            val boxHeight = 60f

                            var boxX = x - boxWidth / 2
                            if (boxX < labelWidth) boxX = labelWidth
                            if (boxX + boxWidth > size.width) boxX = size.width - boxWidth
                            val boxY = -50f

                            tooltipBgPaint.color = surfaceColorArgb
                            nativeCanvas.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 12f, 12f, tooltipBgPaint)
                            nativeCanvas.drawText(label, boxX + padding, boxY + boxHeight - 20f, tooltipTextPaint)

                            // Dots on data points
                            if (showPm25 && pt.pm25 != null) {
                                val y = getY(pt.pm25)
                                drawCircle(surfaceColor, radius = 5.dp.toPx(), center = Offset(x, y))
                                drawCircle(pm25Color, radius = 3.dp.toPx(), center = Offset(x, y))
                            }
                            if (showPm10 && pt.pm10 != null) {
                                val y = getY(pt.pm10)
                                drawCircle(surfaceColor, radius = 5.dp.toPx(), center = Offset(x, y))
                                drawCircle(pm10Color, radius = 3.dp.toPx(), center = Offset(x, y))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Y-axis label
            Text(
                "Concentration (µg/m³)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ExtendedHistoryPager(
    data: List<HistoricalDataPoint>,
    showPm25: Boolean,
    showPm10: Boolean,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                pageSpacing = 16.dp,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
                modifier = Modifier.height(340.dp),
            ) { page ->
                when (page) {
                    0 -> ExtendedHistoryChart(
                        data = data,
                        showPm25 = showPm25,
                        showPm10 = showPm10,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> ExtendedDotHistoryChart(
                        data = data,
                        showPm25 = showPm25,
                        showPm10 = showPm10,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) { i ->
                val active = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (active) 8.dp else 6.dp)
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            },
                            CircleShape,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (pagerState.currentPage == 0) "Swipe for Dots History" else "Swipe for Trend Graph",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
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
) {
    if (data.isEmpty()) return

    val use25 = showPm25 || (!showPm10)
    val use10 = showPm10 || (!showPm25)
    val title = when {
        use25 && use10 -> "PM2.5 & PM10 Grid"
        use25 -> "PM2.5 Grid"
        else -> "PM10 Grid"
    }

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
    val topLabelPx = 22f
    val bottomLabelPx = 30f
    val haptic = LocalHapticFeedback.current

    val axisTextPaint = remember {
        Paint().apply {
            textSize = 24f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }
    }
    val tooltipTextPaint = remember {
        Paint().apply {
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
    }
    val tooltipBgPaint = remember {
        Paint().apply {
            setShadowLayer(12f, 0f, 4f, android.graphics.Color.argb(50, 0, 0, 0))
        }
    }

    var selected by remember { mutableStateOf<DotCell?>(null) }

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
            val cellW = ((w - gutterPx - gapPx * (cols - 1)) / cols).coerceAtLeast(1f)
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
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (hourly) "By Day and Hour  ·  Tap a Cell for Details" else "Daily Average  ·  Tap a Cell for Details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                    val cellW = ((size.width - gutterPx - gapPx * (cols - 1)) / cols).coerceAtLeast(1f)
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
                                nativeCanvas.drawText(weekdayFormatter.format(Date(day)), 0f, top + cellH / 2 + 8f, axisTextPaint)
                            }
                        }
                        axisTextPaint.textAlign = Paint.Align.CENTER
                        val gridBottom = rows * (cellH + gapPx)
                        listOf(0, 6, 12, 18).forEach { c ->
                            val x = gutterPx + c * (cellW + gapPx) + cellW / 2
                            nativeCanvas.drawText("$c", x, gridBottom + 22f, axisTextPaint)
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
                                nativeCanvas.drawText(weekdayFormatter.format(cal.time), 0f, topLabelPx + row * (cell + gapPx) + cell / 2 + 8f, axisTextPaint)
                            }
                        }
                        var lastMonth = ""
                        for (col in 0 until numCols) {
                            cal.timeInMillis = gridStart + col.toLong() * 7 * dayMillis
                            val month = monthFormatter.format(cal.time)
                            if (month != lastMonth) {
                                nativeCanvas.drawText(month, gutterPx + col * (cell + gapPx), 16f, axisTextPaint)
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
                        val pad = 18f
                        val boxW = textW + pad * 2
                        val boxH = 60f
                        var boxX = sel.left + sel.w / 2 - boxW / 2
                        if (boxX < 0f) boxX = 0f
                        if (boxX + boxW > size.width) boxX = size.width - boxW
                        var boxY = sel.top - boxH - 8f
                        if (boxY < 0f) boxY = sel.top + sel.h + 8f
                        tooltipBgPaint.color = tooltipBgArgb
                        nativeCanvas.drawRoundRect(boxX, boxY, boxX + boxW, boxY + boxH, 14f, 14f, tooltipBgPaint)
                        nativeCanvas.drawText(sel.label, boxX + pad, boxY + boxH - 20f, tooltipTextPaint)
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
