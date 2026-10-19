package com.sidharthify.breathe.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.data.SensorInfo
import com.sidharthify.breathe.navigation.LocalIsTabletLandscape
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.ui.components.EmptyStateCard
import com.sidharthify.breathe.ui.components.ErrorCard
import com.sidharthify.breathe.ui.components.ExtendedHistoryScreen
import com.sidharthify.breathe.ui.components.LoadingScreen
import com.sidharthify.breathe.ui.components.MainDashboardDetail
import com.sidharthify.breathe.ui.components.PinnedHintCard
import com.sidharthify.breathe.ui.components.PinnedOverviewGrid
import com.sidharthify.breathe.ui.components.PinnedZonesButtonGroup
import com.sidharthify.breathe.ui.components.ReorderPinnedZonesSheet
import com.sidharthify.breathe.ui.components.WelcomeDialog
import com.sidharthify.breathe.viewmodel.BreatheViewModel
import kotlinx.coroutines.launch

@ExperimentalMaterial3ExpressiveApi
@Composable
fun HomeScreen(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    isAmoled: Boolean = false,
    error: String?,
    pinnedZones: List<AqiResponse>,
    allAqiData: List<AqiResponse>,
    pinnedIds: Set<String>,
    zones: List<Zone>,
    sensorInfos: List<SensorInfo>,
    onGoToExplore: () -> Unit,
    onRetry: () -> Unit,
    viewModel: BreatheViewModel = viewModel(),
) {
    val isUsAqi by viewModel.isUsAqi.collectAsState()
    val isOverviewHome by viewModel.isOverviewHome.collectAsState()
    val hasOnboarded by viewModel.hasOnboarded.collectAsState()
    val historyState by viewModel.historyState.collectAsState()
    val context = LocalContext.current

    val pullRefreshState = rememberPullToRefreshState()

    var selectedZone by remember { mutableStateOf(pinnedZones.firstOrNull()) }
    var showHistory by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }
    var showPinnedHint by rememberSaveable { mutableStateOf(false) }
    var isOverviewExpanded by rememberSaveable { mutableStateOf(false) }
    val showOverviewGrid = isOverviewHome && pinnedZones.isNotEmpty() && !isOverviewExpanded
    val coroutineScope = rememberCoroutineScope()

    if (showReorderSheet) {
        ReorderPinnedZonesSheet(
            zones = pinnedZones,
            isUsAqi = isUsAqi,
            onReorder = { order -> viewModel.reorderPinnedZones(context, order) },
            onDismiss = { showReorderSheet = false },
        )
    }

    // Track whether we just came back from history to scroll to bottom
    var scrollToBottom by remember { mutableStateOf(false) }
    val lazyColumnState = rememberLazyListState()

    BackHandler(enabled = showHistory) {
        showHistory = false
        scrollToBottom = true
    }

    BackHandler(enabled = isOverviewHome && isOverviewExpanded && !showHistory) {
        isOverviewExpanded = false
    }

    val liveZoneIds =
        remember(zones) {
            zones
                .filter { it.provider?.contains("airgradient", ignoreCase = true) == true }
                .map { it.id }
                .toSet()
        }

    val openZone: (AqiResponse) -> Unit = { zone ->
        if (zone.zoneId !in pinnedIds) {
            viewModel.togglePin(context, zone.zoneId)
        }
        selectedZone = zone
        isOverviewExpanded = true
        coroutineScope.launch {
            lazyColumnState.animateScrollToItem(0)
        }
    }

    val snapshotZones = allAqiData.filter { it.zoneId in liveZoneIds }

    if (!hasOnboarded && pinnedZones.isEmpty() && snapshotZones.size >= 2) {
        WelcomeDialog(
            snapshotZones = snapshotZones,
            isUsAqi = isUsAqi,
            onZoneClick = { zone ->
                showPinnedHint = true
                openZone(zone)
            },
            onGoToExplore = onGoToExplore,
        )
    }

    LaunchedEffect(pinnedZones) {
        if (selectedZone == null && pinnedZones.isNotEmpty()) {
            selectedZone = pinnedZones.first()
        } else if (pinnedZones.isNotEmpty()) {
            val updatedZone = pinnedZones.find { it.zoneId == selectedZone?.zoneId }
            if (updatedZone != null) {
                selectedZone = updatedZone
            } else {
                selectedZone = pinnedZones.first()
            }
        }
    }

    // Scroll to the dashboard_detail item after returning from history
    LaunchedEffect(scrollToBottom) {
        if (scrollToBottom) {
            lazyColumnState.animateScrollToItem(3)
            scrollToBottom = false
        }
    }

    AnimatedContent(
        targetState = showHistory,
        transitionSpec = {
            val slideSpec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
            if (targetState) {
                slideInHorizontally(slideSpec) { it }.togetherWith(
                    slideOutHorizontally(slideSpec) { -it }
                )
            } else {
                slideInHorizontally(slideSpec) { -it }.togetherWith(
                    slideOutHorizontally(slideSpec) { it }
                )
            }
        },
        label = "HistoryTransition",
    ) { isHistoryVisible ->
        if (isHistoryVisible && selectedZone != null) {
            val nodeKeys = remember(selectedZone) {
                selectedZone?.nodes?.keys?.toList() ?: emptyList()
            }

            ExtendedHistoryScreen(
                zoneName = selectedZone!!.zoneName,
                historyState = historyState,
                nodeKeys = nodeKeys,
                onBack = {
                    showHistory = false
                    scrollToBottom = true
                },
                onRangeSelected = { viewModel.setHistoryRange(it) },
                onToggleCustom = { viewModel.toggleHistoryCustomInputs() },
                onCustomRangeChanged = { viewModel.setCustomRange(it) },
                onCustomIntervalChanged = { viewModel.setCustomInterval(it) },
                onApplyCustom = { viewModel.applyCustomHistory() },
                onSensorSelected = { viewModel.setHistorySensor(it) },
                onTogglePm25 = { viewModel.toggleHistoryPm25() },
                onTogglePm10 = { viewModel.toggleHistoryPm10() },
                onDownloadCSV = { viewModel.downloadHistoryCSV(context) },
                onWeatherFilterSelected = { viewModel.setWeatherFilter(it) },
            )
        } else {
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
                        state = lazyColumnState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = LocalNavBarClearance.current),
                    ) {
                        item {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, end = 12.dp, top = 24.dp, bottom = 8.dp)
                                        .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Pinned Locations",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )

                                if (pinnedZones.size > 1) {
                                    IconButton(onClick = { showReorderSheet = true }) {
                                        Icon(
                                            Icons.Filled.SwapHoriz,
                                            contentDescription = "Reorder pinned locations",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            if (showOverviewGrid) {
                                PinnedOverviewGrid(
                                    zones = pinnedZones,
                                    isUsAqi = isUsAqi,
                                    liveZoneIds = liveZoneIds,
                                    columns = if (LocalIsTabletLandscape.current) 4 else 2,
                                    onZoneClick = openZone,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            } else if (isOverviewHome && pinnedZones.isNotEmpty()) {
                                TextButton(
                                    onClick = { isOverviewExpanded = false },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("All Pinned Locations")
                                }
                            } else if (pinnedZones.isNotEmpty()) {
                                val listState = rememberLazyListState()

                                LazyRow(
                                    state = listState,
                                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
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
                            AnimatedVisibility(
                                visible = showPinnedHint && pinnedZones.isNotEmpty(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                PinnedHintCard(
                                    onDismiss = { showPinnedHint = false },
                                    modifier =
                                        Modifier
                                            .padding(horizontal = 24.dp)
                                            .padding(top = 24.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (selectedZone != null && !showOverviewGrid) {
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
                                    sensorInfos = sensorInfos,
                                    onOpenHistory = {
                                        viewModel.openHistory(selectedZone!!.zoneId)
                                        showHistory = true
                                    },
                                    isWideLayout = LocalIsTabletLandscape.current,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
