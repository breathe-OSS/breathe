package com.sidharthify.breathe.data

data class DailyAqi(
    val date: String, // yyyy-mm-dd
    val aqi: Int?     // null = insufficient data
)
