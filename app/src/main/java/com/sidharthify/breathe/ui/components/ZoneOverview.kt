// SPDX-License-Identifier: MIT
/*
 * ZoneOverview.kt - Overview sheet for a sensor or estimate opened from the map or explore
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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sidharthify.breathe.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.HistoricalDataPoint
import com.sidharthify.breathe.data.RetrofitClient
import com.sidharthify.breathe.util.asAqiText
import com.sidharthify.breathe.util.calculateChange1h
import com.sidharthify.breathe.util.calculateCigarettes
import com.sidharthify.breathe.util.displayAqi
import com.sidharthify.breathe.util.formatPollutantName
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.getHealthAdvice
import com.sidharthify.breathe.util.getTimeAgo
import com.sidharthify.breathe.util.isEstimate
import com.sidharthify.breathe.util.isSensorOffline
import com.sidharthify.breathe.util.onAqiColor
import com.sidharthify.breathe.util.placeLine
import com.sidharthify.breathe.util.pm25
import com.sidharthify.breathe.util.pm25ToAqi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val ABOUT_URL = "https://about.breatheoss.app"
private const val WHO_PM25_24H = 15.0

private enum class PanelRange(val label: String, val token: String) {
    DAY("24h", "24h"),
    MONTH("30d", "30d"),
    HALF_YEAR("180d", "180d"),
}

private data class PanelBar(
    val label: String,
    val value: Double,
)

@Composable
fun ZoneOverview(
    zone: AqiResponse,
    isPinned: Boolean,
    useNaqi: Boolean,
    onPinToggle: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isLive = !zone.isEstimate
    val offline = zone.isSensorOffline
    val aqi = zone.displayAqi(useNaqi)
    val color = getAqiColor(aqi, !useNaqi)
    val textColor = color.asAqiText(MaterialTheme.colorScheme.surface)
    val place = placeLine(zone.district, zone.division)
    var range by rememberSaveable(zone.zoneId) { mutableStateOf(PanelRange.DAY) }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    zone.zoneName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (place.isNotEmpty()) {
                    Text(
                        place,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    if (isLive && !offline) {
                        LiveDot()
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        when {
                            !isLive -> "Satellite estimate, no sensor here yet"
                            offline -> "Sensor offline, showing a satellite estimate"
                            zone.showingRaw -> "Live ground sensor · raw readings"
                            else -> "Live ground sensor"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalIconButton(onClick = onPinToggle) {
                Icon(
                    if (isPinned) {
                        Icons.Filled.PushPin
                    } else {
                        Icons.Outlined.PushPin
                    },
                    contentDescription =
                        if (isPinned) {
                            "Unpin ${zone.zoneName}"
                        } else {
                            "Pin ${zone.zoneName}"
                        },
                )
            }
            FilledTonalIconButton(
                onClick = {
                    val url = "https://breatheoss.app/zone/${zone.zoneId}"
                    val send =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${zone.zoneName} air quality | Breathe")
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                    context.startActivity(Intent.createChooser(send, null))
                },
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Share ${zone.zoneName}")
            }
        }

        PanelHero(zone = zone, aqi = aqi, useNaqi = useNaqi, color = color, textColor = textColor)

        HealthAdviceCard(aqi = aqi, useNaqi = useNaqi)

        PanelChartCard(
            zone = zone,
            isLive = isLive,
            range =
                if (isLive) {
                    range
                } else {
                    PanelRange.DAY
                },
            useNaqi = useNaqi,
            onRangeChange = { range = it },
        )

        PanelStatTiles(zone = zone)

        if (isLive) {
            PanelGrid(zone = zone)
        }

        PanelCard {
            Text(
                "Where this comes from",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val body =
                when {
                    !isLive -> "There's no ground sensor in ${zone.zoneName} yet. These numbers are modelled by CAMS and served through Open-Meteo, so they can miss local smoke and dust."
                    offline -> "This BreatheOSS AirGradient sensor isn't reporting right now, so these numbers are a CAMS estimate for its location, served through Open-Meteo."
                    else -> {
                        val district =
                            if (zone.district != null) {
                                " in the ${zone.district} district"
                            } else {
                                ""
                            }
                        val raw =
                            if (zone.showingRaw) {
                                " You are seeing its raw readings, before AirGradient's correction."
                            } else {
                                ""
                            }
                        "PM2.5 and PM10 come from this BreatheOSS AirGradient sensor$district. Gases come from CAMS through Open-Meteo.$raw"
                    }
                }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isLive) {
                Button(onClick = { uriHandler.openUri("$ABOUT_URL/#help") }) {
                    Text("Host a sensor here")
                }
            }
        }

        PanelCard {
            Icon(
                Icons.Filled.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                "Become a contributor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Host a sensor for free, send code, or help fund the network. Every new sensor puts a real reading where there was only a model before.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { uriHandler.openUri("$ABOUT_URL/#help") }) {
                    Text("Help expand the network")
                }
                OutlinedButton(onClick = { uriHandler.openUri("$ABOUT_URL/#contribute") }) {
                    Text("Other ways to help")
                }
            }
        }

        OutlinedButton(
            onClick = onOpenDetails,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open full details")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PanelCard(content: @Composable () -> Unit) {
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
private fun PanelHero(
    zone: AqiResponse,
    aqi: Int,
    useNaqi: Boolean,
    color: Color,
    textColor: Color,
) {
    val standard =
        if (useNaqi) {
            "NAQI"
        } else {
            "US AQI"
        }
    val chip =
        if (zone.showingRaw) {
            "Raw $standard"
        } else {
            standard
        }
    val change = remember(zone.history, aqi, useNaqi) { calculateChange1h(zone.history, aqi, useNaqi) }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        getAqiCategory(aqi, !useNaqi).label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                    zone.timestampUnix?.let {
                        Text(
                            "Updated ${getTimeAgo(it.toLong()).replaceFirstChar { c -> c.lowercase() }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$aqi",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                    )
                    Surface(color = color, shape = RoundedCornerShape(100)) {
                        Text(
                            chip,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color.onAqiColor(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val tonal = color.copy(alpha = 0.22f).compositeOver(color.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val pollutant =
                    if (!useNaqi && !zone.usMainPollutant.isNullOrBlank() && zone.usMainPollutant != "n/a") {
                        zone.usMainPollutant
                    } else {
                        zone.mainPollutant
                    }
                HeroPill(color = tonal) {
                    Text(
                        "Main ${formatPollutantName(pollutant)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (zone.pm25 > 0) {
                    HeroPill(color = tonal) {
                        Text(
                            "PM2.5 ${zone.pm25} µg/m³",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (change != null && change != 0) {
                    HeroPill(color = tonal) {
                        Text(
                            if (change < 0) {
                                "${-change} better in 1h"
                            } else {
                                "$change worse in 1h"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HealthAdviceCard(
    aqi: Int,
    useNaqi: Boolean,
) {
    val advice = getHealthAdvice(aqi, !useNaqi)
    val color = getAqiColor(aqi, !useNaqi)
    InfoRow(
        shape = MaterialTheme.shapes.extraLarge,
        iconContainer = color.copy(alpha = 0.22f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh),
        title = advice.title,
        body = advice.text,
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = color.asAqiText(MaterialTheme.colorScheme.surface),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PanelChartCard(
    zone: AqiResponse,
    isLive: Boolean,
    range: PanelRange,
    useNaqi: Boolean,
    onRangeChange: (PanelRange) -> Unit,
) {
    val hourFormat = remember { SimpleDateFormat("HH:00", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val daily by produceState<List<PanelBar>?>(null, zone.zoneId, range, zone.showingRaw) {
        if (range == PanelRange.DAY) {
            value = null
            return@produceState
        }
        value = null
        val metric =
            if (zone.showingRaw) {
                "pm2.5_raw"
            } else {
                "pm2.5"
            }
        val rows =
            withContext(Dispatchers.IO) {
                runCatching {
                    RetrofitClient.api.getHistoricalData(zone.zoneId, range.token, "1d", metric).data
                }.getOrDefault(emptyList())
            }
        value =
            rows
                .sortedBy { it.ts }
                .mapNotNull { row ->
                    val v =
                        if (zone.showingRaw) {
                            row.pm25Raw
                        } else {
                            row.pm25
                        }
                    v?.let { PanelBar(dayFormat.format(Date(row.ts * 1000)), it) }
                }
    }
    val bars =
        if (range == PanelRange.DAY) {
            zone.history
                .orEmpty()
                .sortedBy { it.ts }
                .mapNotNull { point -> point.pm25?.let { PanelBar(hourFormat.format(Date(point.ts * 1000)), it) } }
        } else {
            daily.orEmpty()
        }
    val pills =
        if (range == PanelRange.DAY) {
            listOfNotNull(
                bars.map { it.value }.averageOrNull()?.let { "Last 24 hours" to it },
                bars.takeLast(12).map { it.value }.averageOrNull()?.let { "Last 12 hours" to it },
            )
        } else {
            listOfNotNull(
                bars.map { it.value }.averageOrNull()?.let { "${range.label.removeSuffix("d")} day average" to it },
                bars.maxOfOrNull { it.value }?.let { "Worst day" to it },
            )
        }

    PanelCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PM2.5", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (range == PanelRange.DAY) {
                        "µg/m³, hourly"
                    } else {
                        "µg/m³, daily average"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isLive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                PanelRange.entries.forEachIndexed { index, option ->
                    ToggleButton(
                        checked = range == option,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onRangeChange(option)
                            }
                        },
                        shapes =
                            when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                PanelRange.entries.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (range != PanelRange.DAY && daily == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(204.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Loading",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (bars.isEmpty()) {
            Text(
                "No readings yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PanelBars(bars = bars, useNaqi = useNaqi)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pills.forEach { (label, value) ->
                val pillAqi = pm25ToAqi(value, useNaqi)
                val pillColor = getAqiColor(pillAqi, !useNaqi)
                Surface(
                    color = pillColor.copy(alpha = 0.2f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(100),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(10.dp)
                                    .background(pillColor, RoundedCornerShape(100)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "${"%.1f".format(value)} µg/m³",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun List<Double>.averageOrNull(): Double? {
    if (isEmpty()) {
        return null
    }
    return average()
}

@Composable
private fun PanelBars(
    bars: List<PanelBar>,
    useNaqi: Boolean,
) {
    var selected by remember(bars) { mutableStateOf<Int?>(null) }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val measurer = rememberTextMeasurer()
    val max = maxOf(bars.maxOf { it.value }, 10.0)
    val scroll = rememberScrollState()
    LaunchedEffect(bars, scroll.maxValue) {
        scroll.scrollTo(scroll.maxValue)
    }
    Column {
        Text(
            selected?.let { i ->
                val bar = bars[i]
                "${bar.label}  ·  ${"%.1f".format(bar.value)} µg/m³  ·  ${getAqiCategory(pm25ToAqi(bar.value, useNaqi), !useNaqi).label}"
            } ?: if (scroll.maxValue > 0) {
                "Tap a bar for its reading, swipe for older days"
            } else {
                "Tap a bar for its reading"
            },
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val slot = maxOf(maxWidth / bars.size, 14.dp)
            val every = maxOf(1, ceil(56.dp / slot).toInt())
            Canvas(
                modifier =
                    Modifier
                        .horizontalScroll(scroll)
                        .width(slot * bars.size)
                        .height(180.dp)
                        .pointerInput(bars) {
                            detectTapGestures { offset ->
                                val step = size.width.toFloat() / bars.size
                                val index = (offset.x / step).toInt().coerceIn(0, bars.size - 1)
                                selected =
                                    if (selected == index) {
                                        null
                                    } else {
                                        index
                                    }
                            }
                        },
            ) {
                val step = size.width / bars.size
                val gap = step * 0.15f
                val axis = 20.dp.toPx()
                val chart = size.height - axis
                bars.forEachIndexed { i, bar ->
                    val h = (bar.value / max * chart).toFloat().coerceAtLeast(2f)
                    val c = getAqiColor(pm25ToAqi(bar.value, useNaqi), !useNaqi)
                    drawRoundRect(
                        color =
                            if (selected == null || selected == i) {
                                c
                            } else {
                                c.copy(alpha = 0.4f)
                            },
                        topLeft = Offset(i * step + gap / 2, chart - h),
                        size = Size(step - gap, h),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )
                    if ((bars.size - 1 - i) % every == 0) {
                        val text = measurer.measure(bar.label, labelStyle)
                        val x =
                            (i * step + step / 2 - text.size.width / 2f)
                                .coerceIn(0f, size.width - text.size.width)
                        drawText(text, topLeft = Offset(x, chart + 4.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelStatTiles(zone: AqiResponse) {
    val pm25 = zone.averages24h?.get("pm2_5") ?: zone.pm25
    if (pm25 <= 0) {
        return
    }
    val ratio = pm25 / WHO_PM25_24H
    val whoColor =
        when {
            ratio <= 1 -> getAqiColor(25, true)
            ratio <= 2.5 -> getAqiColor(75, true)
            ratio <= 5 -> getAqiColor(125, true)
            else -> getAqiColor(175, true)
        }.asAqiText(MaterialTheme.colorScheme.surface)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PanelTile(
            label = "Cigarettes today",
            value = "${calculateCigarettes(pm25)}",
            note = "What breathing this air for a day adds up to, from the 24 hour PM2.5 average",
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        PanelTile(
            label = "WHO guideline",
            value =
                if (ratio <= 1) {
                    "Within"
                } else {
                    "${"%.1f".format(ratio)}×"
                },
            note = "24 hour average ${"%.1f".format(pm25)} µg/m³ against the WHO limit of ${WHO_PM25_24H.roundToInt()} µg/m³",
            valueColor = whoColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PanelTile(
    label: String,
    value: String,
    note: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = valueColor)
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PanelGrid(zone: AqiResponse) {
    val raw = zone.showingRaw
    val rows by produceState<List<HistoricalDataPoint>?>(null, zone.zoneId, raw) {
        value = null
        val metrics =
            if (raw) {
                "pm2.5,pm10,pm2.5_raw,pm10_raw"
            } else {
                "pm2.5,pm10"
            }
        val fetched =
            withContext(Dispatchers.IO) {
                runCatching {
                    RetrofitClient.api.getHistoricalData(zone.zoneId, "9d", "1h", metrics).data
                }.getOrDefault(emptyList())
            }
        value =
            fetched
                .map { row ->
                    if (raw) {
                        row.copy(pm25 = row.pm25Raw, pm10 = row.pm10Raw)
                    } else {
                        row
                    }
                }.filter { it.pm25 != null || it.pm10 != null }
    }
    val loaded = rows
    if (loaded == null) {
        PanelCard {
            Text("Last 10 days by hour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Loading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    if (loaded.isEmpty()) {
        PanelCard {
            Text("Last 10 days by hour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("No history yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    ExtendedDotHistoryChart(
        data = loaded,
        showPm25 = true,
        showPm10 = true,
        title = "Last 10 days by hour",
    )
}
