// SPDX-License-Identifier: MIT
/*
 * GraphComponents.kt - Composable components for displaying graphs and charts
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.HistoryPoint
import com.sidharthify.breathe.data.NodeHistoryPoint
import com.sidharthify.breathe.data.NodeReading
import com.sidharthify.breathe.util.getAqiColor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Helper to convert NodeHistoryPoint list
private fun nodeHistoryValues(history: List<NodeHistoryPoint>, isUsAqi: Boolean): List<Int> =
    history.map { if (!isUsAqi) (it.usAqi ?: it.aqi) else it.aqi }

@Composable
fun AqiHistoryGraph(
    history: List<HistoryPoint>,
    modifier: Modifier = Modifier,
    isUsAqi: Boolean = false,
    nodes: Map<String, NodeReading>? = null,
) {
    if (history.isEmpty()) return

    // State for which node
    var selectedNodeKey by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Compute the node keys that actually have non-empty history
    val nodeKeysWithHistory = remember(nodes) {
        nodes?.filterValues { it.history?.isNotEmpty() == true }?.keys?.toList() ?: emptyList()
    }

    // Determine the active data set
    val values: List<Int> = remember(history, selectedNodeKey, isUsAqi, nodes) {
        if (selectedNodeKey != null) {
            val nodeHistory = nodes?.get(selectedNodeKey)?.history ?: emptyList()
            if (nodeHistory.isEmpty()) {
                history.map { if (!isUsAqi) (it.usAqi ?: it.aqi) else it.aqi }
            } else {
                nodeHistoryValues(nodeHistory, isUsAqi)
            }
        } else {
            history.map { if (!isUsAqi) (it.usAqi ?: it.aqi) else it.aqi }
        }
    }

    // Timestamps for X-axis labels
    val timestamps: List<Long> = remember(history, selectedNodeKey, nodes) {
        if (selectedNodeKey != null) {
            (nodes?.get(selectedNodeKey)?.history?.map { it.ts }?.takeIf { it.isNotEmpty() })
                ?: history.map { it.ts }
        } else {
            history.map { it.ts }
        }
    }

    val graphColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val highlightColorArgb = highlightColor.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceColorArgb = surfaceColor.toArgb()

    var selectedIndex by remember(selectedNodeKey) { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val path = remember { Path() }
    val fillPath = remember { Path() }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val axisTextPaint =
        remember {
            Paint().apply {
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }
        }

    val tooltipTextPaint =
        remember {
            Paint().apply {
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }
        }

    val tooltipBgPaint =
        remember {
            Paint().apply {
                setShadowLayer(12f, 0f, 4f, android.graphics.Color.argb(50, 0, 0, 0))
            }
        }

    val gradientBrush =
        remember(graphColor) {
            Brush.verticalGradient(
                colors = listOf(graphColor.copy(alpha = 0.3f), graphColor.copy(alpha = 0.0f)),
            )
        }

    // Label shown in header next to the hamburger icon
    val graphLabel = if (selectedNodeKey == null) "Zone Average" else selectedNodeKey ?: "Zone Average"

    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
                .padding(16.dp),
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "24 Hour Trend",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    if (nodeKeysWithHistory.isNotEmpty()) {
                        Text(
                            graphLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (nodeKeysWithHistory.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Select node",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Zone Average",
                                        fontWeight = if (selectedNodeKey == null) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    selectedNodeKey = null
                                    menuExpanded = false
                                },
                            )
                            nodeKeysWithHistory.forEach { key ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            key,
                                            fontWeight = if (selectedNodeKey == key) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        selectedNodeKey = key
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val labelWidth = with(density) { 35.dp.toPx() }

            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .pointerInput(values) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val graphWidth = size.width.toFloat() - labelWidth
                                    val touchX = (offset.x - labelWidth).coerceAtLeast(0f)
                                    val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                    val index = (fraction * (values.size - 1)).roundToInt()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = index
                                },
                                onDragEnd = { selectedIndex = null },
                                onDragCancel = { selectedIndex = null },
                                onDrag = { change, _ ->
                                    val graphWidth = size.width.toFloat() - labelWidth
                                    val touchX = (change.position.x - labelWidth).coerceAtLeast(0f)
                                    val fraction = (touchX / graphWidth).coerceIn(0f, 1f)
                                    val index = (fraction * (values.size - 1)).roundToInt()

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

                val maxAqi = values.maxOrNull()?.toFloat()?.coerceAtLeast(100f) ?: 100f
                val minAqi = values.minOrNull()?.toFloat()?.coerceAtMost(0f) ?: 0f
                val range = maxAqi - minAqi

                fun getX(index: Int): Float = labelWidth + (index.toFloat() / (values.size - 1)) * width

                fun getY(aqi: Int): Float = height - ((aqi - minAqi) / range * height)

                path.rewind()
                fillPath.rewind()

                values.forEachIndexed { i, aqi ->
                    val x = getX(i)
                    val y = getY(aqi)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = getX(i - 1)
                        val prevY = getY(values[i - 1])
                        val controlX = prevX + (x - prevX) / 2
                        path.cubicTo(controlX, prevY, controlX, y, x, y)
                    }
                }

                fillPath.addPath(path)
                fillPath.lineTo(size.width, height)
                fillPath.lineTo(labelWidth, height)
                fillPath.close()

                drawPath(path = fillPath, brush = gradientBrush)
                drawPath(path = path, color = graphColor, style = Stroke(width = 3.dp.toPx()))

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    axisTextPaint.color = labelColor
                    axisTextPaint.textAlign = Paint.Align.LEFT
                    axisTextPaint.typeface = Typeface.DEFAULT_BOLD

                    nativeCanvas.drawText("${maxAqi.toInt()}", 0f, 30f, axisTextPaint)
                    val midAqi = (maxAqi + minAqi) / 2
                    nativeCanvas.drawText("${midAqi.toInt()}", 0f, height / 2 + 10f, axisTextPaint)
                    nativeCanvas.drawText("${minAqi.toInt()}", 0f, height - 10f, axisTextPaint)

                    if (selectedIndex == null) {
                        axisTextPaint.textAlign = Paint.Align.CENTER
                        axisTextPaint.typeface = Typeface.DEFAULT

                        val indicesToLabel = listOf(0, values.size / 2, values.size - 1)
                        indicesToLabel.forEach { i ->
                            if (i < timestamps.size) {
                                val date = Date(timestamps[i] * 1000)
                                val label = timeFormatter.format(date)

                                axisTextPaint.textAlign =
                                    when (i) {
                                        0 -> Paint.Align.LEFT
                                        values.size - 1 -> Paint.Align.RIGHT
                                        else -> Paint.Align.CENTER
                                    }
                                val xPos = getX(i)
                                nativeCanvas.drawText(label, xPos, height + 45f, axisTextPaint)
                            }
                        }
                    }

                    selectedIndex?.let { index ->
                        if (index in values.indices) {
                            val aqi = values[index]
                            val ts = if (index < timestamps.size) timestamps[index] else null
                            val x = getX(index)
                            val y = getY(aqi)

                            drawLine(
                                color = highlightColor.copy(alpha = 0.5f),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                            )

                            drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = Offset(x, y))
                            drawCircle(color = highlightColor, radius = 4.dp.toPx(), center = Offset(x, y))

                            tooltipTextPaint.color = highlightColorArgb

                            val timeStr = if (ts != null) timeFormatter.format(Date(ts * 1000)) else "--:--"
                            val label = "AQI $aqi @ $timeStr"

                            val textWidth = tooltipTextPaint.measureText(label)
                            val padding = 20f
                            val boxWidth = textWidth + (padding * 2)
                            val boxHeight = 70f

                            var boxX = x - (boxWidth / 2)
                            if (boxX < labelWidth) boxX = labelWidth
                            if (boxX + boxWidth > size.width) boxX = size.width - boxWidth

                            val boxY = -60f

                            tooltipBgPaint.color = surfaceColorArgb

                            nativeCanvas.drawRoundRect(
                                boxX,
                                boxY,
                                boxX + boxWidth,
                                boxY + boxHeight,
                                16f,
                                16f,
                                tooltipBgPaint,
                            )

                            nativeCanvas.drawText(label, boxX + padding, boxY + boxHeight - 22f, tooltipTextPaint)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DotHistoryGraph(
    history: List<HistoryPoint>,
    modifier: Modifier = Modifier,
    isUsAqi: Boolean = false,
) {
    if (history.isEmpty()) return

    val values: List<Int> = remember(history, isUsAqi) {
        history.map { if (!isUsAqi) (it.usAqi ?: it.aqi) else it.aqi }
    }
    val timestamps: List<Long> = remember(history) { history.map { it.ts } }
    val cellColors: List<Color> = remember(values, isUsAqi) {
        values.map { getAqiColor(it, !isUsAqi) }
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val highlightColorArgb = highlightColor.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceColorArgb = surfaceColor.toArgb()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val axisTextPaint =
        remember {
            Paint().apply {
                textSize = 30f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
        }

    val tooltipTextPaint =
        remember {
            Paint().apply {
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }
        }

    val tooltipBgPaint =
        remember {
            Paint().apply {
                setShadowLayer(12f, 0f, 4f, android.graphics.Color.argb(50, 0, 0, 0))
            }
        }

    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
                .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Hourly AQI",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Last 24 hours",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))

            val cellHeightPx = with(density) { 32.dp.toPx() }
            val gapPx = with(density) { 4.dp.toPx() }
            val radiusPx = with(density) { 4.dp.toPx() }

            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .pointerInput(values) {
                            detectTapGestures { offset ->
                                val step = (size.width.toFloat() + gapPx) / values.size
                                val index = (offset.x / step).toInt().coerceIn(0, values.size - 1)
                                if (index == selectedIndex) {
                                    selectedIndex = null
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = index
                                }
                            }
                        },
            ) {
                val n = values.size
                val cellWidth = ((size.width - gapPx * (n - 1)) / n).coerceAtLeast(1f)

                fun cellLeft(index: Int): Float = index * (cellWidth + gapPx)

                values.forEachIndexed { i, _ ->
                    drawRoundRect(
                        color = cellColors[i],
                        topLeft = Offset(cellLeft(i), 0f),
                        size = Size(cellWidth, cellHeightPx),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                    )
                }

                selectedIndex?.let { index ->
                    if (index in values.indices) {
                        drawRoundRect(
                            color = highlightColor,
                            topLeft = Offset(cellLeft(index), 0f),
                            size = Size(cellWidth, cellHeightPx),
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    if (selectedIndex == null && n > 2) {
                        axisTextPaint.color = labelColor
                        val indicesToLabel = listOf(0, n / 4, n / 2, 3 * n / 4, n - 1).distinct()
                        indicesToLabel.forEach { i ->
                            if (i < timestamps.size) {
                                val label = timeFormatter.format(Date(timestamps[i] * 1000))
                                axisTextPaint.textAlign =
                                    when (i) {
                                        0 -> Paint.Align.LEFT
                                        n - 1 -> Paint.Align.RIGHT
                                        else -> Paint.Align.CENTER
                                    }
                                val x = cellLeft(i) + cellWidth / 2
                                nativeCanvas.drawLine(x, cellHeightPx + 8f, x, cellHeightPx + 16f, axisTextPaint)
                                nativeCanvas.drawText(label, x, cellHeightPx + 44f, axisTextPaint)
                            }
                        }
                    }

                    selectedIndex?.let { index ->
                        if (index in values.indices) {
                            val aqi = values[index]
                            val ts = if (index < timestamps.size) timestamps[index] else null
                            val timeStr = if (ts != null) timeFormatter.format(Date(ts * 1000)) else "--:--"
                            val label = "AQI $aqi @ $timeStr"

                            tooltipTextPaint.color = highlightColorArgb
                            val textWidth = tooltipTextPaint.measureText(label)
                            val padding = 20f
                            val boxWidth = textWidth + padding * 2
                            val boxHeight = 70f

                            var boxX = cellLeft(index) + cellWidth / 2 - boxWidth / 2
                            if (boxX < 0f) boxX = 0f
                            if (boxX + boxWidth > size.width) boxX = size.width - boxWidth
                            val boxY = -(boxHeight + 12f)

                            tooltipBgPaint.color = surfaceColorArgb
                            nativeCanvas.drawRoundRect(
                                boxX,
                                boxY,
                                boxX + boxWidth,
                                boxY + boxHeight,
                                16f,
                                16f,
                                tooltipBgPaint,
                            )
                            nativeCanvas.drawText(label, boxX + padding, boxY + boxHeight - 22f, tooltipTextPaint)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val legendReps = if (!isUsAqi) listOf(25, 75, 125, 175, 250, 400) else listOf(25, 75, 150, 250, 350, 450)
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
                                .background(getAqiColor(rep, !isUsAqi), RoundedCornerShape(2.dp)),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isUsAqi) "Hazardous" else "Severe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun HistoryGraphPager(
    history: List<HistoryPoint>,
    modifier: Modifier = Modifier,
    isUsAqi: Boolean = false,
    nodes: Map<String, NodeReading>? = null,
) {
    if (history.isEmpty()) return

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
                modifier = Modifier.height(280.dp),
            ) { page ->
                when (page) {
                    0 -> AqiHistoryGraph(
                        history = history,
                        isUsAqi = isUsAqi,
                        nodes = nodes,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> DotHistoryGraph(
                        history = history,
                        isUsAqi = isUsAqi,
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
            text = if (pagerState.currentPage == 0) "Swipe for dots history" else "Swipe for trend graph",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
