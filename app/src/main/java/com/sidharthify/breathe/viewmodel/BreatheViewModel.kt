// SPDX-License-Identifier: MIT
/*
 * BreatheViewModel.kt - App state, fetching, caching, pins and the extended history screen
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

package com.sidharthify.breathe.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sidharthify.breathe.data.AppState
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.HistoryState
import com.sidharthify.breathe.data.RetrofitClient
import com.sidharthify.breathe.data.SensorInfo
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.util.HistoryPeriod
import com.sidharthify.breathe.util.HistoryPeriodMode
import com.sidharthify.breathe.util.loadPinnedOrder
import com.sidharthify.breathe.util.savePinnedOrder
import com.sidharthify.breathe.util.weatherPm25Groups
import com.sidharthify.breathe.widgets.forceWidgetUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

private const val PREFS = "breathe_prefs"
private const val CACHE = "breathe_cache"

// the backend pulls new readings about every 15 minutes, no point asking sooner
private val REFRESH_INTERVAL = 16.minutes

// holds onto the application, never an activity, so rotating doesn't leak
// the old activity through the polling loop
class BreatheViewModel(application: Application) : AndroidViewModel(application) {
    private val app: Context
        get() = getApplication()

    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow(AppState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // US EPA is the default, this only flips on for the indian NAQI. the pref
    // key is still is_us_aqi from back when the flag had the opposite name
    private val _useNaqi = MutableStateFlow(prefs.getBoolean("is_us_aqi", false))
    val useNaqi = _useNaqi.asStateFlow()

    private val _isOverviewHome = MutableStateFlow(prefs.getBoolean("is_overview_home", false))
    val isOverviewHome = _isOverviewHome.asStateFlow()

    // people who pinned something before the welcome dialog existed count as onboarded
    private val _hasOnboarded =
        MutableStateFlow(prefs.getBoolean("has_onboarded", false) || loadPinnedOrder(prefs).isNotEmpty())
    val hasOnboarded = _hasOnboarded.asStateFlow()

    private val _historyState = MutableStateFlow(HistoryState())
    val historyState = _historyState.asStateFlow()

    private var historyZoneId: String? = null
    private var historyFetchJob: Job? = null
    private var pollingJob: Job? = null
    private var lastRefreshMillis = 0L

    init {
        loadFromCache()
        refreshData()
    }

    // called from the ui when the app comes to the foreground. catches up
    // straight away if we were gone longer than one refresh, then keeps polling
    fun onForeground() {
        if (pollingJob?.isActive == true) {
            return
        }
        pollingJob =
            viewModelScope.launch {
                val sinceLast = System.currentTimeMillis() - lastRefreshMillis
                if (sinceLast >= REFRESH_INTERVAL.inWholeMilliseconds) {
                    refreshData(isAutoRefresh = true)
                }
                while (isActive) {
                    delay(REFRESH_INTERVAL)
                    refreshData(isAutoRefresh = true)
                }
            }
    }

    fun onBackground() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun toggleAqiStandard() {
        val newValue = !_useNaqi.value
        _useNaqi.value = newValue
        prefs.edit {
            putBoolean("is_us_aqi", newValue)
        }
        forceWidgetUpdate(app)
    }

    fun toggleOverviewHome() {
        val newValue = !_isOverviewHome.value
        _isOverviewHome.value = newValue
        prefs.edit {
            putBoolean("is_overview_home", newValue)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // auto refreshes stay quiet, they don't flash the spinner or replace
    // cached data with an error card
    fun refreshData(isAutoRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isAutoRefresh) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val pinnedOrder = loadPinnedOrder(prefs)
            val pinnedSet = pinnedOrder.toSet()

            try {
                val (zones, sensors) =
                    coroutineScope {
                        val zonesDeferred = async(Dispatchers.IO) { RetrofitClient.api.getZones().zones }
                        val sensorsDeferred =
                            async(Dispatchers.IO) {
                                runCatching { RetrofitClient.api.getSensorInfo().sensors }.getOrDefault(emptyList())
                            }
                        zonesDeferred.await() to sensorsDeferred.await()
                    }

                _uiState.update { it.copy(zones = zones, sensorInfos = sensors) }

                // pinned zones go first so home fills in before the map's
                // twenty odd other zones have come back
                val (pinnedZones, otherZones) = zones.partition { it.id in pinnedSet }

                val pinnedResults = sortedByPinOrder(fetchAqi(pinnedZones), pinnedOrder)
                _uiState.update { current ->
                    val otherIds = otherZones.map { it.id }.toSet()
                    val keptOthers = current.allAqiData.filter { it.zoneId in otherIds }
                    current.copy(
                        allAqiData = pinnedResults + keptOthers,
                        pinnedZones = pinnedResults,
                        pinnedIds = pinnedSet,
                    )
                }

                val allResults = pinnedResults + fetchAqi(otherZones)
                _uiState.update {
                    it.copy(isLoading = false, allAqiData = allResults)
                }

                lastRefreshMillis = System.currentTimeMillis()
                saveToCache(zones, allResults, sensors)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAutoRefresh) {
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoading = false, error = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    // one /aqi/zones request when the backend has it. older backends 404 on it,
    // so fall back to asking zone by zone, where one bad zone just drops out
    private suspend fun fetchAqi(zones: List<Zone>): List<AqiResponse> {
        if (zones.isEmpty()) {
            return emptyList()
        }
        val bulk =
            withContext(Dispatchers.IO) {
                runCatching { RetrofitClient.api.getZonesAqi(zones.joinToString(",") { it.id }).zones }.getOrNull()
            }
        if (bulk != null) {
            return bulk
        }
        return coroutineScope {
            zones
                .map { zone ->
                    async(Dispatchers.IO) {
                        runCatching { RetrofitClient.api.getZoneAqi(zone.id) }.getOrNull()
                    }
                }.awaitAll()
                .filterNotNull()
        }
    }

    private fun sortedByPinOrder(
        zones: List<AqiResponse>,
        order: List<String>,
    ): List<AqiResponse> {
        val rank = order.withIndex().associate { (index, id) -> id to index }
        return zones.sortedBy { rank[it.zoneId] ?: Int.MAX_VALUE }
    }

    private suspend fun saveToCache(
        zones: List<Zone>,
        aqiData: List<AqiResponse>,
        sensors: List<SensorInfo>,
    ) {
        withContext(Dispatchers.IO) {
            app.getSharedPreferences(CACHE, Context.MODE_PRIVATE).edit {
                putString("cached_zones", gson.toJson(zones))
                putString("cached_aqi", gson.toJson(aqiData))
                putString("cached_sensors", gson.toJson(sensors))
            }
        }
    }

    // lets the app open straight into the last readings instead of a spinner.
    // a broken cache just means we wait for the network like a fresh install
    private fun loadFromCache() {
        try {
            val cache = app.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
            val zonesJson = cache.getString("cached_zones", null) ?: return
            val aqiJson = cache.getString("cached_aqi", null) ?: return
            val sensorsJson = cache.getString("cached_sensors", null)

            val zones: List<Zone> = gson.fromJson(zonesJson, object : TypeToken<List<Zone>>() {}.type)
            val aqiData: List<AqiResponse> = gson.fromJson(aqiJson, object : TypeToken<List<AqiResponse>>() {}.type)
            val sensors: List<SensorInfo> =
                if (sensorsJson != null) {
                    gson.fromJson(sensorsJson, object : TypeToken<List<SensorInfo>>() {}.type)
                } else {
                    emptyList()
                }

            val pinnedOrder = loadPinnedOrder(prefs)
            val pinnedSet = pinnedOrder.toSet()

            _uiState.value =
                AppState(
                    isLoading = false,
                    zones = zones,
                    allAqiData = aqiData,
                    pinnedZones = sortedByPinOrder(aqiData.filter { it.zoneId in pinnedSet }, pinnedOrder),
                    pinnedIds = pinnedSet,
                    sensorInfos = sensors,
                )
        } catch (_: Exception) {
        }
    }

    fun togglePin(zoneId: String) {
        val currentOrder = loadPinnedOrder(prefs)
        val updatedOrder =
            if (zoneId in currentOrder) {
                currentOrder - zoneId
            } else {
                currentOrder + zoneId
            }

        savePinnedOrder(prefs, updatedOrder)

        if (updatedOrder.isNotEmpty() && !_hasOnboarded.value) {
            _hasOnboarded.value = true
            prefs.edit {
                putBoolean("has_onboarded", true)
            }
        }

        _uiState.update { state ->
            val byId = state.allAqiData.associateBy { it.zoneId }
            state.copy(
                pinnedIds = updatedOrder.toSet(),
                pinnedZones = updatedOrder.mapNotNull { byId[it] },
            )
        }

        forceWidgetUpdate(app)
    }

    fun reorderPinnedZones(order: List<String>) {
        savePinnedOrder(prefs, order)
        _uiState.update { state ->
            state.copy(pinnedZones = sortedByPinOrder(state.pinnedZones, order))
        }
        forceWidgetUpdate(app)
    }

    // extended history

    fun openHistory(zoneId: String) {
        historyZoneId = zoneId
        _historyState.value = HistoryState()
        fetchHistoricalData()
    }

    fun setHistoryRange(range: String) {
        _historyState.update {
            it.copy(selectedRange = range, showCustomInputs = false, periodActive = false)
        }
        fetchHistoricalData()
    }

    fun toggleHistoryCustomInputs() {
        _historyState.update { it.copy(showCustomInputs = !it.showCustomInputs, periodActive = false) }
    }

    fun showHistoryPeriod() {
        val state = _historyState.value
        val period = state.period ?: HistoryPeriod.containing(state.periodMode, LocalDate.now())
        applyHistoryPeriod(state.periodMode, period)
    }

    // switching mode keeps you around the same dates, but never in the future
    fun setHistoryPeriodMode(mode: HistoryPeriodMode) {
        val today = LocalDate.now()
        val end = _historyState.value.period?.end ?: today
        val anchor =
            if (end.isAfter(today)) {
                today
            } else {
                end
            }
        applyHistoryPeriod(mode, HistoryPeriod.containing(mode, anchor))
    }

    fun stepHistoryPeriod(direction: Long) {
        val state = _historyState.value
        val period = state.period ?: return
        applyHistoryPeriod(state.periodMode, period.shifted(state.periodMode, direction))
    }

    // in month and week mode any picked day snaps to the month or week around it
    fun pickHistoryPeriod(
        start: LocalDate,
        end: LocalDate,
    ) {
        val mode = _historyState.value.periodMode
        val period =
            if (mode == HistoryPeriodMode.DAYS) {
                HistoryPeriod(start, end)
            } else {
                HistoryPeriod.containing(mode, start)
            }
        applyHistoryPeriod(mode, period)
    }

    private fun applyHistoryPeriod(
        mode: HistoryPeriodMode,
        period: HistoryPeriod,
    ) {
        _historyState.update {
            it.copy(
                periodActive = true,
                periodMode = mode,
                period = period,
                selectedRange = period.token,
                showCustomInputs = false,
            )
        }
        fetchHistoricalData()
    }

    // keeps every range somewhere around a couple hundred points
    private fun historyInterval(state: HistoryState): String {
        val period = state.period
        if (state.periodActive && period != null) {
            return period.interval
        }
        if (state.showCustomInputs) {
            return state.customInterval
        }
        return when (state.selectedRange) {
            "1mo" -> "4h"
            "6mo" -> "1d"
            else -> "1h"
        }
    }

    fun setCustomRange(range: String) {
        _historyState.update { it.copy(customRange = range) }
    }

    fun setCustomInterval(interval: String) {
        _historyState.update { it.copy(customInterval = interval) }
    }

    fun applyCustomHistory() {
        _historyState.update { it.copy(selectedRange = it.customRange) }
        fetchHistoricalData()
    }

    fun setHistorySensor(sensor: String) {
        _historyState.update { it.copy(selectedSensor = sensor) }
        fetchHistoricalData()
    }

    fun toggleHistoryPm25() {
        _historyState.update { it.copy(showPm25 = !it.showPm25) }
        fetchHistoricalData()
    }

    fun toggleHistoryPm10() {
        _historyState.update { it.copy(showPm10 = !it.showPm10) }
        fetchHistoricalData()
    }

    fun setWeatherFilter(condition: String) {
        _historyState.update { it.copy(weatherFilter = condition) }
    }

    // a single node is addressed as zone_node on the backend
    private fun historyLocation(
        zoneId: String,
        state: HistoryState,
    ): String {
        if (state.selectedSensor == "zone") {
            return zoneId
        }
        return "${zoneId}_${state.selectedSensor}"
    }

    private fun fetchHistoricalData() {
        val zoneId = historyZoneId ?: return
        val state = _historyState.value
        val location = historyLocation(zoneId, state)
        val interval = historyInterval(state)

        // turning both off would just give an empty chart, so treat it as both on
        val metrics =
            buildList {
                if (state.showPm25) {
                    add("pm2.5")
                }
                if (state.showPm10) {
                    add("pm10")
                }
                if (isEmpty()) {
                    add("pm2.5")
                    add("pm10")
                }
            }.joinToString(",")

        _historyState.update { it.copy(isLoading = true, error = null) }

        // only the newest request gets to write its result
        historyFetchJob?.cancel()
        historyFetchJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    coroutineScope {
                        val historyDeferred =
                            async {
                                RetrofitClient.api.getHistoricalData(
                                    location = location,
                                    timeRange = state.selectedRange,
                                    interval = interval,
                                    metrics = metrics,
                                )
                            }
                        val weatherDeferred =
                            async {
                                runCatching {
                                    RetrofitClient.api.getWeatherHistory(
                                        zoneId = zoneId,
                                        timeRange = state.selectedRange,
                                        interval = interval,
                                    )
                                }.getOrNull()
                            }
                        val response = historyDeferred.await()
                        val weather = weatherDeferred.await()

                        // drop back to all if the new range has no hours with the picked weather
                        val groups = weatherPm25Groups(response.data, weather)
                        val filterHasData = (groups[state.weatherFilter]?.second ?: 0) > 0
                        val nextFilter =
                            if (state.weatherFilter != "all" && !filterHasData) {
                                "all"
                            } else {
                                state.weatherFilter
                            }

                        _historyState.update {
                            it.copy(
                                isLoading = false,
                                data = response.data,
                                stats = response.stats,
                                weatherHistory = weather,
                                weatherFilter = nextFilter,
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _historyState.update {
                        it.copy(isLoading = false, error = "Failed to load: ${e.localizedMessage}")
                    }
                }
            }
    }

    // the csv always carries both pollutants, whatever the chart is showing
    fun historyCsvUrl(): String? {
        val zoneId = historyZoneId ?: return null
        val state = _historyState.value
        val location = historyLocation(zoneId, state)
        val interval = historyInterval(state)
        return "https://api.breatheoss.app/historical-data/$location/${state.selectedRange}/$interval/pm2.5,pm10?format=csv"
    }
}
