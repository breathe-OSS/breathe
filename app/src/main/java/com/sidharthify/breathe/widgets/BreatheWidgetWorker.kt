package com.sidharthify.breathe.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.sidharthify.breathe.RetrofitClient

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

        glanceIds.forEach { glanceId ->
            updateWidgetData(context, glanceId, pinnedIds)
        }

        return Result.success()
    }

    private suspend fun updateWidgetData(context: Context, glanceId: GlanceId, pinnedIds: List<String>) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            if (pinnedIds.isEmpty()) {
                return@updateAppWidgetState prefs.toMutablePreferences().apply {
                    this[PREF_STATUS] = "Empty"
                }
            }

            var index = prefs[PREF_CURRENT_INDEX] ?: 0
            if (index >= pinnedIds.size) index = 0
            if (index < 0) index = pinnedIds.size - 1

            val currentZoneId = pinnedIds[index]

            try {
                // fetch data
                val response = RetrofitClient.api.getZoneAqi(currentZoneId)
                val concentrations = response.concentrations ?: emptyMap()
                
                // get provider name
                val providerName = if(currentZoneId.contains("srinagar", true)) "OpenAQ" else "OpenMeteo"

                prefs.toMutablePreferences().apply {
                    this[PREF_ZONE_ID] = response.zoneId
                    this[PREF_AQI] = response.nAqi
                    this[PREF_ZONE_NAME] = response.zoneName
                    this[PREF_PROVIDER] = "Source: $providerName"
                    this[PREF_STATUS] = "Success"
                    this[PREF_CURRENT_INDEX] = index
                    this[PREF_TOTAL_PINS] = pinnedIds.size

                    this[PREF_PM25] = concentrations["pm2_5"] ?: -1.0
                    this[PREF_PM10] = concentrations["pm10"] ?: -1.0
                    this[PREF_NO2] = concentrations["no2"] ?: -1.0
                    this[PREF_SO2] = concentrations["so2"] ?: -1.0
                    this[PREF_CO] = concentrations["co"] ?: -1.0
                    this[PREF_O3] = concentrations["o3"] ?: -1.0
                }

            } catch (e: Exception) {
                prefs.toMutablePreferences().apply {
                    this[PREF_STATUS] = "Error"
                }
            }
        }
        BreatheWidget().update(context, glanceId)
    }
}