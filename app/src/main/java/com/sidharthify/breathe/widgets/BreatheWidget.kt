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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.sidharthify.breathe.MainActivity
import com.sidharthify.breathe.getAqiColor
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

        if (status == "Empty") {
            EmptyStateWidget()
            return
        }

        // data
        val zoneName = prefs[PREF_ZONE_NAME] ?: "..."
        val aqi = prefs[PREF_AQI] ?: 0
        val provider = prefs[PREF_PROVIDER] ?: ""
        val totalPins = prefs[PREF_TOTAL_PINS] ?: 1

        val aqiColor = getAqiColor(aqi)
        
        // colors
        val baseBg = Color(0xFF1E1F24)
        val baseText = Color.White
        val baseSecondary = Color(0xFFC4C7D0)

        val bgColor = ColorProvider(baseBg)
        val textColor = ColorProvider(baseText)
        val secondaryTextColor = ColorProvider(baseSecondary)
        val attributionColor = ColorProvider(baseSecondary.copy(alpha = 0.7f))

        val isTiny = size.width < 90.dp || size.height < 90.dp
        val isNarrow = size.width < 160.dp
        val showPollutants = size.height >= 140.dp

        val bigTextSize = when {
            isNarrow -> 36.sp
            showPollutants -> 48.sp
            else -> 60.sp
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {

            if (isTiny) {
                // 1x1 tiny
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$aqi",
                        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ColorProvider(aqiColor))
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text("AQI", style = TextStyle(fontSize = 10.sp, color = secondaryTextColor))
                }
            } else {
                // regular / wide
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    
                    // header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zoneName,
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (totalPins > 1) {
                            Row {
                                Text(
                                    text = "◄",
                                    style = TextStyle(color = secondaryTextColor, fontSize = 16.sp),
                                    modifier = GlanceModifier.padding(horizontal = 6.dp).clickable(androidx.glance.appwidget.action.actionRunCallback<PrevLocationAction>())
                                )
                                Text(
                                    text = "►",
                                    style = TextStyle(color = secondaryTextColor, fontSize = 16.sp),
                                    modifier = GlanceModifier.padding(horizontal = 6.dp).clickable(androidx.glance.appwidget.action.actionRunCallback<NextLocationAction>())
                                )
                            }
                        }
                    }

                    Spacer(GlanceModifier.height(4.dp))

                    // aqi number row
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$aqi",
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = bigTextSize,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(aqiColor)
                            )
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            text = "NAQI",
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(aqiColor)
                            ),
                            modifier = GlanceModifier.padding(bottom = if(showPollutants) 10.dp else 12.dp)
                        )
                    }

                    // pollutant grid
                    if (showPollutants) {
                        Spacer(GlanceModifier.height(12.dp))
                        PollutantGrid(prefs, textColor, secondaryTextColor)
                    }
                }
            }

            // attributions
            if (provider.isNotEmpty() && !isTiny) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(bottom = 6.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = provider.replace("Source: ", ""),
                        style = TextStyle(
                            fontSize = 9.sp, 
                            color = attributionColor,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
            
            // loading overlay
            if (status == "Loading") {
                Box(modifier = GlanceModifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                    Text("...", style = TextStyle(color = secondaryTextColor, fontSize = 20.sp))
                }
            }
        }
    }

    @Composable
    private fun PollutantGrid(
        prefs: androidx.datastore.preferences.core.Preferences,
        textColor: ColorProvider,
        subColor: ColorProvider
    ) {
        val pm25 = prefs[PREF_PM25] ?: -1.0
        val pm10 = prefs[PREF_PM10] ?: -1.0
        val no2 = prefs[PREF_NO2] ?: -1.0
        val so2 = prefs[PREF_SO2] ?: -1.0
        val co = prefs[PREF_CO] ?: -1.0
        val o3 = prefs[PREF_O3] ?: -1.0

        // helper to format values
        fun fmt(valDouble: Double): String = if (valDouble < 0) "-" else valDouble.toString()

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                PollutantItem("PM2.5", fmt(pm25), textColor, subColor, GlanceModifier.defaultWeight())
                PollutantItem("PM10", fmt(pm10), textColor, subColor, GlanceModifier.defaultWeight())
                PollutantItem("NO₂", fmt(no2), textColor, subColor, GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                PollutantItem("SO₂", fmt(so2), textColor, subColor, GlanceModifier.defaultWeight())
                PollutantItem("CO", fmt(co), textColor, subColor, GlanceModifier.defaultWeight())
                PollutantItem("O₃", fmt(o3), textColor, subColor, GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun PollutantItem(
        label: String, 
        value: String, 
        textColor: ColorProvider, 
        subColor: ColorProvider,
        modifier: GlanceModifier
    ) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                style = TextStyle(fontSize = 10.sp, color = subColor, fontWeight = FontWeight.Bold)
            )
            Text(
                text = value,
                style = TextStyle(fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Medium)
            )
        }
    }

    @Composable
    private fun EmptyStateWidget() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1E1F24)))
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tap to setup", style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp))
        }
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
                androidx.work.OneTimeWorkRequest.Builder(BreatheWidgetWorker::class.java)
                    .build()
            )
    }
}