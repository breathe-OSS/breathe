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
    fun addBoundaryOverlay(context: Context, mapView: MapView, isDarkTheme: Boolean) {
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
        val overlaysToRemove = mapView.overlays.filterIsInstance<Polygon>()
            .filter { it.id?.startsWith("india_boundary_") == true }
        overlaysToRemove.forEach { mapView.overlays.remove(it) }
    }
    
    private fun loadGeoJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading GeoJSON file: $fileName", e)
            null
        }
    }
    
    private fun addOutlineFromGeoJson(
        geoJsonString: String, 
        mapView: MapView, 
        isDarkTheme: Boolean,
        prefix: String
    ) {
        val json = JSONObject(geoJsonString)
        val features = json.getJSONArray("features")
        
        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val geometryType = geometry.getString("type")
            
            when (geometryType) {
                "Polygon" -> {
                    val polygon = createPolygonOverlay(
                        geometry.getJSONArray("coordinates"),
                        isDarkTheme
                    )
                    polygon.id = "india_boundary_${prefix}_$i"
                    mapView.overlays.add(0, polygon)
                }
                "MultiPolygon" -> {
                    val coordinates = geometry.getJSONArray("coordinates")
                    for (j in 0 until coordinates.length()) {
                        val polygon = createPolygonOverlay(
                            coordinates.getJSONArray(j),
                            isDarkTheme
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
        isDarkTheme: Boolean
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
