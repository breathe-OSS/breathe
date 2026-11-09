// SPDX-License-Identifier: MIT
/*
 * BreatheWidget.kt
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
 * Copyright (C) 2026 Suvesh Moza <hellosuvesh@gmail.com>
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.sidharthify.breathe.MainActivity
import com.sidharthify.breathe.R
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_AQI
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_CO
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_USE_NAQI
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_NO2
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_O3
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PM10
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PM25
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_PROVIDER
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_SO2
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_STATUS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_TOTAL_PINS
import com.sidharthify.breathe.widgets.BreatheWidgetWorker.Companion.PREF_ZONE_NAME
import java.util.Locale

// MARK: - Palette

private val surface: ColorProvider
    @Composable get() = GlanceTheme.colors.widgetBackground

private val onSurface: ColorProvider
    @Composable get() = GlanceTheme.colors.onSurface

private val onSurfaceSub: ColorProvider
    @Composable get() = GlanceTheme.colors.onSurfaceVariant

private val chipBg: ColorProvider
    @Composable get() = GlanceTheme.colors.secondaryContainer

private val onChip: ColorProvider
    @Composable get() = GlanceTheme.colors.onSecondaryContainer

// MARK: - BreatheWidget

class BreatheWidget : GlanceAppWidget() {
    // Exact uses the host's real bounds so short 1×2 (1 row × 2 cols) cells
    // aren't forced into a square Responsive bucket.
    override val sizeMode = SizeMode.Exact

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
        val useNaqi     = prefs[PREF_USE_NAQI]  ?: false
        val totalPins   = prefs[PREF_TOTAL_PINS]  ?: 1
        val rawProvider = prefs[PREF_PROVIDER]    ?: ""

        val displayAqi    = rawAqi.coerceAtMost(500)
        val aqiStd        = if (useNaqi) "NAQI" else "US AQI"
        val category      = getAqiCategory(displayAqi, !useNaqi).label
        val providerName  = if (rawProvider.contains("airgradient", ignoreCase = true)) "AirGradient" else "Open-Meteo"

        // Breakpoints against real host size (SizeMode.Exact).
        // 1×1 = 1×1 cells, 1×2 = 1 row × 2 cols, 2×1 = 2 rows × 1 col.
        val is1x1     = size.width < 110.dp && size.height < 110.dp
        val is1x2     = size.height < 110.dp && size.width >= 110.dp && size.width < 200.dp
        val is2x1     = size.width < 110.dp && size.height >= 110.dp
        val isCompact = size.width >= 110.dp && size.width < 200.dp && size.height >= 110.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .cornerRadius(28.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            if (is1x1 || is1x2 || is2x1) {
                TinyWidget(
                    aqi = displayAqi,
                    isLoading = isLoading,
                    zoneName = zoneName,
                    totalPins = totalPins,
                    is1x1 = is1x1,
                    is2x1 = is2x1,
                )
            } else if (isCompact) {
                SmallWidget(
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

    // MARK: TinyWidget (1×1 / 1×2 / 2×1)

    @Composable
    private fun TinyWidget(
        aqi: Int,
        isLoading: Boolean,
        zoneName: String,
        totalPins: Int,
        is1x1: Boolean,
        is2x1: Boolean,
    ) {
        val size = LocalSize.current

        if (is1x1) {
            val side = minOf(size.width.value, size.height.value)
            val aqiFont = (side * 0.50f).coerceIn(24f, 40f).sp
            val zoneFont = (side * 0.18f).coerceIn(10f, 14f).sp
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (isLoading) "…" else "$aqi",
                    style = TextStyle(
                        fontSize = aqiFont,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = zoneName,
                    style = TextStyle(
                        fontSize = zoneFont,
                        fontWeight = FontWeight.Medium,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
            return
        }

        if (is2x1) {
            // 2 rows × 1 col: chevrons + refresh on top, AQI, zone below.
            val aqiFont = (size.width.value * 0.48f).coerceIn(24f, 40f).sp
            val zoneFont = (size.width.value * 0.16f).coerceIn(11f, 14f).sp

            Column(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavIconButton(
                        R.drawable.outline_chevron_left_24,
                        onSurface,
                        PrevLocationAction::class.java,
                        iconSize = 18.dp,
                        iconPadding = 2.dp,
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    NavIconButton(
                        R.drawable.outline_chevron_right_24,
                        onSurface,
                        NextLocationAction::class.java,
                        iconSize = 18.dp,
                        iconPadding = 2.dp,
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                Text(
                    text = if (isLoading) "…" else "$aqi",
                    style = TextStyle(
                        fontSize = aqiFont,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = zoneName,
                    style = TextStyle(
                        fontSize = zoneFont,
                        fontWeight = FontWeight.Medium,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )

                Spacer(GlanceModifier.defaultWeight())
            }
            return
        }

        // 1 row × 2 cols: zone + chevrons on top, AQI centered.
        val aqiFont = (size.height.value * 0.42f).coerceIn(22f, 36f).sp
        val zoneFont = (size.width.value * 0.08f).coerceIn(12f, 15f).sp

        Box(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isLoading) "…" else "$aqi",
                    style = TextStyle(
                        fontSize = aqiFont,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                    ),
                )
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = zoneName,
                    style = TextStyle(fontSize = zoneFont, fontWeight = FontWeight.Bold, color = onSurface),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                NavControls(
                    totalPins = totalPins,
                    isLoading = isLoading,
                    showRefresh = true,
                    iconSize = 16.dp,
                    iconPadding = 4.dp,
                    iconGap = 2.dp,
                )
            }
        }
    }

    // MARK: SmallWidget (2x2 / compact)

    @Composable
    private fun SmallWidget(
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
        val size = LocalSize.current
        val zoneFont = (size.width.value * 0.09f).coerceIn(11f, 13f).sp
        val aqiFont = (size.width.value * 0.10f).coerceIn(18f, 24f).sp

        val pollutants = listOf(
            "PM2.5" to pm25,
            "PM10" to pm10,
            "NO₂" to no2,
            "O₃" to o3,
            "CO" to co,
            "SO₂" to so2,
        ).filter { it.second >= 0 }.take(5)

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = zoneName,
                    style    = TextStyle(fontSize = zoneFont, fontWeight = FontWeight.Bold, color = onSurface),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                NavControls(
                    totalPins = totalPins,
                    isLoading = isLoading,
                    showRefresh = true,
                    iconSize = 18.dp,
                    iconPadding = 4.dp,
                    iconGap = 2.dp,
                )
            }

            Spacer(GlanceModifier.height(4.dp))

            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = if (isLoading) "…" else "$displayAqi",
                    style = TextStyle(fontSize = aqiFont, fontWeight = FontWeight.Medium, color = onSurface),
                )
                Spacer(GlanceModifier.defaultWeight())
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text  = category,
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurface,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    AqiStandardLabel(
                        aqiStd = aqiStd,
                        providerName = providerName,
                        stdFontSize = 8.sp,
                        providerFontSize = 7.sp,
                        pillPaddingH = 6.dp,
                        pillPaddingV = 1.dp,
                    )
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // Absorb leftover height so chips fill the bottom instead of leaving a dead gap.
            PollutantChipGrid(
                items = pollutants,
                labelFontSize = 9.sp,
                valueFontSize = 12.sp,
                verticalPadding = 3.dp,
                gap = 4.dp,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
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
        val size = LocalSize.current
        val zoneFont = 20.sp
        val aqiFont = (size.width.value * 0.15f).coerceIn(28f, 40f).sp
        val categoryFont = 11.sp
        val chipLabelFont = 9.sp
        val chipValueFont = 12.sp
        val chipPadV = 6.dp

        val pollutants = listOf(
            "PM2.5" to pm25,
            "PM10" to pm10,
            "NO₂" to no2,
            "O₃" to o3,
            "CO" to co,
            "SO₂" to so2,
        ).filter { it.second >= 0 }

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = zoneName,
                    style    = TextStyle(fontSize = zoneFont, fontWeight = FontWeight.Bold, color = onSurface),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                NavControls(
                    totalPins = totalPins,
                    isLoading = isLoading,
                    iconSize = 22.dp,
                    iconPadding = 5.dp,
                    iconGap = 4.dp,
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = if (isLoading) "…" else "$displayAqi",
                    style = TextStyle(fontSize = aqiFont, fontWeight = FontWeight.Medium, color = onSurface),
                )
                Spacer(GlanceModifier.defaultWeight())
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text  = category,
                        style = TextStyle(
                            fontSize   = categoryFont,
                            fontWeight = FontWeight.Medium,
                            color      = onSurface,
                            textAlign  = TextAlign.End,
                        ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(5.dp))
                    AqiStandardLabel(
                        aqiStd = aqiStd,
                        providerName = providerName,
                        stdFontSize = 11.sp,
                        providerFontSize = 9.sp,
                        pillPaddingH = 9.dp,
                        pillPaddingV = 3.dp,
                    )
                }
            }

            Spacer(GlanceModifier.defaultWeight())

            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                pollutants.forEachIndexed { index, (label, value) ->
                    PollutantChip(
                        label = label,
                        value = value,
                        modifier = GlanceModifier.defaultWeight(),
                        labelFontSize = chipLabelFont,
                        valueFontSize = chipValueFont,
                        verticalPadding = chipPadV,
                    )
                    if (index < pollutants.lastIndex) {
                        Spacer(GlanceModifier.width(6.dp))
                    }
                }
            }
        }
    }

    // MARK: PollutantChipGrid

    @Composable
    private fun PollutantChipGrid(
        items: List<Pair<String, Double>>,
        labelFontSize: TextUnit,
        valueFontSize: TextUnit,
        verticalPadding: Dp,
        gap: Dp = 4.dp,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        if (items.isEmpty()) return

        val row1 = items.take(3)
        val row2 = items.drop(3)

        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row1.forEachIndexed { index, (label, value) ->
                    PollutantChip(
                        label = label,
                        value = value,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        labelFontSize = labelFontSize,
                        valueFontSize = valueFontSize,
                        verticalPadding = verticalPadding,
                    )
                    if (index < row1.lastIndex) Spacer(GlanceModifier.width(gap))
                }
                repeat(3 - row1.size) {
                    Spacer(GlanceModifier.width(gap))
                    Spacer(GlanceModifier.defaultWeight())
                }
            }

            if (row2.isNotEmpty()) {
                Spacer(GlanceModifier.height(gap))
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row2.forEachIndexed { index, (label, value) ->
                        PollutantChip(
                            label = label,
                            value = value,
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            labelFontSize = labelFontSize,
                            valueFontSize = valueFontSize,
                            verticalPadding = verticalPadding,
                        )
                        if (index < row2.lastIndex) Spacer(GlanceModifier.width(gap))
                    }
                }
            }
        }
    }

    // MARK: AqiStandardLabel

    @Composable
    private fun AqiStandardLabel(
        aqiStd: String,
        providerName: String,
        stdFontSize: TextUnit,
        providerFontSize: TextUnit,
        pillPaddingH: Dp = 10.dp,
        pillPaddingV: Dp = 3.dp,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = GlanceModifier
                    .background(chipBg)
                    .cornerRadius(50.dp)
                    .padding(horizontal = pillPaddingH, vertical = pillPaddingV),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = aqiStd,
                    style = TextStyle(
                        fontSize   = stdFontSize,
                        fontWeight = FontWeight.Medium,
                        color      = onChip,
                        textAlign  = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
            if (providerName.isNotBlank()) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text  = providerName,
                    style = TextStyle(
                        fontSize   = providerFontSize,
                        fontWeight = FontWeight.Medium,
                        color      = onSurfaceSub,
                        textAlign  = TextAlign.End,
                    ),
                    maxLines = 1,
                )
            }
        }
    }

    // MARK: PollutantChip

    @Composable
    private fun PollutantChip(
        label: String,
        value: Double,
        modifier: GlanceModifier = GlanceModifier,
        labelFontSize: TextUnit = 11.sp,
        valueFontSize: TextUnit = 14.sp,
        verticalPadding: Dp = 6.dp,
    ) {
        if (value < 0) return
        Column(
            modifier = modifier
                .background(chipBg)
                .cornerRadius(8.dp)
                .padding(vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = label,
                style = TextStyle(
                    fontSize = labelFontSize,
                    color = onChip,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text  = formatVal(value),
                style = TextStyle(
                    fontSize = valueFontSize,
                    color = onChip,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }

    // MARK: NavControls

    @Composable
    private fun NavControls(
        totalPins: Int,
        isLoading: Boolean,
        showRefresh: Boolean = true,
        iconSize: Dp = 22.dp,
        iconPadding: Dp = 5.dp,
        iconGap: Dp = 4.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showRefresh) {
                NavIconButton(
                    symbol       = if (isLoading) R.drawable.outline_pending_24 else R.drawable.outline_refresh_24,
                    contentColor = if (isLoading) onSurfaceSub else onSurface,
                    actionClass  = RefreshCallback::class.java,
                    iconSize     = iconSize,
                    iconPadding  = iconPadding,
                )
            }
            if (totalPins > 1) {
                if (showRefresh) Spacer(GlanceModifier.width(iconGap))
                NavIconButton(
                    R.drawable.outline_chevron_left_24,
                    onSurface,
                    PrevLocationAction::class.java,
                    iconSize,
                    iconPadding,
                )
                Spacer(GlanceModifier.width(iconGap))
                NavIconButton(
                    R.drawable.outline_chevron_right_24,
                    onSurface,
                    NextLocationAction::class.java,
                    iconSize,
                    iconPadding,
                )
            }
        }
    }

    // MARK: NavIconButton

    @Composable
    private fun NavIconButton(
        symbol: Int,
        contentColor: ColorProvider,
        actionClass: Class<out ActionCallback>,
        iconSize: Dp = 22.dp,
        iconPadding: Dp = 5.dp,
    ) {
        Box(
            modifier = GlanceModifier
                .cornerRadius(20.dp)
                .clickable(actionRunCallback(actionClass))
                .padding(iconPadding),
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
    else if (d < 10) String.format(Locale.getDefault(), "%.1f", d)
    else d.toInt().toString()

// MARK: - Callbacks

class RefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply { this[PREF_STATUS] = "Loading" }
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
