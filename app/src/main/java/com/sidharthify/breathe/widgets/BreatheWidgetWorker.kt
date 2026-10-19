// SPDX-License-Identifier: MIT
/*
 * BreatheWidgetWorker.kt
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

package com.sidharthify.breathe.widgets

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sidharthify.breathe.data.RetrofitClient
import com.sidharthify.breathe.util.loadPinnedOrder

class BreatheWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters,
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

        val PREF_HISTORY_CSV = stringPreferencesKey("history_csv")
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)

        val appPrefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
        val pinnedIds = loadPinnedOrder(appPrefs)
        val isUsAqi = appPrefs.getBoolean("is_us_aqi", false)

        val targets: List<Pair<List<GlanceId>, GlanceAppWidget>> =
            listOf(
                manager.getGlanceIds(BreatheWidget::class.java) to BreatheWidget(),
                manager.getGlanceIds(BreatheTrendWidget::class.java) to BreatheTrendWidget(),
            )

        targets.forEach { (glanceIds, widget) ->
            glanceIds.forEach { glanceId ->
                try {
                    updateWidgetForId(context, glanceId, widget, pinnedIds, isUsAqi)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return Result.success()
    }

    private suspend fun updateWidgetForId(
        context: Context,
        glanceId: GlanceId,
        widget: GlanceAppWidget,
        pinnedIds: List<String>,
        isUsAqi: Boolean,
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
            widget.update(context, glanceId)
            return
        }

        if (currentIndex >= pinnedIds.size) currentIndex = 0
        val currentZoneId = pinnedIds[currentIndex]

        try {
            val response = RetrofitClient.api.getZoneAqi(currentZoneId)
            val concentrations = response.concentrations ?: emptyMap()

            val historyCsv =
                (response.history ?: emptyList())
                    .joinToString(",") { point ->
                        val value = if (!isUsAqi) (point.usAqi ?: point.aqi) else point.aqi
                        value.coerceIn(0, 500).toString()
                    }

            val source = response.source ?: ""
            val providerName =
                if (source.contains("airgradient", ignoreCase = true)) {
                    "AirGradient"
                } else {
                    "OpenMeteo"
                }

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[PREF_ZONE_ID] = response.zoneId
                    this[PREF_AQI] = response.nAqi
                    this[PREF_ZONE_NAME] = response.zoneName
                    this[PREF_PROVIDER] = "Source: $providerName"
                    this[PREF_STATUS] = "Success"
                    this[PREF_CURRENT_INDEX] = currentIndex
                    this[PREF_TOTAL_PINS] = pinnedIds.size
                    this[PREF_IS_US_AQI] = isUsAqi

                    this[PREF_PM25] = concentrations["pm2_5"] ?: -1.0
                    this[PREF_PM10] = concentrations["pm10"] ?: -1.0
                    this[PREF_NO2] = concentrations["no2"] ?: -1.0
                    this[PREF_SO2] = concentrations["so2"] ?: -1.0
                    this[PREF_CO] = concentrations["co"] ?: -1.0
                    this[PREF_O3] = concentrations["o3"] ?: -1.0

                    this[PREF_HISTORY_CSV] = historyCsv
                }
            }
        } catch (_: Exception) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply { this[PREF_STATUS] = "Error" }
            }
        }

        widget.update(context, glanceId)
    }
}
