package com.sidharthify.breathe

import retrofit2.Retrofit
import retrofit2.http.GET
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

@Serializable
data class Zone(val id: String, val name: String, val provider: String, val lat: Double, val lon: Double)

@Serializable
data class ZonesWrapper(val zones: List<Zone>)

interface ApiService {
    @GET("/zones")
    suspend fun getZones(): ZonesWrapper
}

object Network {
    fun api(): ApiService {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder().build()
        val content = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(content))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
