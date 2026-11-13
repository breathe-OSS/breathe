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
import com.sidharthify.breathe.data.HistoryMetric
import com.sidharthify.breathe.data.HistoryMode
import com.sidharthify.breathe.data.HistoryState
import com.sidharthify.breathe.data.RetrofitClient
import com.sidharthify.breathe.data.SensorInfo
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.util.HistoryPeriod
import com.sidharthify.breathe.util.HistoryPeriodMode
import com.sidharthify.breathe.util.asRaw
import com.sidharthify.breathe.util.isEstimate
import com.sidharthify.breathe.util.loadPinnedOrder
import com.sidharthify.breathe.util.migratePinnedIds
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

const val HISTORY_METRICS =
    "pm2.5,pm10,temp,humidity,us_aqi,pm2.5_raw,pm10_raw,temp_raw,humidity_raw,us_aqi_raw"

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

    private val _rawMode = MutableStateFlow(prefs.getBoolean("raw_readings", false))
    val rawMode = _rawMode.asStateFlow()

    private val _rawHintHidden = MutableStateFlow(prefs.getBoolean("raw_hint_hidden", false))
    val rawHintHidden = _rawHintHidden.asStateFlow()

    private var sourceAqi: List<AqiResponse> = emptyList()

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
    private var compareFetchJob: Job? = null
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

    fun setRawMode(on: Boolean) {
        if (_rawMode.value == on) {
            return
        }
        _rawMode.value = on
        prefs.edit {
            putBoolean("raw_readings", on)
        }
        _uiState.update { state ->
            val shown = shown(sourceAqi)
            val byId = shown.associateBy { it.zoneId }
            state.copy(
                allAqiData = shown,
                pinnedZones = state.pinnedZones.mapNotNull { byId[it.zoneId] },
            )
        }
        forceWidgetUpdate(app)
    }

    fun hideRawHint() {
        _rawHintHidden.value = true
        prefs.edit {
            putBoolean("raw_hint_hidden", true)
        }
    }

    private fun shown(list: List<AqiResponse>): List<AqiResponse> {
        if (!_rawMode.value) {
            return list
        }
        return list.map { it.asRaw() }
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

            var pinnedOrder = loadPinnedOrder(prefs)
            var pinnedSet = pinnedOrder.toSet()

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

                val migrated = migratePinnedIds(pinnedOrder, zones)
                if (migrated != pinnedOrder) {
                    savePinnedOrder(prefs, migrated)
                    pinnedOrder = migrated
                    pinnedSet = migrated.toSet()
                    forceWidgetUpdate(app)
                }

                _uiState.update { it.copy(zones = zones, sensorInfos = sensors) }

                // pinned zones go first so home fills in before the map's
                // twenty odd other zones have come back
                val (pinnedZones, otherZones) = zones.partition { it.id in pinnedSet }

                val pinnedResults = sortedByPinOrder(fetchAqi(pinnedZones), pinnedOrder)
                val otherIds = otherZones.map { it.id }.toSet()
                sourceAqi = pinnedResults + sourceAqi.filter { it.zoneId in otherIds }
                _uiState.update { current ->
                    current.copy(
                        allAqiData = shown(sourceAqi),
                        pinnedZones = shown(pinnedResults),
                        pinnedIds = pinnedSet,
                    )
                }

                val allResults = pinnedResults + fetchAqi(otherZones)
                sourceAqi = allResults
                _uiState.update {
                    it.copy(isLoading = false, allAqiData = shown(allResults))
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

            val pinnedOrder = migratePinnedIds(loadPinnedOrder(prefs), zones)
            val pinnedSet = pinnedOrder.toSet()
            sourceAqi = aqiData
            val shownData = shown(aqiData)

            _uiState.value =
                AppState(
                    isLoading = false,
                    zones = zones,
                    allAqiData = shownData,
                    pinnedZones = sortedByPinOrder(shownData.filter { it.zoneId in pinnedSet }, pinnedOrder),
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
        val sameZone = historyZoneId == zoneId
        val previous = _historyState.value
        historyZoneId = zoneId
        val mode =
            if (_rawMode.value) {
                HistoryMode.RAW
            } else {
                HistoryMode.CAL
            }
        _historyState.value =
            HistoryState(
                metric = previous.metric,
                mode = mode,
                compareIds =
                    if (sameZone) {
                        previous.compareIds
                    } else {
                        emptyList()
                    },
            )
        fetchHistoricalData()
    }

    fun setHistoryRange(range: String) {
        _historyState.update {
            it.copy(selectedRange = range, interval = "1h", showCustomInputs = false, periodActive = false)
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
                interval = "1h",
                showCustomInputs = false,
            )
        }
        fetchHistoricalData()
    }

    fun setCustomRange(range: String) {
        _historyState.update { it.copy(customRange = range) }
    }

    fun setCustomInterval(interval: String) {
        _historyState.update { it.copy(customInterval = interval) }
    }

    fun applyCustomHistory() {
        _historyState.update {
            val interval = it.customInterval.trim().ifEmpty { "1h" }
            it.copy(selectedRange = it.customRange.trim(), interval = interval)
        }
        fetchHistoricalData()
    }

    fun setHistoryMetric(metric: HistoryMetric) {
        _historyState.update { it.copy(metric = metric) }
    }

    fun setHistoryMode(mode: HistoryMode) {
        _historyState.update { it.copy(mode = mode) }
    }

    fun setWeatherFilter(condition: String) {
        _historyState.update { it.copy(weatherFilter = condition) }
    }

    fun toggleCompare(id: String) {
        _historyState.update { state ->
            val next =
                if (id in state.compareIds) {
                    state.compareIds - id
                } else {
                    state.compareIds + id
                }
            state.copy(compareIds = next)
        }
        fetchCompareRows()
    }

    fun setCompareIds(ids: List<String>) {
        _historyState.update { it.copy(compareIds = ids) }
        fetchCompareRows()
    }

    fun comparablePlaces(): List<Zone> {
        val zoneId = historyZoneId
        return _uiState.value.zones.filter { !it.isEstimate && it.id != zoneId }
    }

    private fun fetchHistoricalData() {
        val zoneId = historyZoneId ?: return
        val state = _historyState.value

        _historyState.update { it.copy(isLoading = true, error = null, compareRows = emptyMap()) }

        historyFetchJob?.cancel()
        historyFetchJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    coroutineScope {
                        val historyDeferred =
                            async {
                                RetrofitClient.api.getHistoricalData(
                                    location = zoneId,
                                    timeRange = state.selectedRange,
                                    interval = state.interval,
                                    metrics = HISTORY_METRICS,
                                )
                            }
                        val weatherDeferred =
                            async {
                                runCatching {
                                    RetrofitClient.api.getWeatherHistory(
                                        zoneId = zoneId,
                                        timeRange = state.selectedRange,
                                        interval = state.interval,
                                    )
                                }.getOrNull()
                            }
                        val response = historyDeferred.await()
                        val weather = weatherDeferred.await()
                        val rows = response.data.sortedBy { it.ts }

                        val groups = weatherPm25Groups(rows, weather)
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
                                data = rows,
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
                fetchCompareRows()
            }
    }

    private fun fetchCompareRows() {
        val state = _historyState.value
        val missing = state.compareIds.filter { it !in state.compareRows }
        if (missing.isEmpty()) {
            return
        }
        _historyState.update { it.copy(compareLoading = true) }
        compareFetchJob?.cancel()
        compareFetchJob =
            viewModelScope.launch(Dispatchers.IO) {
                val results =
                    coroutineScope {
                        missing
                            .map { id ->
                                async {
                                    val rows =
                                        runCatching {
                                            RetrofitClient.api
                                                .getHistoricalData(
                                                    location = id,
                                                    timeRange = state.selectedRange,
                                                    interval = state.interval,
                                                    metrics = HISTORY_METRICS,
                                                ).data
                                                .sortedBy { it.ts }
                                        }.getOrDefault(emptyList())
                                    id to rows
                                }
                            }.awaitAll()
                    }
                _historyState.update { current ->
                    if (current.selectedRange != state.selectedRange || current.interval != state.interval) {
                        current
                    } else {
                        current.copy(compareRows = current.compareRows + results, compareLoading = false)
                    }
                }
            }
    }

    fun historyCsvUrl(): String? {
        val zoneId = historyZoneId ?: return null
        val state = _historyState.value
        return "https://api.breatheoss.app/historical-data/$zoneId/${state.selectedRange}/${state.interval}/$HISTORY_METRICS?format=csv"
    }
}
