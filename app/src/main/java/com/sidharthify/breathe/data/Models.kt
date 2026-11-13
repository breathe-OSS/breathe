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
import com.sidharthify.breathe.util.HistoryPeriod
import com.sidharthify.breathe.util.HistoryPeriodMode

data class SensorInfoResponse(
    val sensors: List<SensorInfo>
)

// zone here is the zone name ('Srinagar'), not its id, and name matches the
// key the sensor has in AqiResponse.nodes
data class SensorInfo(
    val name: String,
    val zone: String,
    @SerializedName("location_id") val locationId: Int,
    val provider: String,
    val model: String,
    @SerializedName("is_kit") val isKit: Boolean,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("installation_date") val installationDate: String
)

data class ZonesResponse(
    val zones: List<Zone>,
)

data class ZonesAqiResponse(
    val zones: List<AqiResponse>,
)

data class Zone(
    val id: String,
    val name: String,
    val provider: String?,
    val lat: Double?,
    val lon: Double?,
    val kind: String? = null,
    val district: String? = null,
    val division: String? = null,
    @SerializedName("legacy_zone") val legacyZone: String? = null,
    @SerializedName("zone_type") val zoneType: String? = null,
)

data class Trends(
    @SerializedName("change_1h") val change1h: Int?,
    @SerializedName("change_24h") val change24h: Int?,
)

// one zone right now. aqi is the indian NAQI and us_aqi the US EPA number,
// use displayAqi() instead of picking between them by hand
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
    @SerializedName("averages_24h") val averages24h: Map<String, Double>? = null,
    @SerializedName("trends") val trends: Trends? = null,
    @SerializedName("warning") val warning: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("nodes") val nodes: Map<String, NodeReading>? = null,
    @SerializedName("weather") val weather: WeatherInfo? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("division") val division: String? = null,
    @SerializedName("location_id") val locationId: Int? = null,
    @SerializedName("sensor_status") val sensorStatus: String? = null,
    @SerializedName("readings") val readings: SensorReadings? = null,
    @SerializedName("raw") val raw: RawReadings? = null,
    @SerializedName("raw_available") val rawAvailable: Boolean? = null,
    @SerializedName("averages_24h_raw") val averages24hRaw: Map<String, Double>? = null,
    @Transient val showingRaw: Boolean = false,
)

data class SensorReadings(
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("temp") val temp: Double?,
    @SerializedName("humidity") val humidity: Double?,
)

data class RawReadings(
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("temp") val temp: Double?,
    @SerializedName("humidity") val humidity: Double?,
    @SerializedName("aqi") val nAqi: Int?,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("main_pollutant") val mainPollutant: String?,
    @SerializedName("us_main_pollutant") val usMainPollutant: String?,
    @SerializedName("concentrations_us_units") val concentrations: Map<String, Double>?,
)

data class WeatherInfo(
    val condition: String = "",
    @SerializedName("weather_code") val weatherCode: Int? = null,
    val precipitation: Double? = null,
    val season: String = "",
    val text: String = "",
)

data class WeatherHistoryPoint(
    val ts: Long,
    val condition: String = "",
    val precipitation: Double? = null,
)

data class WeatherHistory(
    val interval: Long = 3600L,
    val anchor: Long = 0L,
    val points: List<WeatherHistoryPoint> = emptyList(),
    @SerializedName("zone_id") val zoneId: String? = null,
)

data class HistoryPoint(
    @SerializedName("ts") val ts: Long,
    @SerializedName("aqi") val aqi: Int,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("pm2_5") val pm25: Double? = null,
    @SerializedName("pm10") val pm10: Double? = null,
    @SerializedName("aqi_raw") val aqiRaw: Int? = null,
    @SerializedName("us_aqi_raw") val usAqiRaw: Int? = null,
    @SerializedName("pm2_5_raw") val pm25Raw: Double? = null,
    @SerializedName("pm10_raw") val pm10Raw: Double? = null,
)

data class NodeHistoryPoint(
    @SerializedName("ts") val ts: Long,
    @SerializedName("aqi") val aqi: Int,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
)

// a single ground sensor in a zone. pm2_5 comes back null when it's offline
data class NodeReading(
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("temp") val temp: Double?,
    @SerializedName("humidity") val humidity: Double?,
    @SerializedName("aqi") val aqi: Int?,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("history") val history: List<NodeHistoryPoint>? = emptyList(),
)

data class HistoricalDataPoint(
    @SerializedName("zone_id") val zoneId: String?,
    @SerializedName("ts") val ts: Long,
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("temp") val temp: Double? = null,
    @SerializedName("humidity") val humidity: Double? = null,
    @SerializedName("us_aqi") val usAqi: Double? = null,
    @SerializedName("pm2_5_raw") val pm25Raw: Double? = null,
    @SerializedName("pm10_raw") val pm10Raw: Double? = null,
    @SerializedName("temp_raw") val tempRaw: Double? = null,
    @SerializedName("humidity_raw") val humidityRaw: Double? = null,
    @SerializedName("us_aqi_raw") val usAqiRaw: Double? = null,
) {
    fun value(key: String): Double? =
        when (key) {
            "pm2_5" -> pm25
            "pm10" -> pm10
            "temp" -> temp
            "humidity" -> humidity
            "us_aqi" -> usAqi
            "pm2_5_raw" -> pm25Raw
            "pm10_raw" -> pm10Raw
            "temp_raw" -> tempRaw
            "humidity_raw" -> humidityRaw
            "us_aqi_raw" -> usAqiRaw
            else -> null
        }
}

data class HistoricalStats(
    @SerializedName("max_pm2_5") val maxPm25: Double?,
    @SerializedName("min_pm2_5") val minPm25: Double?,
    @SerializedName("avg_pm2_5") val avgPm25: Double?,
    @SerializedName("max_pm10") val maxPm10: Double?,
    @SerializedName("min_pm10") val minPm10: Double?,
    @SerializedName("avg_pm10") val avgPm10: Double?,
)

data class HistoricalDataResponse(
    @SerializedName("data") val data: List<HistoricalDataPoint>,
    @SerializedName("stats") val stats: HistoricalStats?,
)

enum class HistoryMetric(
    val label: String,
    val unit: String,
    val keys: List<String>,
    val names: List<String>,
    val decimals: Int,
) {
    AQI("US AQI", "US AQI", listOf("us_aqi"), listOf("US AQI"), 0),
    PM25("PM2.5", "µg/m³", listOf("pm2_5"), listOf("PM2.5"), 1),
    PM10("PM10", "µg/m³", listOf("pm10"), listOf("PM10"), 1),
    PM("PM2.5 + PM10", "µg/m³", listOf("pm2_5", "pm10"), listOf("PM2.5", "PM10"), 1),
    TEMP("Temperature", "°C", listOf("temp"), listOf("Temperature"), 1),
    HUMIDITY("Humidity", "%", listOf("humidity"), listOf("Humidity"), 0),
}

enum class HistoryMode(val label: String) {
    CAL("Calibrated"),
    RAW("Raw"),
    BOTH("Both"),
}

data class HistoryState(
    val isLoading: Boolean = false,
    val data: List<HistoricalDataPoint> = emptyList(),
    val selectedRange: String = "1w",
    val interval: String = "1h",
    val metric: HistoryMetric = HistoryMetric.AQI,
    val mode: HistoryMode = HistoryMode.CAL,
    val customRange: String = "14d",
    val customInterval: String = "1h",
    val showCustomInputs: Boolean = false,
    val error: String? = null,
    val weatherHistory: WeatherHistory? = null,
    val weatherFilter: String = "all",
    val periodActive: Boolean = false,
    val periodMode: HistoryPeriodMode = HistoryPeriodMode.MONTH,
    val period: HistoryPeriod? = null,
    val compareIds: List<String> = emptyList(),
    val compareRows: Map<String, List<HistoricalDataPoint>> = emptyMap(),
    val compareLoading: Boolean = false,
)

data class AppState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val zones: List<Zone> = emptyList(),
    val allAqiData: List<AqiResponse> = emptyList(),
    val pinnedZones: List<AqiResponse> = emptyList(),
    val pinnedIds: Set<String> = emptySet(),
    val sensorInfos: List<SensorInfo> = emptyList(),
)
