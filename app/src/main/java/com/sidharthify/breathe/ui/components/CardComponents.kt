// SPDX-License-Identifier: MIT
/*
 * CardComponents.kt - Reusable composable components for displaying zones, errors, and loading states in card layouts
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

package com.sidharthify.breathe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.ui.expressiveClickable
import com.sidharthify.breathe.util.displayAqi
import com.sidharthify.breathe.util.getAqiColor

@Composable
@ExperimentalMaterial3ExpressiveApi
fun PinnedZonesButtonGroup(
    modifier: Modifier = Modifier,
    zones: List<AqiResponse>,
    selectedZoneId: String?,
    useNaqi: Boolean,
    isAmoled: Boolean = false,
    onZoneSelected: (AqiResponse) -> Unit,
) {
    if (zones.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        zones.forEachIndexed { index, zone ->
            val isSelected = zone.zoneId == selectedZoneId

            val displayAqi = zone.displayAqi(useNaqi)

            val animationSettings = LocalAnimationSettings.current
            val aqiColor by animateColorAsState(
                targetValue = getAqiColor(displayAqi, !useNaqi),
                animationSpec = if (animationSettings.colorTransitions) {
                    tween(durationMillis = 500)
                } else {
                    tween(durationMillis = 0)
                },
                label = "ButtonGroupAqiColor",
            )

            // Border for AMOLED mode
            val borderStroke = if (isAmoled && !isSelected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            } else {
                null
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onZoneSelected(zone) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    zones.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                border = borderStroke,
            ) {
                // Checkmark icon for selected state
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                }

                // AQI indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(aqiColor),
                )
                Spacer(Modifier.width(8.dp))

                // Zone name
                Text(
                    text = zone.zoneName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun ZoneListItem(
    zone: Zone,
    isPinned: Boolean,
    onPinToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLive = zone.provider.equals("airgradient", ignoreCase = true)

    // the whole card is the pin toggle, the icon only shows the state
    Card(
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isPinned) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { stateDescription = if (isPinned) "Pinned" else "Not pinned" }
                .expressiveClickable(
                    role = Role.Switch,
                    onClickLabel = if (isPinned) "Unpin" else "Pin",
                    onClick = onPinToggle,
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(zone.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLive) {
                        LiveDot(size = 8.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        providerLabel(zone.provider),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = null,
                tint =
                    if (isPinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

fun providerLabel(provider: String?): String =
    when (provider?.lowercase()) {
        "openmeteo" -> "Satellite & Model Data"
        "airgradient" -> "Live Ground Sensors"
        else -> provider ?: "Unknown"
    }

// the little green dot that means a zone has real ground sensors
@Composable
fun LiveDot(
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .background(LiveGreen, CircleShape),
    )
}

val LiveGreen = Color(0xFF4CAF50)

@Composable
fun ErrorCard(
    msg: String,
    onRetry: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Connection Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(msg, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyStateCard(onGoToExplore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.PushPin, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Pinned Zones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pin locations to see them here",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Expressive Button
        Box(modifier = Modifier.expressiveClickable { onGoToExplore() }) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Text(
                    "Go to Explore",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
}

private val ReorderRowHeight = 64.dp
private val ReorderRowSpacing = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderPinnedZonesSheet(
    zones: List<AqiResponse>,
    useNaqi: Boolean,
    onReorder: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val orderedZones = remember { mutableStateListOf<AqiResponse>().apply { addAll(zones) } }
    var draggedZoneId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowStepPx = with(LocalDensity.current) { (ReorderRowHeight + ReorderRowSpacing).toPx() }

    fun move(
        from: Int,
        to: Int,
    ) {
        orderedZones.add(to, orderedZones.removeAt(from))
    }

    fun endDrag() {
        draggedZoneId = null
        dragOffset = 0f
        onReorder(orderedZones.map { it.zoneId })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                "Reorder Pinned Locations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Drag the handle or use the arrows",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(ReorderRowSpacing)) {
                orderedZones.forEachIndexed { index, zone ->
                    key(zone.zoneId) {
                        val isDragged = zone.zoneId == draggedZoneId

                        val displayAqi = zone.displayAqi(useNaqi)

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color =
                                if (isDragged) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                            shadowElevation = if (isDragged) 8.dp else 0.dp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(ReorderRowHeight)
                                    .zIndex(if (isDragged) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragged) dragOffset else 0f
                                    },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .size(48.dp)
                                            .padding(12.dp)
                                            .pointerInput(zone.zoneId) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedZoneId = zone.zoneId
                                                        dragOffset = 0f
                                                    },
                                                    onDragEnd = { endDrag() },
                                                    onDragCancel = { endDrag() },
                                                    onDrag = { change, amount ->
                                                        change.consume()
                                                        dragOffset += amount.y

                                                        val currentIndex = orderedZones.indexOfFirst { it.zoneId == zone.zoneId }
                                                        if (dragOffset > rowStepPx / 2 && currentIndex < orderedZones.lastIndex) {
                                                            move(currentIndex, currentIndex + 1)
                                                            dragOffset -= rowStepPx
                                                        } else if (dragOffset < -rowStepPx / 2 && currentIndex > 0) {
                                                            move(currentIndex, currentIndex - 1)
                                                            dragOffset += rowStepPx
                                                        }
                                                    },
                                                )
                                            },
                                )

                                Box(
                                    modifier =
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getAqiColor(displayAqi, !useNaqi)),
                                )
                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = zone.zoneName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )

                                IconButton(
                                    onClick = {
                                        move(index, index - 1)
                                        onReorder(orderedZones.map { it.zoneId })
                                    },
                                    enabled = index > 0,
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                                }

                                IconButton(
                                    onClick = {
                                        move(index, index + 1)
                                        onReorder(orderedZones.map { it.zoneId })
                                    },
                                    enabled = index < orderedZones.lastIndex,
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
