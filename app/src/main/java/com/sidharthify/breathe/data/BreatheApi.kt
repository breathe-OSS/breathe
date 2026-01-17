package com.sidharthify.breathe.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface BreatheApi {
    @GET("/zones")
    suspend fun getZones(): ZonesResponse

    @GET("/aqi/zone/{zone_id}")
    suspend fun getZoneAqi(@Path("zone_id") zoneId: String): AqiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://breathe-oss-5248fe612dbc.herokuapp.com/"

    val api: BreatheApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BreatheApi::class.java)
    }
}