package com.sidharthify.breathe.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.ui.components.ErrorCard
import com.sidharthify.breathe.ui.components.ZoneListItem
import com.sidharthify.breathe.expressiveClickable // Import

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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Explore", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = query,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search city or station ID...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            shape = RoundedCornerShape(100),
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
                    Box(
                        modifier = Modifier
                            .animateItem(tween(durationMillis = 300))
                            .expressiveClickable     { onPinToggle(zone.id) }
                    ) {
                        ZoneListItem(
                            zone = zone,
                            isPinned = pinnedIds.contains(zone.id),
                            onPinClick = {}
                        )
                    }
                }
            }
        }
    }
}