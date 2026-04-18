// SPDX-License-Identifier: MIT
/*
 * Models.kt - Data classes representing API responses and app state
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

package com.sidharthify.breathe.data

import com.google.gson.annotations.SerializedName

data class ZonesResponse(
    val zones: List<Zone>,
)

data class Zone(
    val id: String,
    val name: String,
    val provider: String?,
    val lat: Double?,
    val lon: Double?,
)

data class Trends(
    @SerializedName("change_1h") val change1h: Int?,
    @SerializedName("change_24h") val change24h: Int?,
)

data class AqiResponse(
    @SerializedName("zone_id") val zoneId: String,
    @SerializedName("zone_name") val zoneName: String,
    @SerializedName("aqi") val nAqi: Int,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("main_pollutant") val mainPollutant: String,
    @SerializedName("us_main_pollutant") val usMainPollutant: String?,
    @SerializedName("aqi_breakdown") val aqiBreakdown: Map<String, Int>?,
    @SerializedName("concentrations_us_units") val concentrations: Map<String, Double>?,
    @SerializedName("timestamp_unix") val timestampUnix: Double?,
    @SerializedName("last_update") val lastUpdateStr: String?,
    @SerializedName("history") val history: List<HistoryPoint>? = emptyList(),
    @SerializedName("trends") val trends: Trends? = null,
    @SerializedName("warning") val warning: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("nodes") val nodes: Map<String, NodeReading>? = null,
)

data class HistoryPoint(
    @SerializedName("ts") val ts: Long,
    @SerializedName("aqi") val aqi: Int,
    @SerializedName("us_aqi") val usAqi: Int?,
)

data class NodeHistoryPoint(
    @SerializedName("ts") val ts: Long,
    @SerializedName("aqi") val aqi: Int,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
)

data class NodeReading(
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("temp") val temp: Double?,
    @SerializedName("humidity") val humidity: Double?,
    @SerializedName("aqi") val aqi: Int?,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("history") val history: List<NodeHistoryPoint>? = emptyList(),
)

data class AppState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val zones: List<Zone> = emptyList(),
    val allAqiData: List<AqiResponse> = emptyList(),
    val pinnedZones: List<AqiResponse> = emptyList(),
    val pinnedIds: Set<String> = emptySet(),
)
