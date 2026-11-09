// SPDX-License-Identifier: MIT
/*
 * MapScreen.kt - Composable function for the Map screen, displaying zones on an interactive map with AQI markers
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

package com.sidharthify.breathe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.SensorInfo
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.ui.components.MainDashboardDetail
import com.sidharthify.breathe.util.IndiaBoundaryOverlay
import com.sidharthify.breathe.util.MapMarkerIcons
import com.sidharthify.breathe.util.MapStyle
import com.sidharthify.breathe.util.displayAqi
import com.sidharthify.breathe.util.getAqiColor
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    zones: List<Zone>,
    allAqiData: List<AqiResponse>,
    pinnedIds: Set<String>,
    isDarkTheme: Boolean,
    useNaqi: Boolean,
    sensorInfos: List<SensorInfo>,
    onPinToggle: (String) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navBarClearancePx = with(LocalDensity.current) { LocalNavBarClearance.current.roundToPx() }
    val density = LocalDensity.current.density
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()

    val markerIcons = remember { MapMarkerIcons(context) }

    var selectedZoneData by remember { mutableStateOf<AqiResponse?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) mapViewRef?.onResume()
                if (event == Lifecycle.Event.ON_PAUSE) mapViewRef?.onPause()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    Scaffold { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        minZoomLevel = 7.0
                        maxZoomLevel = 18.0

                        controller.setZoom(MapStyle.HOME_ZOOM)
                        controller.setCenter(MapStyle.HOME_CENTER)

                        mapViewRef = this
                    }
                },
                update = { _ -> },
            )

            Text(
                MapStyle.ATTRIBUTION,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = LocalNavBarClearance.current)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp),
                        ).padding(horizontal = 8.dp, vertical = 2.dp),
            )

            LaunchedEffect(mapViewRef, navBarClearancePx) {
                mapViewRef?.let { map ->
                    val limit = MapStyle.SCROLL_LIMIT
                    map.setScrollableAreaLimitLatitude(limit.latNorth, limit.latSouth, navBarClearancePx)
                    map.setScrollableAreaLimitLongitude(limit.lonWest, limit.lonEast, 0)
                }
            }

            LaunchedEffect(mapViewRef, isDarkTheme, surfaceArgb) {
                mapViewRef?.let { map ->
                    map.overlayManager.tilesOverlay.setColorFilter(MapStyle.tileFilter(isDarkTheme))
                    // unloaded tiles otherwise flash a grey checkerboard while panning
                    map.overlayManager.tilesOverlay.apply {
                        loadingBackgroundColor = surfaceArgb
                        loadingLineColor = surfaceArgb
                    }

                    IndiaBoundaryOverlay.show(context, map, MapStyle.boundary(isDarkTheme, density))

                    map.invalidate()
                }
            }

            LaunchedEffect(mapViewRef, zones, allAqiData, useNaqi, surfaceArgb) {
                val map = mapViewRef ?: return@LaunchedEffect

                map.overlays.removeAll { it is Marker }

                val aqiById = allAqiData.associateBy { it.zoneId }
                val placed =
                    zones.mapNotNull { zone ->
                        val lat = zone.lat ?: return@mapNotNull null
                        val lon = zone.lon ?: return@mapNotNull null
                        Triple(zone, GeoPoint(lat, lon), aqiById[zone.id])
                    }

                // osmdroid draws in list order, so the worst air goes in last and
                // ends up on top wherever bubbles overlap
                placed
                    .sortedBy { (_, _, data) -> data?.displayAqi(useNaqi) ?: -1 }
                    .forEach { (zone, point, data) ->
                        val aqi = data?.displayAqi(useNaqi)
                        val spec =
                            MapMarkerIcons.Spec(
                                text = aqi?.toString() ?: "",
                                fillColor =
                                    if (aqi != null) {
                                        getAqiColor(aqi, !useNaqi).toArgb()
                                    } else {
                                        MapMarkerIcons.NO_DATA_GREY
                                    },
                                ringColor = surfaceArgb,
                                diameterDp = 34f,
                                showLiveDot = zone.provider.equals("airgradient", ignoreCase = true),
                            )
                        val marker =
                            Marker(map).apply {
                                position = point
                                title = zone.name
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = markerIcons.get(spec).toDrawable(resources)
                                setOnMarkerClickListener { _, _ ->
                                    if (data != null) {
                                        selectedZoneData = data
                                    }
                                    true
                                }
                            }
                        map.overlays.add(marker)
                    }
                map.invalidate()
            }
            Column(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }
        }

        if (selectedZoneData != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedZoneData = null },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 48.dp),
                ) {
                    val provider = zones.find { it.id == selectedZoneData!!.zoneId }?.provider

                    // Pass useNaqi here too
                    MainDashboardDetail(
                        zone = selectedZoneData!!,
                        provider = provider,
                        isDarkTheme = isDarkTheme,
                        useNaqi = useNaqi,
                        sensorInfos = sensorInfos,
                    )

                    val isPinned = pinnedIds.contains(selectedZoneData!!.zoneId)
                    Box(Modifier.padding(horizontal = 24.dp)) {
                        OutlinedButton(
                            onClick = { onPinToggle(selectedZoneData!!.zoneId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isPinned) "Unpin from Home" else "Pin to Home")
                        }
                    }
                }
            }
        }
    }
}

