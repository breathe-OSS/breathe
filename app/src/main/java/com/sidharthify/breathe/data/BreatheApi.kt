// SPDX-License-Identifier: MIT
/*
 * BreatheApi.kt - Retrofit interface to interact with the Breathe API
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

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BreatheApi {
    @GET("/zones")
    suspend fun getZones(): ZonesResponse

    @GET("/aqi/zone/{zone_id}")
    suspend fun getZoneAqi(
        @Path("zone_id") zoneId: String,
    ): AqiResponse

    // the same payloads as getZoneAqi in one request, newer backends only
    @GET("/aqi/zones")
    suspend fun getZonesAqi(
        @Query("ids") ids: String,
    ): ZonesAqiResponse

    @GET("/sensor-info")
    suspend fun getSensorInfo(): SensorInfoResponse

    // location is a zone id, or zone_node for one sensor. time_range is 1w, 1mo,
    // 6mo, a custom 14d style range or a start_end pair of dates
    @GET("/historical-data/{location}/{time_range}/{interval}/{metrics}")
    suspend fun getHistoricalData(
        @Path("location") location: String,
        @Path("time_range") timeRange: String,
        @Path("interval") interval: String,
        @Path("metrics") metrics: String,
        @Query("format") format: String = "json",
    ): HistoricalDataResponse

    // weather buckets line up with the history buckets through anchor, see
    // WeatherHistory.kt
    @GET("/weather-history/{zone_id}/{time_range}/{interval}")
    suspend fun getWeatherHistory(
        @Path("zone_id") zoneId: String,
        @Path("time_range") timeRange: String,
        @Path("interval") interval: String,
    ): WeatherHistory
}

object RetrofitClient {
    private const val BASE_URL = "https://api.breatheoss.app/"

    val api: BreatheApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BreatheApi::class.java)
    }
}
