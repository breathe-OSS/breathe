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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.sidharthify.breathe.ui.components.SensorDetails
import com.sidharthify.breathe.util.BreatheTileProvider
import com.sidharthify.breathe.util.IndiaBoundaryOverlay
import com.sidharthify.breathe.util.MapCluster
import com.sidharthify.breathe.util.MapMarkerIcons
import com.sidharthify.breathe.util.MapStyle
import com.sidharthify.breathe.util.SensorPoint
import com.sidharthify.breathe.util.buildSensorPoints
import com.sidharthify.breathe.util.clusterForZoom
import com.sidharthify.breathe.util.displayAqi
import com.sidharthify.breathe.util.getAqiColor
import kotlin.math.roundToInt
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
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

    // clusters only care about zoom, and only in half steps, so pinching
    // doesn't rebuild every marker on every frame
    var zoomStep by remember { mutableDoubleStateOf(MapStyle.HOME_ZOOM) }
    val clusterDistancePx = with(LocalDensity.current) { 40.dp.toPx() }

    var layer by rememberSaveable { mutableStateOf(MapLayer.Sensors) }
    var selectedSensorKey by rememberSaveable { mutableStateOf<String?>(null) }
    val sensorPoints = remember(allAqiData, sensorInfos) { buildSensorPoints(allAqiData, sensorInfos) }
    val selectedSensor = selectedSensorKey?.let { key -> sensorPoints.find { it.key == key } }

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
                MapView(ctx, BreatheTileProvider(ctx.applicationContext)).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    minZoomLevel = 7.0
                    maxZoomLevel = 18.0

                    controller.setZoom(MapStyle.HOME_ZOOM)
                    controller.setCenter(MapStyle.HOME_CENTER)

                    addMapListener(
                        object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean = false

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                val level = event?.zoomLevel ?: return false
                                zoomStep = (level * 2).roundToInt() / 2.0
                                return false
                            }
                        },
                    )

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

        LaunchedEffect(mapViewRef, layer, zones, allAqiData, sensorPoints, useNaqi, surfaceArgb, zoomStep) {
            val map = mapViewRef ?: return@LaunchedEffect

            map.overlays.removeAll { it is Marker }

            val aqiById = allAqiData.associateBy { it.zoneId }
            val zonesWithSensors =
                if (layer == MapLayer.Sensors) {
                    sensorPoints.map { it.zoneId }.toSet()
                } else {
                    emptySet()
                }

            val bubbles = mutableListOf<MapBubble>()

            // in the sensor layer a zone with live sensors is replaced by them,
            // zones that only have modelled data keep one see through bubble
            zones.forEach { zone ->
                val lat = zone.lat ?: return@forEach
                val lon = zone.lon ?: return@forEach
                if (zone.id in zonesWithSensors) {
                    return@forEach
                }
                val isLive = zone.provider.equals("airgradient", ignoreCase = true)
                bubbles +=
                    MapBubble(
                        point = GeoPoint(lat, lon),
                        title = zone.name,
                        aqi = aqiById[zone.id]?.displayAqi(useNaqi),
                        diameterDp = 34f,
                        showLiveDot = isLive && layer == MapLayer.Districts,
                        muted = layer == MapLayer.Sensors && !isLive,
                        onClick = {
                            if (aqiById[zone.id] != null) {
                                selectedZoneId = zone.id
                            }
                        },
                    )
            }

            if (layer == MapLayer.Sensors) {
                sensorPoints.forEach { sensor ->
                    bubbles +=
                        MapBubble(
                            point = GeoPoint(sensor.latitude, sensor.longitude),
                            title = sensor.name,
                            aqi =
                                if (sensor.reading.pm25 != null) {
                                    sensor.reading.displayAqi(useNaqi)
                                } else {
                                    null
                                },
                            diameterDp = 30f,
                            showLiveDot = true,
                            muted = false,
                            onClick = { selectedSensorKey = sensor.key },
                        )
                }
            }

            val clusters =
                clusterForZoom(
                    items = bubbles,
                    zoom = zoomStep,
                    minDistancePx = clusterDistancePx,
                    latitude = { it.point.latitude },
                    longitude = { it.point.longitude },
                    priority = { it.aqi ?: -1 },
                )

            // osmdroid draws in list order, so the worst air goes in last and
            // ends up on top wherever bubbles still overlap
            clusters
                .map { cluster -> clusterBubble(cluster, map) }
                .sortedBy { it.aqi ?: -1 }
                .forEach { bubble ->
                    val spec =
                        MapMarkerIcons.Spec(
                            text = bubble.aqi?.toString() ?: "",
                            fillColor =
                                if (bubble.aqi != null) {
                                    getAqiColor(bubble.aqi, !useNaqi).toArgb()
                                } else {
                                    MapMarkerIcons.NO_DATA_GREY
                                },
                            ringColor = surfaceArgb,
                            diameterDp = bubble.diameterDp,
                            showLiveDot = bubble.showLiveDot,
                            muted = bubble.muted,
                            count = bubble.count,
                        )
                    val marker =
                        Marker(map).apply {
                            position = bubble.point
                            title = bubble.title
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = markerIcons.get(spec).toDrawable(resources)
                            setOnMarkerClickListener { _, _ ->
                                bubble.onClick()
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

        MapLayerToggle(
            layer = layer,
            onLayerChange = { layer = it },
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
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

    if (selectedSensor != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSensorKey = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SensorDetails(
                    nodeName = selectedSensor.name,
                    reading = selectedSensor.reading,
                    useNaqi = useNaqi,
                    sensorInfo = selectedSensor.info,
                )
                // hands over to the zone sheet for the averages, graphs and pinning
                FilledTonalButton(
                    onClick = {
                        selectedSensorKey = null
                        selectedZoneId = selectedSensor.zoneId
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open ${selectedSensor.info.zone}")
                }
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

private enum class MapLayer(
    val label: String,
) {
    Sensors("Sensors"),
    Districts("Districts"),
}

// one bubble on the map, either a whole zone or a single sensor
private data class MapBubble(
    val point: GeoPoint,
    val title: String,
    val aqi: Int?,
    val diameterDp: Float,
    val showLiveDot: Boolean,
    val muted: Boolean,
    val onClick: () -> Unit,
    val count: Int = 1,
)

// sensor names only have to be unique inside their zone
private val SensorPoint.key: String
    get() = "$zoneId/$name"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MapLayerToggle(
    layer: MapLayer,
    onLayerChange: (MapLayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        MapLayer.entries.forEachIndexed { index, entry ->
            ToggleButton(
                checked = layer == entry,
                onCheckedChange = { onLayerChange(entry) },
                shapes =
                    if (index == 0) {
                        ButtonGroupDefaults.connectedLeadingButtonShapes()
                    } else {
                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                modifier = Modifier.semantics { role = Role.RadioButton },
            ) {
                Icon(
                    if (entry == MapLayer.Sensors) {
                        Icons.Filled.Sensors
                    } else {
                        Icons.Filled.Layers
                    },
                    contentDescription = null,
                    modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(entry.label)
            }
        }
    }
}

// a lone bubble passes straight through. a group shows its worst reading
// (the number you'd want to know about) with a count badge, and tapping it
// zooms in far enough to pull the group apart
private fun clusterBubble(
    cluster: MapCluster<MapBubble>,
    map: MapView,
): MapBubble {
    val members = cluster.members
    if (members.size == 1) {
        return members.first()
    }
    val center = GeoPoint(cluster.latitude, cluster.longitude)
    return MapBubble(
        point = center,
        title = "${members.size} places",
        aqi = members.mapNotNull { it.aqi }.maxOrNull(),
        diameterDp = 36f,
        showLiveDot = members.any { it.showLiveDot },
        muted = members.all { it.muted },
        count = members.size,
        onClick = {
            val box = BoundingBox.fromGeoPointsSafe(members.map { it.point })
            if (box.latitudeSpan < 0.001 && box.longitudeSpanWithDateLine < 0.001) {
                map.controller.animateTo(center, (map.zoomLevelDouble + 2).coerceAtMost(map.maxZoomLevel), 500L)
            } else {
                map.zoomToBoundingBox(box.increaseByScale(1.8f), true)
            }
        },
    )
}
