package com.sidharthify.breathe

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    error: String?,
    pinnedZones: List<AqiResponse>,
    zones: List<Zone>,
    onGoToExplore: () -> Unit,
    onRetry: () -> Unit
) {
    var selectedZone by remember { mutableStateOf(pinnedZones.firstOrNull()) }

    LaunchedEffect(pinnedZones) {
        if (selectedZone == null && pinnedZones.isNotEmpty()) {
            selectedZone = pinnedZones.first()
        } else if (pinnedZones.isNotEmpty() && !pinnedZones.any { it.zoneId == selectedZone?.zoneId }) {
            selectedZone = pinnedZones.first()
        }
    }

    if (isLoading && pinnedZones.isEmpty()) {
        LoadingScreen()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Pinned Locations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            if (pinnedZones.isNotEmpty()) {
                val listState = rememberLazyListState()
                
                LazyRow(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pinnedZones, key = { it.zoneId }) { zone ->
                        PinnedMiniCard(
                            zone = zone,
                            isSelected = zone.zoneId == (selectedZone?.zoneId),
                            onClick = { selectedZone = zone }
                        )
                    }
                }
            } else if (error != null) {
                ErrorCard(msg = error, onRetry = onRetry)
            } else {
                EmptyStateCard(onGoToExplore)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (selectedZone != null) {
            item(key = "dashboard_detail") {
                val provider = remember(selectedZone, zones) {
                    zones.find { it.id == selectedZone!!.zoneId }?.provider
                }
                MainDashboardDetail(selectedZone!!, provider, isDarkTheme)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    error: String?,
    zones: List<Zone>,
    pinnedIds: Set<String>,
    query: String,
    onSearchChange: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    onRetry: () -> Unit
) {
    val filteredZones = remember(query, zones) {
        zones.filter {
            it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search J&K...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading && zones.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (error != null && zones.isEmpty()) {
            ErrorCard(msg = error, onRetry = onRetry)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredZones.isEmpty()) {
                    item { Text("No zones found", modifier = Modifier.padding(8.dp)) }
                }
                
                items(filteredZones, key = { it.id }) { zone ->
                    Box(modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))) {
                        ZoneListItem(
                            zone = zone,
                            isPinned = pinnedIds.contains(zone.id),
                            onPinClick = { onPinToggle(zone.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: BreatheViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    
    var showDataSourceDialog by remember { mutableStateOf(false) }

    val currentVersion = try {
        // com.sidharthify.breathe.BuildConfig.VERSION_NAME
        "v2.2"
    } catch (e: Exception) {
        "Unknown"
    }

    if (showDataSourceDialog) {
        AlertDialog(
            onDismissRequest = { showDataSourceDialog = false },
            title = { Text("Data Sources") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Jammu & Kashmir regions (excl. Srinagar)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Air quality and weather data sourced from Open-Meteo.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "https://open-meteo.com/",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { uriHandler.openUri("https://open-meteo.com/") }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column {
                        Text(
                            text = "Srinagar region",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Air quality data sourced from OpenAQ, using measurements published by the Central Pollution Control Board (CPCB).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "cpcb.nic.in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { uriHandler.openUri("https://cpcb.nic.in/") }
                            )
                            Text(
                                text = "openaq.org",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { uriHandler.openUri("https://openaq.org/") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDataSourceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dark Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Toggle app appearance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        SettingsItem("Data Standards", "Indian National Air Quality Index (NAQI)")

        SettingsItem(
            title = "Data Sources", 
            subtitle = "OpenMeteo & OpenAQ",
            onClick = { showDataSourceDialog = true }
        )

        SettingsItem(
            title = "Breathe OSS",
            subtitle = "View Source on GitHub",
            onClick = { uriHandler.openUri("https://github.com/breathe-OSS") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Developers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            title = "Sidharth Sharma",
            subtitle = "@sidharthify",
            onClick = { uriHandler.openUri("https://github.com/sidharthify") }
        )
        SettingsItem(
            title = "Aaditya Gupta",
            subtitle = "@Flashwreck",
            onClick = { uriHandler.openUri("https://github.com/Flashwreck") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Contributors",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            title = "Veer P.S Singh",
            subtitle = "@Lostless1907",
            onClick = { uriHandler.openUri("https://github.com/Lostless1907") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Updates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Version: $currentVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.checkForUpdates(context, currentVersion)
                    }
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check for Updates")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "View all releases",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/breathe-OSS/breathe/releases/")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(onClick != null) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}