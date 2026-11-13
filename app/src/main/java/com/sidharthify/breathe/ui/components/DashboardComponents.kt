// SPDX-License-Identifier: MIT
/*
 * DashboardComponents.kt - Composable components for the main dashboard screen, including AQI display, category spectrum, and pollutant details
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
 * Copyright (C) 2026 Aditya Choudhary <empirea99@gmail.com>
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



import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.sidharthify.breathe.R
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.data.NodeReading
import com.sidharthify.breathe.data.SensorInfo
import com.sidharthify.breathe.data.WeatherInfo
import com.sidharthify.breathe.ui.expressiveClickable
import com.sidharthify.breathe.ui.loopingFloat
import com.sidharthify.breathe.ui.pressSquish
import com.sidharthify.breathe.util.PollutantText
import com.sidharthify.breathe.util.calculateChange1h
import com.sidharthify.breathe.util.calculateCigarettes
import com.sidharthify.breathe.util.asAqiText
import com.sidharthify.breathe.util.displayAqi
import com.sidharthify.breathe.util.onAqiColor
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.navigation.LocalIsTabletLandscape
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.getHealthAdvice
import com.sidharthify.breathe.util.getTimeAgo
import com.sidharthify.breathe.util.pm25
import com.sidharthify.breathe.util.weatherConditionIconRes
import com.sidharthify.breathe.util.isEstimate
import com.sidharthify.breathe.util.isSensorOffline
import com.sidharthify.breathe.util.placeLine
import kotlin.math.ceil
import kotlin.math.roundToInt

class SoftBurstShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = size.minDimension / 2f
        val polygon =
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                radius = radius,
                innerRadius = radius * 0.7f,
                rounding = CornerRounding(radius * 0.2f),
                centerX = size.width / 2f,
                centerY = size.height / 2f,
            )
        return Outline.Generic(polygon.toPath().asComposePath())
    }
}

@Composable
fun MainDashboardDetail(
    zone: AqiResponse,
    provider: String?,
    isDarkTheme: Boolean,
    useNaqi: Boolean = false,
    sensorInfos: List<SensorInfo> = emptyList(),
    onOpenHistory: (() -> Unit)? = null,
    isWideLayout: Boolean = false,
    rawMode: Boolean = false,
    rawHintHidden: Boolean = true,
    onRawModeChange: ((Boolean) -> Unit)? = null,
    onHideRawHint: (() -> Unit)? = null,
) {
    val pm25 = zone.pm25
    val displayAqi = zone.displayAqi(useNaqi)

    val pm25For24h = zone.averages24h?.get("pm2_5") ?: zone.averages24h?.get("pm2.5") ?: pm25
    val cigarettes = if (pm25For24h > 0) calculateCigarettes(pm25For24h) else 0.0

    val animationSettings = LocalAnimationSettings.current

    val aqiColor by animateColorAsState(
        targetValue = getAqiColor(displayAqi, !useNaqi),
        animationSpec = if (animationSettings.colorTransitions) {
            tween(durationMillis = 600, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "DashboardColor",
    )

    val aqiTextColor = aqiColor.asAqiText(MaterialTheme.colorScheme.surface)
    val uriHandler = LocalUriHandler.current

    val isOpenMeteo = zone.isEstimate
    val isAirGradient = !zone.isEstimate
    val offline = zone.isSensorOffline
    val place = placeLine(zone.district, zone.division)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentWidth()
                .widthIn(max = 1200.dp)
                .padding(vertical = 12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.zoneName,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 40.sp,
                )

                if (place.isNotEmpty()) {
                    Text(
                        place,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (isAirGradient && !offline) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        LiveDot()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (zone.showingRaw) {
                                "Live ground sensor · raw readings"
                            } else {
                                "Live ground sensor"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        if (offline) {
                            "Sensor offline, showing a satellite estimate"
                        } else {
                            "Satellite estimate, no sensor here yet"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isAirGradient) {
                    Image(
                        painter = painterResource(id = R.drawable.air_gradient_logo),
                        contentDescription = "AirGradient",
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .height(28.dp)
                                .expressiveClickable { uriHandler.openUri("https://www.airgradient.com/") },
                        alpha = 0.9f,
                    )
                }

                if (isOpenMeteo) {
                    val logoRes = if (isDarkTheme) R.drawable.open_meteo_logo else R.drawable.open_meteo_logo_light
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "OpenMeteo",
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .height(28.dp)
                                .expressiveClickable { uriHandler.openUri("https://open-meteo.com/") },
                        alpha = 0.9f,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isAirGradient && onRawModeChange != null) {
            RawModeSwitch(
                rawMode = rawMode,
                rawAvailable = zone.raw != null,
                onRawModeChange = onRawModeChange,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        val rawUnavailable =
            when {
                rawMode && zone.isEstimate -> "Raw readings are switched on, but there is no sensor here, so nothing changes."
                rawMode && zone.raw == null -> "Raw readings are switched on, but this sensor has no live reading to show raw, so these are the calibrated satellite numbers."
                else -> null
            }
        if (zone.showingRaw && !rawHintHidden) {
            RawBanner(
                message = "Showing this sensor's uncorrected readings.",
                showPoints = true,
                onHide = onHideRawHint,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else if (rawUnavailable != null) {
            Text(
                rawUnavailable,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!zone.warning.isNullOrBlank()) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    ),
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = zone.warning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 20.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        val weather = zone.weather
        val pollutants = zone.concentrations ?: emptyMap()

        if (isWideLayout) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AqiHeroCard(
                    zone = zone,
                    displayAqi = displayAqi,
                    useNaqi = useNaqi,
                    aqiColor = aqiColor,
                    aqiTextColor = aqiTextColor,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                )

                ContextRows(
                    displayAqi = displayAqi,
                    useNaqi = useNaqi,
                    weather = weather,
                    cigarettes = cigarettes,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (pollutants.isNotEmpty()) {
                    ConcentrationsSection(
                        pollutants = pollutants,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (isAirGradient) {
                    AboutSensorCard(
                        zone = zone,
                        sensorInfo = sensorInfos.find { it.locationId == zone.locationId },
                        modifier = Modifier.weight(1f),
                    )
                } else if (!zone.nodes.isNullOrEmpty()) {
                    IndividualNodeReadingsSection(
                        nodes = zone.nodes,
                        useNaqi = useNaqi,
                        sensorInfos = sensorInfos,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            AqiHeroCard(
                zone = zone,
                displayAqi = displayAqi,
                useNaqi = useNaqi,
                aqiColor = aqiColor,
                aqiTextColor = aqiTextColor,
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            ContextRows(
                displayAqi = displayAqi,
                useNaqi = useNaqi,
                weather = weather,
                cigarettes = cigarettes,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (pollutants.isNotEmpty()) {
                ConcentrationsSection(
                    pollutants = pollutants,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isAirGradient) {
                AboutSensorCard(
                    zone = zone,
                    sensorInfo = sensorInfos.find { it.locationId == zone.locationId },
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (!zone.nodes.isNullOrEmpty()) {
                IndividualNodeReadingsSection(
                    nodes = zone.nodes,
                    useNaqi = useNaqi,
                    sensorInfos = sensorInfos,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (!zone.history.isNullOrEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                HistoryGraphPager(
                    history = zone.history,
                    useNaqi = useNaqi,
                    nodes = zone.nodes,
                )
            }

            if (isAirGradient && onOpenHistory != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Text("View Extended History")
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun AqiHeroCard(
    zone: AqiResponse,
    displayAqi: Int,
    useNaqi: Boolean,
    aqiColor: Color,
    aqiTextColor: Color,
    modifier: Modifier = Modifier,
) {
    val animationSettings = LocalAnimationSettings.current
    val standardLabel = if (!useNaqi) "US AQI" else "NAQI"
    val aqiLabel =
        if (zone.showingRaw) {
            "Raw $standardLabel"
        } else {
            standardLabel
        }
    val category = getAqiCategory(displayAqi, !useNaqi).label

    val screenWidth = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    val compact = screenWidth < 390.dp
    val burstSize = if (compact) 132.dp else 156.dp
    val aqiFontSize =
        when {
            displayAqi >= 100 && compact -> 40.sp
            displayAqi >= 100 -> 48.sp
            compact -> 52.sp
            else -> 60.sp
        }

    val animatedAqi by animateIntAsState(
        targetValue = displayAqi,
        animationSpec = if (animationSettings.numberAnimations) {
            tween(durationMillis = 800, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "DashboardNumber",
    )

    val breatheScale by loopingFloat(
        enabled = animationSettings.pulseEffects,
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breatheScale",
    )

    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val container = aqiColor.copy(alpha = if (dark) 0.18f else 0.14f).compositeOver(MaterialTheme.colorScheme.surfaceContainer)
    val pillColor = aqiColor.copy(alpha = if (dark) 0.26f else 0.22f).compositeOver(container)
    val burst = remember { SoftBurstShape() }
    val change1h = remember(zone.history, displayAqi, useNaqi) {
        calculateChange1h(history = zone.history, currentAqi = displayAqi, useNaqi = useNaqi)
    }
    val pollutant =
        if (!useNaqi && !zone.usMainPollutant.isNullOrBlank() && zone.usMainPollutant != "n/a") {
            zone.usMainPollutant
        } else {
            zone.mainPollutant
        }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        modifier = modifier.pressSquish(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = aqiTextColor,
                    )
                    Text(
                        zone.timestampUnix?.let { "Updated ${getTimeAgo(it.toLong()).replaceFirstChar { c -> c.lowercase() }}" } ?: "Live",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(color = aqiColor, shape = RoundedCornerShape(100)) {
                    Text(
                        aqiLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = aqiColor.onAqiColor(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier
                                .size(burstSize)
                                .graphicsLayer {
                                    scaleX = breatheScale
                                    scaleY = breatheScale
                                }.background(pillColor, burst),
                    )
                    Text(
                        text = "$animatedAqi",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = aqiFontSize),
                        fontWeight = FontWeight.Black,
                        color = aqiTextColor,
                        letterSpacing = (-1).sp,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroPill(color = pillColor) {
                        Text(
                            "Main ",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PollutantText(
                            rawKey = pollutant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (change1h != null && change1h != 0) {
                        val better = change1h < 0
                        HeroPill(color = pillColor) {
                            Icon(
                                if (better) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                if (better) {
                                    "${-change1h} better in 1h"
                                } else {
                                    "$change1h worse in 1h"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else if (change1h == 0) {
                        HeroPill(color = pillColor) {
                            Text(
                                "Steady in 1h",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            AqiSegmentGauge(displayAqi = displayAqi, useNaqi = useNaqi)
        }
    }
}

@Composable
fun HeroPill(
    color: Color,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(color = color, shape = RoundedCornerShape(100)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            content = content,
        )
    }
}

@Composable
private fun AqiSegmentGauge(
    displayAqi: Int,
    useNaqi: Boolean,
) {
    val animationSettings = LocalAnimationSettings.current
    val bands =
        if (useNaqi) {
            listOf(0 to 50, 51 to 100, 101 to 200, 201 to 300, 301 to 400, 401 to 500)
        } else {
            listOf(0 to 50, 51 to 100, 101 to 150, 151 to 200, 201 to 300, 301 to 500)
        }
    val clamped = displayAqi.coerceIn(0, 500)
    val position by animateFloatAsState(
        targetValue = clamped / 500f,
        animationSpec =
            if (animationSettings.numberAnimations) {
                tween(durationMillis = 800, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 0)
            },
        label = "GaugePosition",
    )
    val handle = MaterialTheme.colorScheme.onSurface
    val ring = MaterialTheme.colorScheme.surface
    val dim =
        if (ring.luminance() < 0.5f) {
            0.4f
        } else {
            0.45f
        }
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(24.dp),
    ) {
        val gap = 4.dp.toPx()
        val trackH = 10.dp.toPx()
        val top = (size.height - trackH) / 2
        val usable = size.width - gap * (bands.size - 1)
        var x = 0f
        bands.forEach { (low, high) ->
            val w = usable * ((high - low + 1).toFloat() / 501f)
            val sample = (low + high) / 2
            val inside = clamped in low..high
            drawRoundRect(
                color =
                    if (inside) {
                        getAqiColor(sample, !useNaqi)
                    } else {
                        getAqiColor(sample, !useNaqi).copy(alpha = dim).compositeOver(ring)
                    },
                topLeft = Offset(x, top),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2, trackH / 2),
            )
            x += w + gap
        }
        val hx = (position * size.width).coerceIn(3.dp.toPx(), size.width - 3.dp.toPx())
        val hw = 4.dp.toPx()
        drawRoundRect(
            color = ring,
            topLeft = Offset(hx - hw / 2 - 2.dp.toPx(), 0f),
            size = Size(hw + 4.dp.toPx(), size.height),
            cornerRadius = CornerRadius(hw, hw),
        )
        drawRoundRect(
            color = handle,
            topLeft = Offset(hx - hw / 2, 2.dp.toPx()),
            size = Size(hw, size.height - 4.dp.toPx()),
            cornerRadius = CornerRadius(hw / 2, hw / 2),
        )
    }
}

@Composable
private fun ContextRows(
    displayAqi: Int,
    useNaqi: Boolean,
    weather: WeatherInfo?,
    cigarettes: Double,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val locale = LocalLocale.current.platformLocale
    val advice = getHealthAdvice(displayAqi, !useNaqi)
    val aqiColor = getAqiColor(displayAqi, !useNaqi)
    val showWeather = weather != null && weather.text.isNotBlank()
    val showCigarettes = cigarettes > 0.1
    val count = 1 + (if (showWeather) 1 else 0) + (if (showCigarettes) 1 else 0)
    var index = 0
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        InfoRow(
            shape = groupShape(index++, count),
            iconContainer = aqiColor.copy(alpha = 0.22f).compositeOver(colorScheme.surfaceContainerHigh),
            title = advice.title,
            body = advice.text,
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = aqiColor.asAqiText(colorScheme.surface),
                modifier = Modifier.size(24.dp),
            )
        }
        if (showWeather && weather != null) {
            val condition = weather.condition.replaceFirstChar { it.titlecase(locale) }
            val season = weather.season.replaceFirstChar { it.titlecase(locale) }
            InfoRow(
                shape = groupShape(index++, count),
                iconContainer = colorScheme.secondaryContainer,
                title = "$condition · $season",
                body = weather.text,
            ) {
                Icon(
                    painter = painterResource(weatherConditionIconRes(weather.condition)),
                    contentDescription = condition,
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        if (showCigarettes) {
            InfoRow(
                shape = groupShape(index, count),
                iconContainer = colorScheme.errorContainer,
                title = "≈ $cigarettes cigarettes",
                body = "Equivalent PM2.5 inhalation today",
            ) {
                Icon(
                    Icons.Filled.SmokingRooms,
                    contentDescription = null,
                    tint = colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

fun groupShape(
    index: Int,
    count: Int,
): Shape {
    val outer = 24.dp
    val inner = 6.dp
    val top =
        if (index == 0) {
            outer
        } else {
            inner
        }
    val bottom =
        if (index == count - 1) {
            outer
        } else {
            inner
        }
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
fun InfoRow(
    shape: Shape,
    iconContainer: Color,
    title: String,
    body: String,
    icon: @Composable () -> Unit,
) {
    val burst = remember { SoftBurstShape() }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(iconContainer, burst),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConcentrationsSection(
    pollutants: Map<String, Double>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            "Concentrations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        SimpleFlowGrid(
            items = pollutants.entries.toList(),
            columns =
                if (LocalIsTabletLandscape.current) {
                    3
                } else {
                    2
                },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherContextCard(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val conditionLabel = weather.condition.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
    }
    val seasonLabel = weather.season.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
    }
    val colorScheme = MaterialTheme.colorScheme
    val iconBackground = colorScheme.secondaryContainer
    val iconTint = colorScheme.onSecondaryContainer
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurface,
            ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(color = iconBackground, shape = MaterialShapes.Circle.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(weatherConditionIconRes(weather.condition)),
                    contentDescription = conditionLabel,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "$conditionLabel · $seasonLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )
                Text(
                    weather.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SimpleFlowGrid(
    items: List<Map.Entry<String, Double>>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (key, value) ->
                    PollutantChip(key, value, Modifier.weight(1f))
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Individual Node Readings Section
// ──────────────────────────────────────────────

@Composable
fun IndividualNodeReadingsSection(
    modifier: Modifier = Modifier,
    nodes: Map<String, NodeReading>,
    useNaqi: Boolean,
    sensorInfos: List<SensorInfo> = emptyList(),
) {
    Column(modifier = modifier) {
        Text(
            "Individual Node Readings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        val nodeList = nodes.entries.toList()
        val rows = ceil(nodeList.size / 2f).toInt()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val firstIndex = i * 2
                    val secondIndex = firstIndex + 1

                    if (firstIndex < nodeList.size) {
                        val (name, reading) = nodeList[firstIndex]
                        NodeReadingCard(
                            nodeName = name,
                            reading = reading,
                            useNaqi = useNaqi,
                            sensorInfo = sensorInfos.find { it.name == name },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (secondIndex < nodeList.size) {
                        val (name, reading) = nodeList[secondIndex]
                        NodeReadingCard(
                            nodeName = name,
                            reading = reading,
                            useNaqi = useNaqi,
                            sensorInfo = sensorInfos.find { it.name == name },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeReadingCard(
    modifier: Modifier = Modifier,
    nodeName: String,
    reading: NodeReading,
    useNaqi: Boolean,
    sensorInfo: SensorInfo? = null,
) {
    val isDown = reading.pm25 == null
    val displayAqi: Int? =
        if (isDown) {
            null
        } else {
            reading.displayAqi(useNaqi)
        }
    val aqiLabel = if (!useNaqi) "US AQI" else "NAQI"

    var menuExpanded by remember { mutableStateOf(false) }
    var sheetVisible by remember { mutableStateOf(false) }

    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }) {
            SensorDetails(
                nodeName = nodeName,
                reading = reading,
                useNaqi = useNaqi,
                sensorInfo = sensorInfo,
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
            )
        }
    }

    val animationSettings = LocalAnimationSettings.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationSettings.pressFeedback) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "NodeCardSquish",
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onLongPress = {
                        isPressed = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sheetVisible = true
                    },
                )
            },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: name + hamburger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    nodeName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Node options",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sensor Info") },
                            onClick = {
                                menuExpanded = false
                                sheetVisible = true
                            },
                        )
                    }
                }
            }

            // AQI value
            if (isDown) {
                Text(
                    "N/A",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "${displayAqi ?: "—"}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = getAqiColor(displayAqi ?: 0, !useNaqi).asAqiText(MaterialTheme.colorScheme.surface),
                )
            }
            Text(
                aqiLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // PM2.5 and PM10
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PM2.5",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (isDown) "N/A" else reading.pm25.let { "$it" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PM10",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (isDown) "N/A" else reading.pm10?.let { "$it" } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun PollutantChip(
    key: String,
    value: Double,
    modifier: Modifier = Modifier,
) {
    val unit = if (key.lowercase() in listOf("ch4", "co")) "mg/m³" else "µg/m³"

    Box(
        modifier =
            modifier
                .height(80.dp)
                .pressSquish(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    PollutantText(
                        rawKey = key,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$value",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
@Composable
fun AboutSensorCard(
    zone: AqiResponse,
    sensorInfo: SensorInfo?,
    modifier: Modifier = Modifier,
) {
    val readings = zone.readings
    val rawSuffix =
        if (zone.showingRaw) {
            " (raw)"
        } else {
            ""
        }
    fun fmt(
        value: Double?,
        unit: String,
    ): String {
        if (value == null) {
            return "--"
        }
        return "${(value * 10).roundToInt() / 10.0}$unit"
    }
    val facts =
        listOf(
            "Provider" to (sensorInfo?.provider ?: "AirGradient"),
            "Model" to (sensorInfo?.model ?: "--"),
            "Location ID" to (zone.locationId ?: sensorInfo?.locationId)?.toString().orEmpty().ifEmpty { "--" },
            "Installed" to (sensorInfo?.installationDate ?: "--"),
        )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "About this sensor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (readings != null) {
                val rows =
                    listOf(
                        "Temperature$rawSuffix" to fmt(readings.temp, " °C"),
                        "Humidity$rawSuffix" to fmt(readings.humidity, "%"),
                        "PM2.5$rawSuffix" to fmt(readings.pm25, " µg/m³"),
                        "PM10$rawSuffix" to fmt(readings.pm10, " µg/m³"),
                    )
                rows.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { (label, value) ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    value,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            facts.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
