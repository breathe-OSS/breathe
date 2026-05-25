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

package com.sidharthify.breathe.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidharthify.breathe.data.HistoricalDataPoint
import com.sidharthify.breathe.data.HistoricalStats
import com.sidharthify.breathe.data.HistoryState
import com.sidharthify.breathe.data.NodeReading
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "$zoneName History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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
                CircularProgressIndicator()
            }
        } else if (historyState.error != null) {
            Text(
                historyState.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        } else if (historyState.data.isNotEmpty()) {
            ExtendedHistoryChart(
                data = historyState.data,
                showPm25 = historyState.showPm25,
                showPm10 = historyState.showPm10,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Download CSV button
        OutlinedButton(
            onClick = onDownloadCSV,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download CSV")
        }

        Spacer(modifier = Modifier.height(100.dp))
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Sensor dropdown
        if (nodeKeys.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = sensorExpanded,
                onExpandedChange = { sensorExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = if (selectedSensor == "zone") "Zone Average" else selectedSensor,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sensorExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).height(48.dp),
                )
                ExposedDropdownMenu(
                    expanded = sensorExpanded,
                    onDismissRequest = { sensorExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Zone Average", fontWeight = if (selectedSensor == "zone") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onSensorSelected("zone"); sensorExpanded = false },
                    )
                    nodeKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key, fontWeight = if (selectedSensor == key) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { onSensorSelected(key); sensorExpanded = false },
                        )
                    }
                }
            }
        }

        // PM checkboxes
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showPm25, onCheckedChange = { onTogglePm25() })
            Text("PM2.5", style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showPm10, onCheckedChange = { onTogglePm10() })
            Text("PM10", style = MaterialTheme.typography.labelMedium)
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
    val presets = listOf("1w" to "1 Week", "1mo" to "1 Month", "6mo" to "6 Months")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { (key, label) ->
            val isSelected = selectedRange == key && !showCustomInputs
            if (isSelected) {
                FilledTonalButton(
                    onClick = { onRangeSelected(key) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            } else {
                OutlinedButton(
                    onClick = { onRangeSelected(key) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }

        if (showCustomInputs) {
            FilledTonalButton(
                onClick = onToggleCustom,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Custom", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        } else {
            OutlinedButton(
                onClick = onToggleCustom,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Custom", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }

    AnimatedVisibility(visible = showCustomInputs) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customRange,
                    onValueChange = onCustomRangeChanged,
                    label = { Text("Range", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                )
                OutlinedTextField(
                    value = customInterval,
                    onValueChange = onCustomIntervalChanged,
                    label = { Text("Interval", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                )
                FilledTonalButton(
                    onClick = onApplyCustom,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text("Apply", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun HistoryStatsPanel(stats: HistoricalStats) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
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
            value?.let { String.format("%.1f", it) } ?: "--",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ExtendedHistoryChart(
    data: List<HistoricalDataPoint>,
    showPm25: Boolean = true,
    showPm10: Boolean = true,
    modifier: Modifier = Modifier,
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
        shape = RoundedCornerShape(24.dp),
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
                            if (showPm25 && pt.pm25 != null) parts.add("PM2.5: ${String.format("%.1f", pt.pm25)}")
                            if (showPm10 && pt.pm10 != null) parts.add("PM10: ${String.format("%.1f", pt.pm10)}")
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
