// SPDX-License-Identifier: MIT
/*
 * HomeOverviewComponents.kt - Region snapshot and pinned overview cards shown on the home screen
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.expressiveClickable
import com.sidharthify.breathe.util.displayAqi
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.util.getAqiColor
import kotlinx.coroutines.delay

@Composable
fun AirSnapshotSection(
    zones: List<AqiResponse>,
    isUsAqi: Boolean,
    onZoneClick: (AqiResponse) -> Unit,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = false,
    count: Int = 3,
) {
    val rankedZones =
        zones
            .filter { it.displayAqi(isUsAqi) > 0 }
            .sortedByDescending { it.displayAqi(isUsAqi) }

    if (rankedZones.size < 2) {
        return
    }

    val worstZones = rankedZones.take(count)
    val bestZones = rankedZones.reversed().take(count)

    Column(modifier = modifier) {
        Text(
            "Right Now",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Live ground sensor readings. Tap one to pin it",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SnapshotCard(
                title = "Most Polluted",
                icon = Icons.Filled.Warning,
                zones = worstZones,
                isUsAqi = isUsAqi,
                onZoneClick = onZoneClick,
                animateEntrance = animateEntrance,
                entranceOffset = 0,
                modifier = Modifier.weight(1f),
            )
            SnapshotCard(
                title = "Cleanest",
                icon = Icons.Filled.Air,
                zones = bestZones,
                isUsAqi = isUsAqi,
                onZoneClick = onZoneClick,
                animateEntrance = animateEntrance,
                entranceOffset = worstZones.size,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SnapshotCard(
    title: String,
    icon: ImageVector,
    zones: List<AqiResponse>,
    isUsAqi: Boolean,
    onZoneClick: (AqiResponse) -> Unit,
    animateEntrance: Boolean,
    entranceOffset: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            zones.forEachIndexed { index, zone ->
                SnapshotRow(
                    zone = zone,
                    isUsAqi = isUsAqi,
                    onClick = { onZoneClick(zone) },
                    entranceDelayMillis =
                        if (animateEntrance) {
                            (entranceOffset + index) * 70
                        } else {
                            null
                        },
                )
            }
        }
    }
}

@Composable
private fun SnapshotRow(
    zone: AqiResponse,
    isUsAqi: Boolean,
    onClick: () -> Unit,
    entranceDelayMillis: Int?,
) {
    val aqi = zone.displayAqi(isUsAqi)
    var isVisible by remember { mutableStateOf(entranceDelayMillis == null) }

    LaunchedEffect(Unit) {
        if (entranceDelayMillis != null) {
            delay(entranceDelayMillis.toLong())
            isVisible = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "SnapshotRowEntrance",
    )
    val animatedAqi by animateIntAsState(
        targetValue = if (isVisible) aqi else 0,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "SnapshotRowAqi",
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = progress.coerceIn(0f, 1f)
                    translationY = (1f - progress) * 24.dp.toPx()
                }.expressiveClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            zone.zoneName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "$animatedAqi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = getAqiColor(aqi, !isUsAqi),
        )
    }
}

@Composable
fun PinnedOverviewGrid(
    zones: List<AqiResponse>,
    isUsAqi: Boolean,
    liveZoneIds: Set<String>,
    columns: Int,
    onZoneClick: (AqiResponse) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        zones.chunked(columns).forEach { rowZones ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowZones.forEach { zone ->
                    PinnedOverviewCard(
                        zone = zone,
                        isUsAqi = isUsAqi,
                        isLive = zone.zoneId in liveZoneIds,
                        onClick = { onZoneClick(zone) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowZones.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PinnedOverviewCard(
    zone: AqiResponse,
    isUsAqi: Boolean,
    isLive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aqi = zone.displayAqi(isUsAqi)
    val aqiColor = getAqiColor(aqi, !isUsAqi)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = aqiColor.copy(alpha = 0.12f),
        modifier = modifier.expressiveClickable(onClick),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    zone.zoneName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isLive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CAF50), CircleShape),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$aqi",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = aqiColor,
            )
            Text(
                getAqiCategory(aqi, !isUsAqi).label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeDialog(
    snapshotZones: List<AqiResponse>,
    isUsAqi: Boolean,
    onZoneClick: (AqiResponse) -> Unit,
    onGoToExplore: () -> Unit,
) {
    val animationSettings = LocalAnimationSettings.current
    val entrance = remember { Animatable(if (animationSettings.screenTransitions) 0f else 1f) }
    val entranceSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()

    LaunchedEffect(Unit) {
        entrance.animateTo(1f, entranceSpec)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WelcomeBadge")
    val badgeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animationSettings.pulseEffects) 360f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "BadgeRotation",
    )

    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val scale = 0.85f + 0.15f * entrance.value
                        scaleX = scale
                        scaleY = scale
                        alpha = entrance.value.coerceIn(0f, 1f)
                        translationY = (1f - entrance.value) * 48.dp.toPx()
                    },
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .graphicsLayer { rotationZ = badgeRotation }
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialShapes.Cookie9Sided.toShape(),
                                ),
                    )
                    Icon(
                        Icons.Filled.Air,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Welcome to Breathe",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Breathe tracks the air across Jammu Kashmir & Ladakh. Pin a place to get started and it will live on your Home screen.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))

                AirSnapshotSection(
                    zones = snapshotZones,
                    isUsAqi = isUsAqi,
                    onZoneClick = onZoneClick,
                    animateEntrance = animationSettings.listAnimations,
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onGoToExplore,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Browse all locations")
                }
            }
        }
    }
}

@Composable
fun PinnedHintCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "That's how Breathe works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Your pinned locations live up here. Pin more from Explore or the Map, and tap one to switch to it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Got it")
                }
            }
        }
    }
}
