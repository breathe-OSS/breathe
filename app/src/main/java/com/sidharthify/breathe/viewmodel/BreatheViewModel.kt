package com.sidharthify.breathe.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sidharthify.breathe.data.AppState
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.DailyAqi
import com.sidharthify.breathe.data.RetrofitClient
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.forceWidgetUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BreatheViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isUsAqi = MutableStateFlow(false)
    val isUsAqi = _isUsAqi.asStateFlow()

    private val _dailyAqi = MutableStateFlow<List<DailyAqi>>(emptyList())
    val dailyAqi: StateFlow<List<DailyAqi>> = _dailyAqi.asStateFlow()

    private val gson = Gson()
    private var pollingJob: Job? = null
    private var isInitialLoad = true

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
        _isUsAqi.value = prefs.getBoolean("is_us_aqi", false)

        if (isInitialLoad) {
            loadFromCache(context)
            isInitialLoad = false
        }

        refreshData(context)
        startPolling(context)
    }

    private fun startPolling(context: Context) {
        if (pollingJob?.isActive == true) return
        pollingJob =
            viewModelScope.launch {
                while (isActive) {
                    delay(60000)
                    refreshData(context, isAutoRefresh = true)
                }
            }
    }

    fun toggleAqiStandard(context: Context) {
        val newValue = !_isUsAqi.value
        _isUsAqi.value = newValue
        context
            .getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_us_aqi", newValue)
            .apply()
    }

    fun refreshData(
        context: Context,
        isAutoRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            if (!isAutoRefresh) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val prefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            val pinnedSet = prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()

            try {
                val zonesList = RetrofitClient.api.getZones().zones
                _uiState.update { it.copy(zones = zonesList) }

                val (pinnedZones, unpinnedZones) = zonesList.partition { it.id in pinnedSet }

                val pinnedResults =
                    pinnedZones
                        .map { zone ->
                            async(Dispatchers.IO) {
                                try {
                                    RetrofitClient.api.getZoneAqi(zone.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        .awaitAll()
                        .filterNotNull()

                _uiState.update { current ->
                    val unpinnedIds = unpinnedZones.map { it.id }.toSet()
                    val preservedUnpinned =
                        current.allAqiData.filter { it.zoneId in unpinnedIds }

                    current.copy(
                        allAqiData = pinnedResults + preservedUnpinned,
                        pinnedZones = pinnedResults,
                        pinnedIds = pinnedSet,
                    )
                }

                val unpinnedResults =
                    if (unpinnedZones.isNotEmpty()) {
                        unpinnedZones
                            .map { zone ->
                                async(Dispatchers.IO) {
                                    try {
                                        RetrofitClient.api.getZoneAqi(zone.id)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            .awaitAll()
                            .filterNotNull()
                    } else {
                        emptyList()
                    }

                val completeList = pinnedResults + unpinnedResults

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allAqiData = completeList,
                    )
                }

                saveToCache(context, zonesList, completeList)
            } catch (e: Exception) {
                if (!isAutoRefresh) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Error: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private fun saveToCache(
        context: Context,
        zones: List<Zone>,
        aqiData: List<AqiResponse>,
    ) {
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

            val pinPrefs =
                context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            val pinnedSet = pinPrefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()

            if (zonesJson != null && aqiJson != null) {
                val zonesType = object : TypeToken<List<Zone>>() {}.type
                val aqiType = object : TypeToken<List<AqiResponse>>() {}.type

                val zones: List<Zone> = gson.fromJson(zonesJson, zonesType)
                val aqiData: List<AqiResponse> = gson.fromJson(aqiJson, aqiType)
                val pinnedResults = aqiData.filter { it.zoneId in pinnedSet }

                _uiState.value =
                    AppState(
                        isLoading = false,
                        zones = zones,
                        allAqiData = aqiData,
                        pinnedZones = pinnedResults,
                        pinnedIds = pinnedSet,
                    )
            }
        } catch (_: Exception) {
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(
        context: Context,
        zoneId: String,
    ) {
        val currentSet = _uiState.value.pinnedIds.toMutableSet()
        val isAdding = !currentSet.contains(zoneId)

        if (isAdding) currentSet.add(zoneId) else currentSet.remove(zoneId)

        context
            .getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("pinned_ids", currentSet)
            .apply()

        val updatedPinnedList =
            _uiState.value.allAqiData.filter { it.zoneId in currentSet }

        _uiState.update {
            it.copy(
                pinnedIds = currentSet,
                pinnedZones = updatedPinnedList,
            )
        }

        forceWidgetUpdate(context)
    }

    fun buildWeeklyTrend(
        zoneId: String,
        history: List<AqiResponse>,
    ) {
        val grouped =
            history
                .filter { it.zoneId == zoneId }
                .groupBy { it.timestamp.take(10) }

        val daily =
            grouped.entries
                .sortedBy { it.key }
                .takeLast(7)
                .map { (date, values) ->
                    val aqiValues =
                        values.mapNotNull {
                            if (_isUsAqi.value) it.usAqi else it.nAqi
                        }

                    if (aqiValues.size < 3) {
                        DailyAqi(date, null)
                    } else {
                        DailyAqi(date, aqiValues.sorted()[aqiValues.size / 2])
                    }
                }

        _dailyAqi.value = daily
    }
}
