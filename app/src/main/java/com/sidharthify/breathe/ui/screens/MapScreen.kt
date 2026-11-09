// SPDX-License-Identifier: MIT
/*
 * MapScreen.kt - OSM map of every zone with its AQI, tap one for the full dashboard
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val navBarClearance = LocalNavBarClearance.current
    val navBarClearancePx = with(LocalDensity.current) { navBarClearance.roundToPx() }
    val density = LocalDensity.current.density
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()

    val markerIcons = remember { MapMarkerIcons(context) }

    // keep the id, not the response, so the sheet picks up new readings
    // when a refresh lands while it's open
    var selectedZoneId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedZone = selectedZoneId?.let { id -> allAqiData.find { it.zoneId == id } }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // osmdroid has its own lifecycle and leaks its tile threads without this
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    mapViewRef?.onResume()
                }
                if (event == Lifecycle.Event.ON_PAUSE) {
                    mapViewRef?.onPause()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
        )

        // the extra pixels let the southern zones scroll up past the nav bar
        LaunchedEffect(mapViewRef, navBarClearancePx) {
            val map = mapViewRef ?: return@LaunchedEffect
            val limit = MapStyle.SCROLL_LIMIT
            map.setScrollableAreaLimitLatitude(limit.latNorth, limit.latSouth, navBarClearancePx)
            map.setScrollableAreaLimitLongitude(limit.lonWest, limit.lonEast, 0)
        }

        LaunchedEffect(mapViewRef, isDarkTheme, surfaceArgb) {
            val map = mapViewRef ?: return@LaunchedEffect
            map.overlayManager.tilesOverlay.apply {
                setColorFilter(MapStyle.tileFilter(isDarkTheme))
                // unloaded tiles otherwise flash a grey checkerboard while panning
                loadingBackgroundColor = surfaceArgb
                loadingLineColor = surfaceArgb
            }
            IndiaBoundaryOverlay.show(context, map, MapStyle.boundary(isDarkTheme, density))
            map.invalidate()
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
                                    selectedZoneId = zone.id
                                }
                                true
                            }
                        }
                    map.overlays.add(marker)
                }
            map.invalidate()
        }

        Text(
            MapStyle.ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = navBarClearance)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp),
                    ).padding(horizontal = 8.dp, vertical = 2.dp),
        )

        VerticalFloatingToolbar(
            expanded = true,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = navBarClearance),
            contentPadding = PaddingValues(4.dp),
        ) {
            IconButton(onClick = { mapViewRef?.controller?.zoomIn() }) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom in")
            }
            IconButton(onClick = { mapViewRef?.controller?.zoomOut() }) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
            }
            IconButton(
                onClick = {
                    mapViewRef?.controller?.animateTo(MapStyle.HOME_CENTER, MapStyle.HOME_ZOOM, 600L)
                },
            ) {
                Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Show all zones")
            }
        }
    }

    if (selectedZone != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedZoneId = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                // up top, the dashboard below is long enough that nobody found it
                // at the bottom
                val isPinned = selectedZone.zoneId in pinnedIds
                FilledTonalButton(
                    onClick = { onPinToggle(selectedZone.zoneId) },
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(horizontal = 24.dp),
                ) {
                    Icon(
                        if (isPinned) {
                            Icons.Filled.PushPin
                        } else {
                            Icons.Outlined.PushPin
                        },
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPinned) {
                            "Unpin from Home"
                        } else {
                            "Pin to Home"
                        },
                    )
                }

                MainDashboardDetail(
                    zone = selectedZone,
                    provider = zones.find { it.id == selectedZone.zoneId }?.provider,
                    isDarkTheme = isDarkTheme,
                    useNaqi = useNaqi,
                    sensorInfos = sensorInfos,
                )
            }
        }
    }
}
