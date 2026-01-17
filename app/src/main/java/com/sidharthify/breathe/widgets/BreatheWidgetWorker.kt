package com.sidharthify.breathe.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.sidharthify.breathe.data.RetrofitClient

class BreatheWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        val PREF_ZONE_ID = stringPreferencesKey("zone_id")
        val PREF_ZONE_NAME = stringPreferencesKey("zone_name")
        val PREF_AQI = intPreferencesKey("aqi")
        val PREF_PROVIDER = stringPreferencesKey("provider")
        val PREF_STATUS = stringPreferencesKey("status")
        val PREF_CURRENT_INDEX = intPreferencesKey("current_index")
        val PREF_TOTAL_PINS = intPreferencesKey("total_pins")
        val PREF_IS_US_AQI = booleanPreferencesKey("is_us_aqi")

        val PREF_PM25 = doublePreferencesKey("pm25")
        val PREF_PM10 = doublePreferencesKey("pm10")
        val PREF_NO2 = doublePreferencesKey("no2")
        val PREF_SO2 = doublePreferencesKey("so2")
        val PREF_CO = doublePreferencesKey("co")
        val PREF_O3 = doublePreferencesKey("o3")
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(BreatheWidget::class.java)

        val appPrefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
        val pinnedIds = (appPrefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()).sorted()
        val isUsAqi = appPrefs.getBoolean("is_us_aqi", false) // READ PREF

        glanceIds.forEach { glanceId ->
            try {
                updateWidgetForId(context, glanceId, pinnedIds, isUsAqi)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success()
    }

    private suspend fun updateWidgetForId(
        context: Context, 
        glanceId: GlanceId, 
        pinnedIds: List<String>,
        isUsAqi: Boolean
    ) {
        var currentIndex = 0
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            currentIndex = prefs[PREF_CURRENT_INDEX] ?: 0
            prefs
        }

        if (pinnedIds.isEmpty()) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply { this[PREF_STATUS] = "Empty" }
            }
            BreatheWidget().update(context, glanceId)
            return
        }

        if (currentIndex >= pinnedIds.size) currentIndex = 0
        val currentZoneId = pinnedIds[currentIndex]

        try {
            val response = RetrofitClient.api.getZoneAqi(currentZoneId)
            val concentrations = response.concentrations ?: emptyMap()
            
            val providerName = if(currentZoneId.contains("srinagar", true)) "AirGradient" else "OpenMeteo"

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[PREF_ZONE_ID] = response.zoneId
                    this[PREF_AQI] = response.nAqi
                    this[PREF_ZONE_NAME] = response.zoneName
                    this[PREF_PROVIDER] = "Source: $providerName"
                    this[PREF_STATUS] = "Success"
                    this[PREF_CURRENT_INDEX] = currentIndex
                    this[PREF_TOTAL_PINS] = pinnedIds.size
                    this[PREF_IS_US_AQI] = isUsAqi // STORE IN WIDGET STATE

                    this[PREF_PM25] = concentrations["pm2_5"] ?: -1.0
                    this[PREF_PM10] = concentrations["pm10"] ?: -1.0
                    this[PREF_NO2] = concentrations["no2"] ?: -1.0
                    this[PREF_SO2] = concentrations["so2"] ?: -1.0
                    this[PREF_CO] = concentrations["co"] ?: -1.0
                    this[PREF_O3] = concentrations["o3"] ?: -1.0
                }
            }
        } catch (e: Exception) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply { this[PREF_STATUS] = "Error" }
            }
        }
        
        BreatheWidget().update(context, glanceId)
    }
}