// SPDX-License-Identifier: MIT
/*
 * ExploreScreen.kt - Composable function for the Explore screen, allowing users to search and pin zones
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
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

package com.sidharthify.breathe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.data.SensorInfo
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.ui.components.ErrorCard
import com.sidharthify.breathe.ui.components.LiveDot
import com.sidharthify.breathe.ui.components.MainDashboardDetail
import com.sidharthify.breathe.ui.components.ZoneOverview
import com.sidharthify.breathe.ui.expressiveClickable
import com.sidharthify.breathe.util.displayAqi
import com.sidharthify.breathe.util.divisionRank
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.isEstimate
import com.sidharthify.breathe.util.isSensorOffline
import com.sidharthify.breathe.util.onAqiColor

private data class DistrictGroup(
    val key: String,
    val district: String,
    val division: String,
    val places: List<Zone>,
)

private fun groupPlaces(zones: List<Zone>): List<DistrictGroup> =
    zones
        .groupBy { "${it.division ?: "Other"}/${it.district ?: it.name}" }
        .map { (key, places) ->
            DistrictGroup(
                key = key,
                district = places.first().district ?: places.first().name,
                division = places.first().division ?: "Other",
                places = places.sortedWith(compareBy<Zone> { it.isEstimate }.thenBy { it.name }),
            )
        }.sortedWith(compareBy<DistrictGroup> { divisionRank(it.division) }.thenBy { it.district })

private fun districtSummary(group: DistrictGroup): String {
    val sensors = group.places.count { !it.isEstimate }
    val estimates = group.places.size - sensors
    if (group.places.size == 1) {
        val only = group.places.first()
        if (only.isEstimate) {
            if (only.name == group.district) {
                return "Satellite estimate"
            }
            return "${only.name} · satellite estimate"
        }
        return "${only.name} · 1 sensor"
    }
    val parts = mutableListOf<String>()
    if (sensors > 0) {
        parts +=
            if (sensors == 1) {
                "1 sensor"
            } else {
                "$sensors sensors"
            }
    }
    if (estimates > 0) {
        parts +=
            if (estimates == 1) {
                "1 satellite estimate"
            } else {
                "$estimates satellite estimates"
            }
    }
    return parts.joinToString(", ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    error: String?,
    zones: List<Zone>,
    allAqiData: List<AqiResponse>,
    pinnedIds: Set<String>,
    useNaqi: Boolean,
    sensorInfos: List<SensorInfo>,
    query: String,
    onSearchChange: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    onRetry: () -> Unit,
    rawMode: Boolean = false,
    rawHintHidden: Boolean = true,
    onRawModeChange: (Boolean) -> Unit = {},
    onHideRawHint: () -> Unit = {},
) {
    val animationSettings = LocalAnimationSettings.current
    val aqiById = remember(allAqiData) { allAqiData.associateBy { it.zoneId } }
    val groups = remember(zones) { groupPlaces(zones) }
    var openDistricts by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showFullDetails by rememberSaveable { mutableStateOf(false) }
    val needle = query.trim()

    val visible =
        remember(groups, needle) {
            groups.mapNotNull { group ->
                if (needle.isEmpty() || group.district.contains(needle, ignoreCase = true)) {
                    return@mapNotNull group
                }
                val hits = group.places.filter { it.name.contains(needle, ignoreCase = true) || it.id.contains(needle, ignoreCase = true) }
                if (hits.isEmpty()) {
                    null
                } else {
                    group.copy(places = hits)
                }
            }
        }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Explore", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = query,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search sensor or district") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(100),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && zones.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (error != null && zones.isEmpty()) {
            ErrorCard(msg = error, onRetry = onRetry)
        } else {
            BoxWithConstraints {
            val multiColumn = maxWidth >= 744.dp
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 360.dp),
                verticalArrangement =
                    Arrangement.spacedBy(
                        if (multiColumn) {
                            12.dp
                        } else {
                            3.dp
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = LocalNavBarClearance.current),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (visible.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("No sensor or district matches that.", modifier = Modifier.padding(8.dp))
                    }
                }

                visible
                    .groupBy { it.division }
                    .forEach { (division, divisionGroups) ->
                        item(key = "division_$division", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                division,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
                            )
                        }
                        itemsIndexed(divisionGroups, key = { _, it -> it.key }) { index, group ->
                            val expanded = needle.isNotEmpty() || group.key in openDistricts
                            DistrictCard(
                                group = group,
                                expanded = expanded,
                                shape =
                                    if (multiColumn) {
                                        RoundedCornerShape(24.dp)
                                    } else {
                                        segmentShape(index, divisionGroups.size)
                                    },
                                aqiById = aqiById,
                                useNaqi = useNaqi,
                                pinnedIds = pinnedIds,
                                onToggle = {
                                    openDistricts =
                                        if (group.key in openDistricts) {
                                            openDistricts - group.key
                                        } else {
                                            openDistricts + group.key
                                        }
                                },
                                onOpen = { id ->
                                    if (aqiById[id] != null) {
                                        showFullDetails = false
                                        selectedId = id
                                    }
                                },
                                onPinToggle = onPinToggle,
                                modifier =
                                    if (animationSettings.listAnimations) {
                                        Modifier.animateItem(tween(durationMillis = 300))
                                    } else {
                                        Modifier
                                    },
                            )
                        }
                    }
            }
            }
        }
    }

    val selected = selectedId?.let { aqiById[it] }
    if (selected != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedId = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                if (showFullDetails) {
                    MainDashboardDetail(
                        zone = selected,
                        provider = zones.find { it.id == selected.zoneId }?.provider,
                        isDarkTheme = isDarkTheme,
                        useNaqi = useNaqi,
                        sensorInfos = sensorInfos,
                        rawMode = rawMode,
                        rawHintHidden = rawHintHidden,
                        onRawModeChange = onRawModeChange,
                        onHideRawHint = onHideRawHint,
                    )
                } else {
                    ZoneOverview(
                        zone = selected,
                        isPinned = selected.zoneId in pinnedIds,
                        useNaqi = useNaqi,
                        onPinToggle = { onPinToggle(selected.zoneId) },
                        onOpenDetails = { showFullDetails = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun DistrictCard(
    group: DistrictGroup,
    expanded: Boolean,
    shape: Shape,
    aqiById: Map<String, AqiResponse>,
    useNaqi: Boolean,
    pinnedIds: Set<String>,
    onToggle: () -> Unit,
    onOpen: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val single = group.places.size == 1
    val worst = group.places.mapNotNull { aqiById[it.id]?.displayAqi(useNaqi) }.maxOrNull()
    val chevron by animateFloatAsState(
        targetValue =
            if (expanded) {
                180f
            } else {
                0f
            },
        label = "DistrictChevron",
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .expressiveClickable(
                            onClick = {
                                if (single) {
                                    onOpen(group.places.first().id)
                                } else {
                                    onToggle()
                                }
                            },
                        ).padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${group.district} district",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val only = group.places.first()
                        if (single && !only.isEstimate) {
                            LiveDot(offline = aqiById[only.id]?.isSensorOffline == true)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            if (single && !only.isEstimate && aqiById[only.id]?.isSensorOffline == true) {
                                "${only.name} · offline, showing a satellite estimate"
                            } else {
                                districtSummary(group)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                AqiChip(aqi = worst, useNaqi = useNaqi)
                if (single) {
                    PinButton(
                        pinned = group.places.first().id in pinnedIds,
                        name = group.places.first().name,
                        onClick = { onPinToggle(group.places.first().id) },
                    )
                } else {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription =
                            if (expanded) {
                                "Collapse"
                            } else {
                                "Expand"
                            },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp).rotate(chevron),
                    )
                }
            }
            if (!single) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        group.places.forEach { place ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            PlaceRow(
                                place = place,
                                data = aqiById[place.id],
                                useNaqi = useNaqi,
                                pinned = place.id in pinnedIds,
                                onOpen = { onOpen(place.id) },
                                onPinToggle = { onPinToggle(place.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun segmentShape(
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
private fun PlaceRow(
    place: Zone,
    data: AqiResponse?,
    useNaqi: Boolean,
    pinned: Boolean,
    onOpen: () -> Unit,
    onPinToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .expressiveClickable(onClick = onOpen)
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                place.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val offline = data?.isSensorOffline == true
                if (!place.isEstimate) {
                    LiveDot(offline = offline)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    when {
                        place.isEstimate -> "Satellite estimate"
                        offline -> "Offline, showing a satellite estimate"
                        else -> "Live sensor"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AqiChip(aqi = data?.displayAqi(useNaqi), useNaqi = useNaqi)
        PinButton(pinned = pinned, name = place.name, onClick = onPinToggle)
    }
}

@Composable
private fun AqiChip(
    aqi: Int?,
    useNaqi: Boolean,
) {
    if (aqi == null) {
        Text(
            "--",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        return
    }
    val color = getAqiColor(aqi, !useNaqi)
    Surface(
        color = color,
        shape = RoundedCornerShape(100),
        border = BorderStroke(2.dp, Color.White),
    ) {
        Text(
            "$aqi",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color.onAqiColor(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PinButton(
    pinned: Boolean,
    name: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            if (pinned) {
                Icons.Filled.PushPin
            } else {
                Icons.Outlined.PushPin
            },
            contentDescription =
                if (pinned) {
                    "Unpin $name"
                } else {
                    "Pin $name"
                },
            tint =
                if (pinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}
