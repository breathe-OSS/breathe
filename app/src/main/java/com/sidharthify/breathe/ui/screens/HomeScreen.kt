package com.sidharthify.breathe.ui.screens

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.ui.components.AqiTrendGraph
import com.sidharthify.breathe.ui.components.EmptyStateCard
import com.sidharthify.breathe.ui.components.ErrorCard
import com.sidharthify.breathe.ui.components.LoadingScreen
import com.sidharthify.breathe.ui.components.MainDashboardDetail
import com.sidharthify.breathe.ui.components.PinnedZonesButtonGroup
import com.sidharthify.breathe.viewmodel.BreatheViewModel

@ExperimentalMaterial3ExpressiveApi
@Composable
fun HomeScreen(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    isAmoled: Boolean = false,
    error: String?,
    pinnedZones: List<AqiResponse>,
    zones: List<Zone>,
    onGoToExplore: () -> Unit,
    onRetry: () -> Unit,
    viewModel: BreatheViewModel = viewModel(),
) {
    val isUsAqi by viewModel.isUsAqi.collectAsState()
    val dailyTrend by viewModel.dailyAqi.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()

    var selectedZone by remember { mutableStateOf(pinnedZones.firstOrNull()) }

    LaunchedEffect(pinnedZones) {
        if (selectedZone == null && pinnedZones.isNotEmpty()) {
            selectedZone = pinnedZones.first()
        } else if (pinnedZones.isNotEmpty()) {
            val updatedZone = pinnedZones.find { it.zoneId == selectedZone?.zoneId }
            selectedZone = updatedZone ?: pinnedZones.first()
        }
    }

    LaunchedEffect(selectedZone) {
        selectedZone?.let { zone ->
            viewModel.buildWeeklyTrend(
                zoneId = zone.zoneId,
                history = pinnedZones
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        state = pullRefreshState,
        onRefresh = onRetry,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isLoading,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        if (isLoading && pinnedZones.isEmpty()) {
            LoadingScreen()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                item {
                    Text(
                        text = "Pinned Locations",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                item {
                    if (pinnedZones.isNotEmpty()) {
                        val listState = rememberLazyListState()

                        LazyRow(
                            state = listState,
                            flingBehavior = rememberSnapFlingBehavior(listState),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            item {
                                PinnedZonesButtonGroup(
                                    zones = pinnedZones,
                                    selectedZoneId = selectedZone?.zoneId,
                                    isUsAqi = isUsAqi,
                                    isAmoled = isAmoled,
                                    onZoneSelected = { selectedZone = it },
                                )
                            }
                        }
                    } else if (error != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                        ) {
                            ErrorCard(msg = error, onRetry = onRetry)
                        }
                    } else {
                        EmptyStateCard(onGoToExplore)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                if (selectedZone != null) {
                    item(key = "dashboard_detail") {
                        val provider =
                            remember(selectedZone, zones) {
                                zones.find { it.id == selectedZone!!.zoneId }?.provider
                            }

                        MainDashboardDetail(
                            zone = selectedZone!!,
                            provider = provider,
                            isDarkTheme = isDarkTheme,
                            isUsAqi = isUsAqi,
                        )
                    }
                }

                if (dailyTrend.isNotEmpty()) {
                    item(key = "aqi_trend") {
                        AqiTrendGraph(daily = dailyTrend)
                    }
                }
            }
        }
    }
}
