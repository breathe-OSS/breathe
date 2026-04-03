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
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

/**
 * Helper class to render India's official political boundaries
 * Thanks to udit-001/india-maps-data for the GeoJSON data
 */
object IndiaBoundaryOverlay {
    private const val TAG = "IndiaBoundaryOverlay"

    // Border styling
    private const val BORDER_WIDTH = 2.5f

    /**
     * Loads and adds J&K and Ladakh boundary overlays to the map
     * @param context
     * @param mapView
     * @param isDarkTheme
     */
    fun addBoundaryOverlay(
        context: Context,
        mapView: MapView,
        isDarkTheme: Boolean,
    ) {
        try {
            // Load J&K boundary
            val jkGeoJson = loadGeoJsonFromAssets(context, "jammu-and-kashmir.geojson")
            if (jkGeoJson != null) {
                addOutlineFromGeoJson(jkGeoJson, mapView, isDarkTheme, "jk")
            }

            // Load Ladakh boundary
            val ladakhGeoJson = loadGeoJsonFromAssets(context, "ladakh.geojson")
            if (ladakhGeoJson != null) {
                addOutlineFromGeoJson(ladakhGeoJson, mapView, isDarkTheme, "ladakh")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading boundary overlay", e)
        }
    }

    /**
     * Removes all boundary overlays from the map
     */
    fun removeBoundaryOverlays(mapView: MapView) {
        val overlaysToRemove =
            mapView.overlays
                .filterIsInstance<Polygon>()
                .filter { it.id?.startsWith("india_boundary_") == true }
        overlaysToRemove.forEach { mapView.overlays.remove(it) }
    }

    private fun loadGeoJsonFromAssets(
        context: Context,
        fileName: String,
    ): String? =
        try {
            context.assets
                .open(fileName)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading GeoJSON file: $fileName", e)
            null
        }

    private fun addOutlineFromGeoJson(
        geoJsonString: String,
        mapView: MapView,
        isDarkTheme: Boolean,
        prefix: String,
    ) {
        val json = JSONObject(geoJsonString)
        val features = json.getJSONArray("features")

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val geometryType = geometry.getString("type")

            when (geometryType) {
                "Polygon" -> {
                    val polygon =
                        createPolygonOverlay(
                            geometry.getJSONArray("coordinates"),
                            isDarkTheme,
                        )
                    polygon.id = "india_boundary_${prefix}_$i"
                    mapView.overlays.add(0, polygon)
                }

                "MultiPolygon" -> {
                    val coordinates = geometry.getJSONArray("coordinates")
                    for (j in 0 until coordinates.length()) {
                        val polygon =
                            createPolygonOverlay(
                                coordinates.getJSONArray(j),
                                isDarkTheme,
                            )
                        polygon.id = "india_boundary_${prefix}_${i}_$j"
                        mapView.overlays.add(0, polygon)
                    }
                }
            }
        }
    }

    private fun createPolygonOverlay(
        coordinates: JSONArray,
        isDarkTheme: Boolean,
    ): Polygon {
        val polygon = Polygon()

        if (coordinates.length() > 0) {
            val ring = coordinates.getJSONArray(0)
            val geoPoints = mutableListOf<GeoPoint>()

            for (i in 0 until ring.length()) {
                val coord = ring.getJSONArray(i)
                val lon = coord.getDouble(0)
                val lat = coord.getDouble(1)
                geoPoints.add(GeoPoint(lat, lon))
            }

            polygon.points = geoPoints
        }

        polygon.fillPaint.color = 0x00000000
        polygon.outlinePaint.color = if (isDarkTheme) Color.WHITE else Color.BLACK
        polygon.outlinePaint.strokeWidth = BORDER_WIDTH
        polygon.outlinePaint.style = Paint.Style.STROKE
        polygon.outlinePaint.isAntiAlias = true

        return polygon
    }
}
