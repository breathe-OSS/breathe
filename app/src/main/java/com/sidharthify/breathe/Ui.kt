package com.sidharthify.breathe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pullrefresh.PullRefreshIndicator
import androidx.compose.material3.pullrefresh.pullRefresh
import androidx.compose.material3.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@Composable
fun BreezeApp(vm: MainViewModel) {
    val zones by vm.zones.collectAsState()
    val selected by vm.selected.collectAsState()
    val loading by vm.loading.collectAsState()
    val scaffoldState = rememberTopAppBarState()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MainScaffold(
                zones = zones,
                selected = selected,
                loading = loading,
                onRefresh = { vm.loadZones() },
                onSelect = { id -> vm.loadZoneAqi(id) },
                onClearSelection = { /* no-op or implement */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    zones: List<Zone>,
    selected: AqiResponse?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onClearSelection: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val refreshing = loading
    val pullState = rememberPullRefreshState(refreshing, { onRefresh() })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("breeze", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    ) { outerPadding ->
        Box(modifier = Modifier
            .padding(outerPadding)
            .pullRefresh(pullState)
            .fillMaxSize()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                ZoneList(
                    zones = zones,
                    modifier = Modifier.weight(0.45f).fillMaxHeight(),
                    onSelect = onSelect
                )

                Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

                if (selected != null) {
                    ZoneDetails(
                        aqi = selected,
                        modifier = Modifier.weight(0.55f).fillMaxHeight()
                    )
                } else {
                    EmptyDetails(modifier = Modifier.weight(0.55f).fillMaxHeight())
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun ZoneList(
    zones: List<Zone>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    Card(modifier = modifier.padding(8.dp), shape = RoundedCornerShape(8.dp)) {
        Column {
            Text(
                "zones",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(12.dp)
            )
            Divider()
            LazyColumn {
                items(zones) { z ->
                    ZoneRow(z, onSelect)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ZoneRow(z: Zone, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(z.id) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = z.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = z.provider ?: "", style = MaterialTheme.typography.bodySmall)
        }
        Text(text = z.id, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ZoneDetails(aqi: AqiResponse, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(12.dp), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = aqi.zone_name ?: (aqi.zone_id ?: "zone"),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text("last: ${aqi.timestamp_unix ?: "-"}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("overall us aqi: ${aqi.us_aqi}", style = MaterialTheme.typography.headlineSmall)
            Text("main pollutant: ${aqi.main_pollutant ?: "n/a"}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Text("breakdown", style = MaterialTheme.typography.titleMedium)
            aqi.aqi_breakdown.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(k.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(v.toString(), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("concentrations (µg/m³)", style = MaterialTheme.typography.titleMedium)
            aqi.concentrations_raw_ugm3.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(k.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(String.format("%.2f", v), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun EmptyDetails(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        Text("select a zone to view details", style = MaterialTheme.typography.bodyLarge)
    }
}
