package com.sidharthify.breathe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
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
    onPinToggle: (String) -> Unit
) {
    val context = LocalContext.current
    val startPoint = GeoPoint(34.0837, 74.7973)

    var selectedZoneData by remember { mutableStateOf<AqiResponse?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
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

                        val jkRegion = BoundingBox(37.5, 81.0, 32.0, 72.0)
                        setScrollableAreaLimitDouble(jkRegion)
                        minZoomLevel = 7.0
                        maxZoomLevel = 20.0

                        controller.setZoom(9.0)
                        controller.setCenter(startPoint)

                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    val tilesOverlay = mapView.overlayManager.tilesOverlay
                    if (isDarkTheme) {
                        val inverseMatrix = ColorMatrix(
                            floatArrayOf(
                                -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                                0.0f, -1.0f, 0.0f, 0.0f, 255f,
                                0.0f, 0.0f, -1.0f, 0.0f, 255f,
                                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                            )
                        )
                        tilesOverlay.setColorFilter(ColorMatrixColorFilter(inverseMatrix))
                    } else {
                        tilesOverlay.setColorFilter(null)
                    }

                    mapView.overlays.clear()
                    zones.forEach { zone ->
                        if (zone.lat != null && zone.lon != null) {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(zone.lat, zone.lon)
                            marker.title = zone.name
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                            val data = allAqiData.find { it.zoneId == zone.id }

                            val colorInt = if (data != null) {
                                getAqiColor(data.nAqi).toArgb()
                            } else {
                                android.graphics.Color.GRAY
                            }

                            val aqiText = data?.nAqi?.toString() ?: ""

                            marker.icon = createNumberedMarker(context, aqiText, colorInt)

                            marker.setOnMarkerClickListener { _, _ ->
                                if (data != null) {
                                    selectedZoneData = data
                                }
                                true
                            }
                            mapView.overlays.add(marker)
                        }
                    }
                    mapView.invalidate()
                }
            )

            // Custom MD3 Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }
        }

        if (selectedZoneData != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedZoneData = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 48.dp)
                ) {
                    val provider = zones.find { it.id == selectedZoneData!!.zoneId }?.provider
                    MainDashboardDetail(selectedZoneData!!, provider, isDarkTheme)

                    val isPinned = pinnedIds.contains(selectedZoneData!!.zoneId)
                    Box(Modifier.padding(horizontal = 24.dp)) {
                        OutlinedButton(
                            onClick = { onPinToggle(selectedZoneData!!.zoneId) },
                            modifier = Modifier.fillMaxWidth()
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

fun createNumberedMarker(context: Context, text: String, color: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (40 * density).toInt() 
    val textSizePx = 14f * density

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

    if (text.isNotEmpty()) {
        val textPaint = Paint().apply {
            this.color = Color.BLACK
            textSize = textSizePx
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(1.5f, 0f, 0f, Color.DKGRAY)
        }

        val xPos = sizePx / 2f
        val yPos = (sizePx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2)

        canvas.drawText(text, xPos, yPos, textPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}