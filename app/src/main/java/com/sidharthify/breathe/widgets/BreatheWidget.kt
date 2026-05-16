// SPDX-License-Identifier: MIT
/*
 * BreatheWidget.kt
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
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sidharthify.breathe.MainActivity
import com.sidharthify.breathe.R
import com.sidharthify.breathe.util.calculateUsAqi
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_AQI
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_CO
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_IS_US_AQI
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_NO2
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_O3
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PM10
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PM25
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PROVIDER
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_SO2
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_STATUS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_TOTAL_PINS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_ZONE_NAME

// MARK: - Palette

private val surface      = ColorProvider(Color(0xFF1A1C22))
private val onSurface    = ColorProvider(Color(0xFFE3E5EC))
private val onSurfaceSub = ColorProvider(Color(0xFF9AA0B4))
private val chipBg       = ColorProvider(Color(0x22FFFFFF))

// MARK: - BreatheWidget

class BreatheWidget : GlanceAppWidget() {
    override val sizeMode =
        SizeMode.Responsive(
            setOf(
                DpSize(40.dp,  40.dp),
                DpSize(100.dp, 100.dp),
                DpSize(200.dp, 140.dp),
            ),
        )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { WidgetContent() }
        }
    }

    // MARK: Root

    @Composable
    private fun WidgetContent() {
        val prefs     = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
        val size      = LocalSize.current
        val status    = prefs[PREF_STATUS] ?: "Loading"
        val isLoading = status == "Loading"

        if (status == "Empty") {
            EmptyStateWidget()
            return
        }

        val zoneName    = prefs[PREF_ZONE_NAME]  ?: "…"
        val rawAqi      = prefs[PREF_AQI]        ?: 0
        val isUsAqi     = prefs[PREF_IS_US_AQI]  ?: false
        val pm25        = prefs[PREF_PM25]        ?: 0.0
        val totalPins   = prefs[PREF_TOTAL_PINS]  ?: 1
        val rawProvider = prefs[PREF_PROVIDER]    ?: ""

        // is_us_aqi: false = US AQI (default), true = Indian NAQI
        val isNaqi        = isUsAqi
        val displayAqi    = (if (!isNaqi && pm25 > 0) calculateUsAqi(pm25) else rawAqi).coerceAtMost(500)
        val aqiStd        = if (isNaqi) "NAQI" else "US AQI"
        val category      = getAqiCategory(displayAqi, !isNaqi).label
        val providerName  = if (rawProvider.contains("airgradient", ignoreCase = true)) "airgradient" else "openmeteo"

        val isTiny   = size.width < 90.dp || size.height < 90.dp
        val isNarrow = size.width < 160.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .cornerRadius(28.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            if (isTiny) {
                TinyWidget(displayAqi, isLoading)
            } else if (isNarrow) {
                SmallWidget(
                    zoneName     = zoneName,
                    displayAqi   = displayAqi,
                    aqiStd       = aqiStd,
                    providerName = providerName,
                    pm25         = prefs[PREF_PM25] ?: -1.0,
                    pm10         = prefs[PREF_PM10] ?: -1.0,
                    totalPins    = totalPins,
                    isLoading    = isLoading,
                )
            } else {
                MediumWidget(
                    zoneName     = zoneName,
                    displayAqi   = displayAqi,
                    aqiStd       = aqiStd,
                    category     = category,
                    providerName = providerName,
                    pm25         = prefs[PREF_PM25] ?: -1.0,
                    pm10         = prefs[PREF_PM10] ?: -1.0,
                    no2          = prefs[PREF_NO2]  ?: -1.0,
                    so2          = prefs[PREF_SO2]  ?: -1.0,
                    co           = prefs[PREF_CO]   ?: -1.0,
                    o3           = prefs[PREF_O3]   ?: -1.0,
                    totalPins    = totalPins,
                    isLoading    = isLoading,
                )
            }
        }
    }

    // MARK: TinyWidget

    @Composable
    private fun TinyWidget(aqi: Int, isLoading: Boolean) {
        Column(
            modifier            = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = if (isLoading) "…" else "$aqi",
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onSurface),
            )
            Text(
                text  = "AQI",
                style = TextStyle(fontSize = 10.sp, color = onSurfaceSub, fontWeight = FontWeight.Medium),
            )
        }
    }

    // MARK: SmallWidget

    @Composable
    private fun SmallWidget(
        zoneName: String,
        displayAqi: Int,
        aqiStd: String,
        providerName: String,
        pm25: Double,
        pm10: Double,
        totalPins: Int,
        isLoading: Boolean,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = zoneName,
                    style    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = onSurface),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                NavControls(totalPins, isLoading)
                Spacer(GlanceModifier.width(6.dp))
                ProviderBadge(providerName)
            }

            Spacer(GlanceModifier.defaultWeight())

            Text(
                text  = "$displayAqi",
                style = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Medium, color = onSurface),
            )

            Spacer(GlanceModifier.defaultWeight())

            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PollutantChip("PM2.5", pm25)
                    Spacer(GlanceModifier.width(10.dp))
                    PollutantChip("PM10",  pm10)
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text  = aqiStd,
                    style = TextStyle(
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color      = onSurfaceSub,
                        textAlign  = TextAlign.End,
                    ),
                )
            }
        }
    }

    // MARK: MediumWidget

    @Composable
    private fun MediumWidget(
        zoneName: String,
        displayAqi: Int,
        aqiStd: String,
        category: String,
        providerName: String,
        pm25: Double,
        pm10: Double,
        no2: Double,
        so2: Double,
        co: Double,
        o3: Double,
        totalPins: Int,
        isLoading: Boolean,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = zoneName,
                    style    = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                NavControls(totalPins, isLoading)
                Spacer(GlanceModifier.width(8.dp))
                ProviderBadge(providerName)
            }

            Spacer(GlanceModifier.defaultWeight())

            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "$displayAqi",
                    style = TextStyle(fontSize = 60.sp, fontWeight = FontWeight.Medium, color = onSurface),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text  = category,
                    style = TextStyle(
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color      = onSurface,
                        textAlign  = TextAlign.End,
                    ),
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PollutantChip("PM2.5", pm25)
                    Spacer(GlanceModifier.width(10.dp))
                    PollutantChip("PM10",  pm10)
                    Spacer(GlanceModifier.width(10.dp))
                    PollutantChip("CO",    co)
                    Spacer(GlanceModifier.width(10.dp))
                    PollutantChip("SO₂",   so2)
                    Spacer(GlanceModifier.width(10.dp))
                    PollutantChip("NO₂",   no2)
                    Spacer(GlanceModifier.width(10.dp))
                    PollutantChip("O₃",    o3)
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text  = aqiStd,
                    style = TextStyle(
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = onSurfaceSub,
                        textAlign  = TextAlign.End,
                    ),
                )
            }
        }
    }

    // MARK: PollutantChip

    @Composable
    private fun PollutantChip(label: String, value: Double) {
        if (value >= 0) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text  = label,
                    style = TextStyle(fontSize = 9.sp, color = onSurfaceSub, fontWeight = FontWeight.Medium),
                )
                Text(
                    text  = formatVal(value),
                    style = TextStyle(fontSize = 12.sp, color = onSurface, fontWeight = FontWeight.Bold),
                )
            }
        }
    }

    // MARK: NavControls

    @Composable
    private fun NavControls(totalPins: Int, isLoading: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavIconButton(
                symbol       = if (isLoading) R.drawable.outline_pending_24 else R.drawable.outline_refresh_24,
                contentColor = if (isLoading) onSurfaceSub else onSurface,
                actionClass  = RefreshCallback::class.java,
            )
            if (totalPins > 1) {
                Spacer(GlanceModifier.width(4.dp))
                NavIconButton(R.drawable.outline_arrow_left_24,  onSurface, PrevLocationAction::class.java)
                Spacer(GlanceModifier.width(4.dp))
                NavIconButton(R.drawable.outline_arrow_right_24, onSurface, NextLocationAction::class.java)
            }
        }
    }

    // MARK: NavIconButton

    @Composable
    private fun NavIconButton(
        symbol: Int,
        contentColor: ColorProvider,
        actionClass: Class<out ActionCallback>,
    ) {
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(chipBg)
                .cornerRadius(14.dp)
                .clickable(actionRunCallback(actionClass)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider           = ImageProvider(symbol),
                contentDescription = null,
                colorFilter        = ColorFilter.tint(contentColor),
                modifier           = GlanceModifier.size(16.dp),
            )
        }
    }

    // MARK: ProviderBadge

    @Composable
    private fun ProviderBadge(name: String) {
        Box(
            modifier = GlanceModifier
                .background(chipBg)
                .cornerRadius(12.dp)
                .padding(horizontal = 7.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = name,
                style = TextStyle(
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color      = onSurfaceSub,
                ),
            )
        }
    }

    // MARK: EmptyStateWidget

    @Composable
    private fun EmptyStateWidget() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .cornerRadius(28.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = "No zones pinned",
                    style = TextStyle(color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text  = "Open Breathe to pin locations",
                    style = TextStyle(color = onSurfaceSub, fontSize = 11.sp),
                )
            }
        }
    }
}

// MARK: - Helpers

private fun formatVal(d: Double): String =
    if (d < 0) "--"
    else if (d < 10) String.format("%.1f", d)
    else d.toInt().toString()

// MARK: - Callbacks

class RefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply { this[BreatheWidgetWorker.PREF_STATUS] = "Loading" }
        }
        BreatheWidget().update(context, glanceId)

        androidx.work.WorkManager
            .getInstance(context)
            .enqueue(
                androidx.work.OneTimeWorkRequest
                    .Builder(BreatheWidgetWorker::class.java)
                    .build(),
            )
    }
}

class BreatheWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BreatheWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        triggerWorker(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        triggerWorker(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.sidharthify.breathe.FORCE_WIDGET_UPDATE") {
            triggerWorker(context)
        }
    }

    private fun triggerWorker(context: Context) {
        androidx.work.WorkManager
            .getInstance(context)
            .enqueue(
                androidx.work.OneTimeWorkRequest
                    .Builder(BreatheWidgetWorker::class.java)
                    .build(),
            )
    }
}
