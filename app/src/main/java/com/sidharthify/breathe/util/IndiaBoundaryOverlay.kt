// SPDX-License-Identifier: MIT
/*
 * IndiaBoundaryOverlay.kt - Helper class to render India's official political boundaries on the map
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

package com.sidharthify.breathe.util

import android.content.Context
import android.graphics.Paint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

/**
 * Draws India's official boundaries for Jammu & Kashmir and Ladakh over the
 * tiles, district by district. OSM tiles only show the line of control, so
 * this overlay is what makes the map show the borders India recognises.
 *
 * Thanks to udit-001/india-maps-data for the GeoJSON data
 */
object IndiaBoundaryOverlay {
    private const val TAG = "IndiaBoundaryOverlay"
    private const val ID_PREFIX = "india_boundary_"
    private val FILES = listOf("jammu-and-kashmir.geojson", "ladakh.geojson")

    // every ring is drawn twice, a wide soft halo first and the crisp line on
    // top, so the border still reads over busy terrain and city labels
    data class Style(
        val lineColor: Int,
        val lineWidthPx: Float,
        val haloColor: Int,
        val haloWidthPx: Float,
    )

    private val loadLock = Mutex()
    private var cachedRings: List<List<GeoPoint>>? = null

    /**
     * Adds the boundary under everything else on the map, or just restyles it
     * if it's already there. Parsing only ever happens once per process and
     * never on the main thread.
     */
    suspend fun show(
        context: Context,
        mapView: MapView,
        style: Style,
    ) {
        val existing = boundaryOverlays(mapView)
        if (existing.isNotEmpty()) {
            existing.forEach { applyStyle(it, style) }
            mapView.invalidate()
            return
        }

        val rings = loadRings(context.applicationContext)
        val halos = rings.mapIndexed { index, ring -> buildPolygon(ring, "${ID_PREFIX}halo_$index") }
        val lines = rings.mapIndexed { index, ring -> buildPolygon(ring, "${ID_PREFIX}line_$index") }

        // index 0 is the bottom of the overlay stack, right above the tiles,
        // so markers always land on top. halos lead the list so they sit
        // under the lines
        (halos + lines).forEach { applyStyle(it, style) }
        mapView.overlays.addAll(0, halos + lines)
        mapView.invalidate()
    }

    private fun boundaryOverlays(mapView: MapView): List<Polygon> =
        mapView.overlays
            .filterIsInstance<Polygon>()
            .filter { it.id?.startsWith(ID_PREFIX) == true }

    private fun applyStyle(
        polygon: Polygon,
        style: Style,
    ) {
        val isHalo = polygon.id?.startsWith("${ID_PREFIX}halo_") == true
        polygon.fillPaint.color = 0x00000000
        polygon.outlinePaint.apply {
            color =
                if (isHalo) {
                    style.haloColor
                } else {
                    style.lineColor
                }
            strokeWidth =
                if (isHalo) {
                    style.haloWidthPx
                } else {
                    style.lineWidthPx
                }
            this.style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    private fun buildPolygon(
        ring: List<GeoPoint>,
        polygonId: String,
    ): Polygon =
        Polygon().apply {
            id = polygonId
            points = ring
            // keep taps going to the markers underneath, the border isn't interactive
            setOnClickListener { _, _, _ -> false }
        }

    private suspend fun loadRings(context: Context): List<List<GeoPoint>> =
        loadLock.withLock {
            cachedRings?.let { return@withLock it }
            val rings =
                withContext(Dispatchers.Default) {
                    FILES.flatMap { readRings(context, it) }
                }
            cachedRings = rings
            rings
        }

    // only the outer ring of each polygon is drawn, holes are skipped
    private fun readRings(
        context: Context,
        fileName: String,
    ): List<List<GeoPoint>> {
        val text =
            try {
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading GeoJSON file: $fileName", e)
                return emptyList()
            }

        val rings = mutableListOf<List<GeoPoint>>()
        val features = JSONObject(text).getJSONArray("features")
        for (i in 0 until features.length()) {
            val geometry = features.getJSONObject(i).getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")
            when (geometry.getString("type")) {
                "Polygon" -> {
                    rings += outerRing(coordinates)
                }

                "MultiPolygon" -> {
                    for (j in 0 until coordinates.length()) {
                        rings += outerRing(coordinates.getJSONArray(j))
                    }
                }
            }
        }
        return rings.filter { it.isNotEmpty() }
    }

    // geojson is [lon, lat], osmdroid wants it the other way round
    private fun outerRing(polygon: JSONArray): List<GeoPoint> {
        if (polygon.length() == 0) {
            return emptyList()
        }
        val ring = polygon.getJSONArray(0)
        return List(ring.length()) { index ->
            val coord = ring.getJSONArray(index)
            GeoPoint(coord.getDouble(1), coord.getDouble(0))
        }
    }
}
