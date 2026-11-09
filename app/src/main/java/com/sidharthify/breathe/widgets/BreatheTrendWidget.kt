// SPDX-License-Identifier: MIT
/*
 * BreatheTrendWidget.kt
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.sidharthify.breathe.MainActivity
import com.sidharthify.breathe.R
import com.sidharthify.breathe.util.calculateUsAqi
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_AQI
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_HISTORY_CSV
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_USE_NAQI
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PM25
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_STATUS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_TOTAL_PINS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_ZONE_NAME

// MARK: - Palette

private val surface: ColorProvider
    @Composable get() = GlanceTheme.colors.widgetBackground

private val onSurface: ColorProvider
    @Composable get() = GlanceTheme.colors.onSurface

private val onSurfaceSub: ColorProvider
    @Composable get() = GlanceTheme.colors.onSurfaceVariant

private val primary: ColorProvider
    @Composable get() = GlanceTheme.colors.primary

private val onPrimary: ColorProvider
    @Composable get() = GlanceTheme.colors.onPrimary

// MARK: - BreatheTrendWidget

class BreatheTrendWidget : GlanceAppWidget() {
    override val sizeMode =
        SizeMode.Responsive(
            setOf(
                DpSize(110.dp, 110.dp),
                DpSize(180.dp, 110.dp),
                DpSize(250.dp, 110.dp),
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
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val status = prefs[PREF_STATUS] ?: "Loading"
        val isLoading = status == "Loading"

        if (status == "Empty") {
            EmptyStateWidget()
            return
        }

        val zoneName = prefs[PREF_ZONE_NAME] ?: "…"
        val rawAqi = prefs[PREF_AQI] ?: 0
        val useNaqi = prefs[PREF_USE_NAQI] ?: false
        val pm25 = prefs[PREF_PM25] ?: 0.0
        val totalPins = prefs[PREF_TOTAL_PINS] ?: 1

        val displayAqi = (if (!useNaqi && pm25 > 0) calculateUsAqi(pm25) else rawAqi).coerceAtMost(500)
        val category = getAqiCategory(displayAqi, !useNaqi).label

        val values: List<Int> =
            (prefs[PREF_HISTORY_CSV] ?: "")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }

        val delta = if (values.size >= 2) values.last() - values.first() else 0

        val isCompact = LocalSize.current.width < 220.dp
        if (isCompact) {
            CompactBody(zoneName, displayAqi, category, delta, values, totalPins, isLoading)
        } else {
            TrendBody(zoneName, displayAqi, category, delta, values, totalPins, isLoading)
        }
    }

    // MARK: CompactBody

    @Composable
    private fun CompactBody(
        zoneName: String,
        displayAqi: Int,
        category: String,
        delta: Int,
        values: List<Int>,
        totalPins: Int,
        isLoading: Boolean,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .cornerRadius(28.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "24h Trend",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    if (totalPins > 1) {
                        NavIconButton(R.drawable.outline_chevron_left_24, onSurface, TrendPrevLocationAction::class.java, 24.dp)
                        NavIconButton(R.drawable.outline_chevron_right_24, onSurface, TrendNextLocationAction::class.java, 24.dp)
                    }
                }
                Column(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = zoneName,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface, textAlign = TextAlign.Center),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                    Text(
                        text = category,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = onSurfaceSub),
                        maxLines = 1,
                    )
                    Text(
                        text = "$displayAqi",
                        style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = onSurface, textAlign = TextAlign.Center),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                    TrendChange(delta)
                }
                BarChart(values, heightOverrideDp = 22f)
            }
        }
    }

    // MARK: TrendBody

    @Composable
    private fun TrendBody(
        zoneName: String,
        displayAqi: Int,
        category: String,
        delta: Int,
        values: List<Int>,
        totalPins: Int,
        isLoading: Boolean,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .cornerRadius(28.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Header( totalPins, isLoading)
                Spacer(GlanceModifier.height(4.dp))
                SummaryRow(zoneName, displayAqi, category, delta)
                Spacer(GlanceModifier.defaultWeight())
                BarChart(values)
            }
        }
    }

    // MARK: Header

    @Composable
    private fun Header(totalPins: Int, isLoading: Boolean) {
        val compact = LocalSize.current.width < 220.dp
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                Box(
                    modifier = GlanceModifier
                        .size(26.dp)
                        .background(primary)
                        .cornerRadius(13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(onPrimary),
                        modifier = GlanceModifier.size(48.dp),
                    )
                }
                Spacer(GlanceModifier.width(10.dp))
            }
            Text(
                text = "24h Trend",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            NavControls(totalPins, isLoading, compact)
        }
    }

    // MARK: SummaryRow

    @Composable
    private fun SummaryRow(zoneName: String, displayAqi: Int, category: String, delta: Int) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = zoneName,
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface),
                    maxLines = 1,
                )
                Text(
                    text = category,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = onSurfaceSub),
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$displayAqi",
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        textAlign = TextAlign.End,
                    ),
                )
                TrendChange(delta)
            }
        }
    }

    // MARK: TrendChange

    @Composable
    private fun TrendChange(delta: Int) {
        if (delta == 0) {
            Text(
                text = "Steady over 24h",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = onSurfaceSub),
                maxLines = 1,
            )
            return
        }

        val icon = if (delta > 0) R.drawable.outline_trending_up_24 else R.drawable.outline_trending_down_24
        val label = if (delta > 0) "+$delta" else "$delta"

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(onSurfaceSub),
                modifier = GlanceModifier.size(18.dp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = label,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = onSurfaceSub),
                maxLines = 1,
            )
        }
    }

    // MARK: BarChart

    // Glance caps direct children per container (~10), so the 24 bars are drawn
    // into one Bitmap and shown as a single Image.
    @Composable
    private fun BarChart(values: List<Int>, heightOverrideDp: Float? = null) {
        if (values.isEmpty()) {
            Text(
                text = "No trend data yet",
                style = TextStyle(fontSize = 12.sp, color = onSurfaceSub),
            )
            return
        }

        val context = androidx.glance.LocalContext.current
        val size = LocalSize.current
        val density = context.resources.displayMetrics.density

        val chartMaxDp = heightOverrideDp ?: (size.height.value - 118f).coerceIn(28f, 120f)
        val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
        val heightPx = (chartMaxDp * density).toInt().coerceAtLeast(1)

        val bitmap = renderTrendBitmap(values, widthPx, heightPx, density)

        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "24 hour AQI trend",
            colorFilter = ColorFilter.tint(primary),
            contentScale = androidx.glance.layout.ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxWidth().height(chartMaxDp.dp),
        )
    }

    // MARK: NavControls

    @Composable
    private fun NavControls(totalPins: Int, isLoading: Boolean, compact: Boolean = false) {
        val gap = if (compact) 0.dp else 4.dp
        val iconSize = if (compact) 18.dp else 24.dp
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavIconButton(
                symbol = if (isLoading) R.drawable.outline_pending_24 else R.drawable.outline_refresh_24,
                contentColor = onSurface,
                actionClass = TrendRefreshCallback::class.java,
                iconSize = iconSize,
            )
            if (totalPins > 1) {
                Spacer(GlanceModifier.width(gap))
                NavIconButton(R.drawable.outline_chevron_left_24, onSurface, TrendPrevLocationAction::class.java, iconSize)
                Spacer(GlanceModifier.width(gap))
                NavIconButton(R.drawable.outline_chevron_right_24, onSurface, TrendNextLocationAction::class.java, iconSize)
            }
        }
    }

    // MARK: NavIconButton

    @Composable
    private fun NavIconButton(
        symbol: Int,
        contentColor: ColorProvider,
        actionClass: Class<out ActionCallback>,
        iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    ) {
        Box(
            // cornerRadius clips the ripple to a circle; padding enlarges the touch
            // target and gives the ripple margin so it isn't cramped on the glyph.
            modifier = GlanceModifier
                .cornerRadius(20.dp)
                .clickable(actionRunCallback(actionClass))
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(symbol),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = GlanceModifier.size(iconSize),
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
                    text = "No zones pinned",
                    style = TextStyle(color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Open Breathe to pin locations",
                    style = TextStyle(color = onSurfaceSub, fontSize = 11.sp),
                )
            }
        }
    }
}

// MARK: - Helpers

// Bars are drawn white so the Image's ColorFilter tint can follow light/dark mode.
private fun renderTrendBitmap(
    values: List<Int>,
    widthPx: Int,
    heightPx: Int,
    density: Float,
): android.graphics.Bitmap {
    val bitmap = createBitmap(widthPx, heightPx)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE

    val n = values.size
    val gap = 2f * density
    val barWidth = ((widthPx - gap * (n - 1)) / n).coerceAtLeast(1f)
    val radius = (barWidth / 2f)
    val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    val minBarPx = 3f * density

    values.forEachIndexed { index, value ->
        val ratio = value.toFloat() / maxValue
        val barHeight = (minBarPx + ratio * (heightPx - minBarPx)).coerceIn(minBarPx, heightPx.toFloat())
        val left = index * (barWidth + gap)
        val top = heightPx - barHeight
        canvas.drawRoundRect(left, top, left + barWidth, heightPx.toFloat(), radius, radius, paint)
    }

    return bitmap
}

// MARK: - Callbacks

class TrendRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply { this[PREF_STATUS] = "Loading" }
        }
        BreatheTrendWidget().update(context, glanceId)

        WorkManager
            .getInstance(context)
            .enqueue(OneTimeWorkRequest.from(BreatheWidgetWorker::class.java))
    }
}

class TrendNextLocationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        cycleTrendLocation(context, glanceId, 1)
    }
}

class TrendPrevLocationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        cycleTrendLocation(context, glanceId, -1)
    }
}

private suspend fun cycleTrendLocation(context: Context, glanceId: GlanceId, direction: Int) {
    val appPrefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
    val size = (appPrefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()).size

    if (size <= 1) return

    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        val currentIndex = prefs[BreatheWidgetWorker.PREF_CURRENT_INDEX] ?: 0

        var newIndex = currentIndex + direction
        if (newIndex >= size) newIndex = 0
        if (newIndex < 0) newIndex = size - 1

        prefs.toMutablePreferences().apply {
            this[BreatheWidgetWorker.PREF_CURRENT_INDEX] = newIndex
            this[PREF_STATUS] = "Loading"
        }
    }

    BreatheTrendWidget().update(context, glanceId)

    WorkManager.getInstance(context).enqueue(
        OneTimeWorkRequest.from(BreatheWidgetWorker::class.java),
    )
}

// MARK: - Receiver

class BreatheTrendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BreatheTrendWidget()

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
        WorkManager
            .getInstance(context)
            .enqueue(OneTimeWorkRequest.from(BreatheWidgetWorker::class.java))
    }
}
