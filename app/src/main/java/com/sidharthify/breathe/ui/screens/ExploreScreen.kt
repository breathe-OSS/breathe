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

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.expressiveClickable
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.ui.components.ErrorCard
import com.sidharthify.breathe.ui.components.ZoneListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    error: String?,
    zones: List<Zone>,
    pinnedIds: Set<String>,
    query: String,
    onSearchChange: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val animationSettings = LocalAnimationSettings.current
    
    val filteredZones =
        remember(query, zones) {
            zones.filter {
                it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
            }
        }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Explore", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = query,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search city or station ID...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            shape = RoundedCornerShape(100),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading && zones.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (error != null && zones.isEmpty()) {
            ErrorCard(msg = error, onRetry = onRetry)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 360.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = LocalNavBarClearance.current),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (filteredZones.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("No zones found", modifier = Modifier.padding(8.dp))
                    }
                }

                items(filteredZones, key = { it.id }) { zone ->
                    Box(
                        modifier =
                            Modifier
                                .then(
                                    if (animationSettings.listAnimations) {
                                        Modifier.animateItem(tween(durationMillis = 300))
                                    } else {
                                        Modifier
                                    }
                                )
                                .expressiveClickable { onPinToggle(zone.id) },
                    ) {
                        ZoneListItem(
                            zone = zone,
                            isPinned = pinnedIds.contains(zone.id),
                            onPinClick = {},
                        )
                    }
                }
            }
        }
    }
}
