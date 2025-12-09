package com.sidharthify.breathe

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class BreatheViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val gson = Gson()
    private var pollingJob: Job? = null
    private var isInitialLoad = true

    fun init(context: Context) {
        if (isInitialLoad) {
            loadFromCache(context)
            isInitialLoad = false
        }

        refreshData(context)

        startPolling(context)
    }

    private fun startPolling(context: Context) {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(60000) // auto refresh every 60 seconds
                refreshData(context, isAutoRefresh = true)
            }
        }
    }

    fun refreshData(context: Context, isAutoRefresh: Boolean = false) {
        viewModelScope.launch {
            if (_uiState.value.allAqiData.isEmpty() && !isAutoRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val prefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            val pinnedSet = prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()

            try {
                val zonesList = RetrofitClient.api.getZones().zones

                // PARALLEL FETCHING
                val allJobs = zonesList.map { zone ->
                    async {
                        try { RetrofitClient.api.getZoneAqi(zone.id) } catch (e: Exception) { null }
                    }
                }

                val allResults = allJobs.awaitAll().filterNotNull()
                val pinnedResults = allResults.filter { it.zoneId in pinnedSet }

                _uiState.value = AppState(
                    isLoading = false,
                    error = if (zonesList.isEmpty()) "Could not connect to server." else null,
                    zones = zonesList,
                    allAqiData = allResults,
                    pinnedZones = pinnedResults,
                    pinnedIds = pinnedSet
                )

                // SAVE TO CACHE
                saveToCache(context, zonesList, allResults)

            } catch (e: Exception) {
                if (!isAutoRefresh) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun saveToCache(context: Context, zones: List<Zone>, aqiData: List<AqiResponse>) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("breathe_cache", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("cached_zones", gson.toJson(zones))
            editor.putString("cached_aqi", gson.toJson(aqiData))
            editor.apply()
        }
    }

    private fun loadFromCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences("breathe_cache", Context.MODE_PRIVATE)
            val zonesJson = prefs.getString("cached_zones", null)
            val aqiJson = prefs.getString("cached_aqi", null)
            
            val pinPrefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            val pinnedSet = pinPrefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()

            if (zonesJson != null && aqiJson != null) {
                val zonesType = object : TypeToken<List<Zone>>() {}.type
                val aqiType = object : TypeToken<List<AqiResponse>>() {}.type

                val zones: List<Zone> = gson.fromJson(zonesJson, zonesType)
                val aqiData: List<AqiResponse> = gson.fromJson(aqiJson, aqiType)
                val pinnedResults = aqiData.filter { it.zoneId in pinnedSet }

                _uiState.value = AppState(
                    isLoading = false,
                    zones = zones,
                    allAqiData = aqiData,
                    pinnedZones = pinnedResults,
                    pinnedIds = pinnedSet
                )
            }
        } catch (e: Exception) {
            // cache load failed, wait for network
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(context: Context, zoneId: String) {
        val currentSet = _uiState.value.pinnedIds.toMutableSet()
        val isAdding = !currentSet.contains(zoneId)

        if (isAdding) currentSet.add(zoneId) else currentSet.remove(zoneId)

        context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            .edit().putStringSet("pinned_ids", currentSet).apply()

        val updatedPinnedList = _uiState.value.allAqiData.filter { it.zoneId in currentSet }

        _uiState.value = _uiState.value.copy(
            pinnedIds = currentSet,
            pinnedZones = updatedPinnedList
        )

        forceWidgetUpdate(context)
    }

    fun checkForUpdates(context: Context, currentVersion: String) {
        viewModelScope.launch {
            try {
                val latestReleaseUrl = "https://api.github.com/repos/breathe-OSS/breathe/releases/latest"
                val response = withContext(Dispatchers.IO) { URL(latestReleaseUrl).readText() }
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name")
                val htmlUrl = json.getString("html_url")

                if (latestTag.removePrefix("v") != currentVersion.removePrefix("v")) {
                    Toast.makeText(context, "Update found: $latestTag", Toast.LENGTH_LONG).show()
                    val browserIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(htmlUrl)
                    )
                    context.startActivity(browserIntent)
                } else {
                    Toast.makeText(context, "You are on the latest version.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to check for updates.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}