package com.sidharthify.breathe.data

import com.google.gson.annotations.SerializedName

data class ZonesResponse(
    val zones: List<Zone>
)

data class Zone(
    val id: String,
    val name: String,
    val provider: String?,
    val lat: Double?,
    val lon: Double?
)

data class Trends(
    @SerializedName("change_1h") val change1h: Int?,
    @SerializedName("change_24h") val change24h: Int?
)

data class AqiResponse(
    @SerializedName("zone_id") val zoneId: String,
    @SerializedName("zone_name") val zoneName: String,
    @SerializedName("aqi") val nAqi: Int,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("main_pollutant") val mainPollutant: String,
    @SerializedName("aqi_breakdown") val aqiBreakdown: Map<String, Int>?,
    @SerializedName("concentrations_us_units") val concentrations: Map<String, Double>?,
    @SerializedName("timestamp_unix") val timestampUnix: Double?,
    @SerializedName("last_update") val lastUpdateStr: String?,
    @SerializedName("history") val history: List<HistoryPoint>? = emptyList(),
    @SerializedName("trends") val trends: Trends? = null
)

data class HistoryPoint(
    @SerializedName("ts") val ts: Long,
    @SerializedName("aqi") val aqi: Int,
    @SerializedName("us_aqi") val usAqi: Int?
)

data class AppState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val zones: List<Zone> = emptyList(),
    val allAqiData: List<AqiResponse> = emptyList(),
    val pinnedZones: List<AqiResponse> = emptyList(),
    val pinnedIds: Set<String> = emptySet()
)