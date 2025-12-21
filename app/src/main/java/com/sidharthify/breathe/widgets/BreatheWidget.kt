package com.sidharthify.breathe.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.sidharthify.breathe.MainActivity
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_AQI
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PROVIDER
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_STATUS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_ZONE_NAME
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_TOTAL_PINS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PM25
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PM10
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_NO2
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_SO2
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_CO
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_O3

class BreatheWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(40.dp, 40.dp),
            DpSize(100.dp, 100.dp),
            DpSize(200.dp, 140.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
        val size = LocalSize.current
        val status = prefs[PREF_STATUS] ?: "Loading"
        val isLoading = status == "Loading"

        val bgColor = GlanceTheme.colors.surface
        val onSurface = GlanceTheme.colors.onSurface
        val onSurfaceVariant = GlanceTheme.colors.onSurfaceVariant
        val outline = GlanceTheme.colors.outline
        val surfaceVariant = GlanceTheme.colors.surfaceVariant

        if (status == "Empty") {
            EmptyStateWidget(bgColor, onSurface)
            return
        }

        val zoneName = prefs[PREF_ZONE_NAME] ?: "..."
        val aqi = prefs[PREF_AQI] ?: 0
        val rawProvider = prefs[PREF_PROVIDER] ?: ""
        val providerText = rawProvider.replace("Source: ", "").replace("-", " ")
        
        val totalPins = prefs[PREF_TOTAL_PINS] ?: 1
        val aqiColor = ColorProvider(getAqiColor(aqi))

        val isTiny = size.width < 90.dp || size.height < 90.dp
        val isNarrow = size.width < 160.dp
        val showPollutants = size.height >= 130.dp && !isNarrow

        val bigTextSize = when {
            isNarrow -> 42.sp
            else -> 56.sp
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp)
        ) {
            if (isTiny) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$aqi", style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = aqiColor))
                    Text("AQI", style = TextStyle(fontSize = 11.sp, color = onSurfaceVariant))
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = zoneName,
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface),
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WidgetIconButton(
                                symbol = if (isLoading) "..." else "↻",
                                contentColor = if (isLoading) outline else onSurface,
                                containerColor = surfaceVariant,
                                actionClass = RefreshCallback::class.java
                            )

                            if (totalPins > 1) {
                                Spacer(GlanceModifier.width(8.dp))
                                WidgetIconButton("◄", onSurface, surfaceVariant, PrevLocationAction::class.java)
                                Spacer(GlanceModifier.width(4.dp))
                                WidgetIconButton("►", onSurface, surfaceVariant, NextLocationAction::class.java)
                            }
                        }
                    }

                    Spacer(GlanceModifier.defaultWeight())

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$aqi",
                            style = TextStyle(fontSize = bigTextSize, fontWeight = FontWeight.Medium, color = aqiColor),
                            modifier = GlanceModifier.padding(bottom = (-6).dp)
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Column(modifier = GlanceModifier.padding(bottom = 6.dp)) {
                            Text("NAQI", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = aqiColor))
                        }
                    }

                    Spacer(GlanceModifier.defaultWeight())

                    if (showPollutants) {
                        Spacer(GlanceModifier.height(12.dp))
                        PollutantGrid(prefs, onSurface, onSurfaceVariant)
                    }
                    if (providerText.isNotEmpty()) {
                        Spacer(GlanceModifier.height(8.dp))
                        Text(
                            text = providerText,
                            style = TextStyle(fontSize = 10.sp, color = outline, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetIconButton(
        symbol: String, 
        contentColor: ColorProvider,
        containerColor: ColorProvider,
        actionClass: Class<out ActionCallback>
    ) {
        Box(
            modifier = GlanceModifier
                .size(32.dp) 
                .background(containerColor)
                .cornerRadius(12.dp)
                .clickable(actionRunCallback(actionClass)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = TextStyle(
                    color = contentColor, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun PollutantGrid(
        prefs: androidx.datastore.preferences.core.Preferences,
        textColor: ColorProvider,
        labelColor: ColorProvider
    ) {
        val pm25 = prefs[PREF_PM25] ?: -1.0
        val pm10 = prefs[PREF_PM10] ?: -1.0
        val no2 = prefs[PREF_NO2] ?: -1.0
        val so2 = prefs[PREF_SO2] ?: -1.0
        val co = prefs[PREF_CO] ?: -1.0
        val o3 = prefs[PREF_O3] ?: -1.0
        
        fun fmt(d: Double) = if (d < 0) "--" else d.toInt().toString()

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.outline)) {}
            Spacer(GlanceModifier.height(8.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                PollutantItem("PM2.5", fmt(pm25), textColor, labelColor, GlanceModifier.defaultWeight())
                PollutantItem("PM10", fmt(pm10), textColor, labelColor, GlanceModifier.defaultWeight())
                PollutantItem("NO₂", fmt(no2), textColor, labelColor, GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                PollutantItem("SO₂", fmt(so2), textColor, labelColor, GlanceModifier.defaultWeight())
                PollutantItem("CO", fmt(co), textColor, labelColor, GlanceModifier.defaultWeight())
                PollutantItem("O₃", fmt(o3), textColor, labelColor, GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun PollutantItem(label: String, value: String, textColor: ColorProvider, labelColor: ColorProvider, modifier: GlanceModifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            Text(text = label, style = TextStyle(fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Medium))
            Text(text = value, style = TextStyle(fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Bold))
        }
    }

    @Composable
    private fun EmptyStateWidget(bgColor: ColorProvider, textColor: ColorProvider) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tap to setup", style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium))
        }
    }
}

class RefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
             prefs.toMutablePreferences().apply {
                 this[BreatheWidgetWorker.PREF_STATUS] = "Loading"
             }
        }
        BreatheWidget().update(context, glanceId)
        
        androidx.work.WorkManager.getInstance(context)
            .enqueue(
                androidx.work.OneTimeWorkRequest.Builder(BreatheWidgetWorker::class.java).build()
            )
    }
}

class BreatheWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BreatheWidget()

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
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
        androidx.work.WorkManager.getInstance(context)
            .enqueue(
                androidx.work.OneTimeWorkRequest.Builder(BreatheWidgetWorker::class.java).build()
            )
    }
}