package com.sidharthify.breathe

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen(val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Explore("Explore", Icons.Filled.Search, Icons.Outlined.Search),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class BreatheViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun init(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val zonesResp = RetrofitClient.api.getZones()
                val zonesList = zonesResp.zones

                val prefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
                val pinnedSet = prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()

                val pinnedAqiList = mutableListOf<AqiResponse>()
                
                for (id in pinnedSet) {
                    try {
                        pinnedAqiList.add(RetrofitClient.api.getZoneAqi(id))
                    } catch (e: Exception) { }
                }

                _uiState.value = AppState(
                    isLoading = false,
                    error = null,
                    zones = zonesList,
                    pinnedZones = pinnedAqiList,
                    pinnedIds = pinnedSet
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not connect to server.\n(${e.localizedMessage})"
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(context: Context, zoneId: String) {
        val currentSet = _uiState.value.pinnedIds.toMutableSet()
        if (currentSet.contains(zoneId)) {
            currentSet.remove(zoneId)
        } else {
            currentSet.add(zoneId)
        }
        context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            .edit().putStringSet("pinned_ids", currentSet).apply()
        init(context)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            val context = LocalContext.current
            
            val colorScheme = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BreatheApp(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreatheApp(isDarkTheme: Boolean, onThemeToggle: () -> Unit, viewModel: BreatheViewModel = viewModel()) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                AppScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                if (isSelected) screen.iconFilled else screen.iconOutlined, 
                                contentDescription = screen.label
                            ) 
                        },
                        label = { 
                            Text(
                                screen.label, 
                                fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                AppScreen.Home -> HomeScreen(
                    isLoading = state.isLoading,
                    error = state.error,
                    pinnedZones = state.pinnedZones,
                    onGoToExplore = { currentScreen = AppScreen.Explore },
                    onRetry = { viewModel.init(context) }
                )
                AppScreen.Explore -> ExploreScreen(
                    isLoading = state.isLoading,
                    error = state.error,
                    zones = state.zones,
                    pinnedIds = state.pinnedIds,
                    query = viewModel.searchQuery.collectAsState().value,
                    onSearchChange = viewModel::onSearchQueryChanged,
                    onPinToggle = { id -> viewModel.togglePin(context, id) },
                    onRetry = { viewModel.init(context) }
                )
                AppScreen.Settings -> SettingsScreen(isDarkTheme, onThemeToggle)
            }
        }
    }
}

// ----------------------------------------------------------------
// SECTION 1: HOME
// ----------------------------------------------------------------
@Composable
fun HomeScreen(
    isLoading: Boolean,
    error: String?,
    pinnedZones: List<AqiResponse>,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Pinned Locations",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (pinnedZones.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pinnedZones) { zone ->
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

        Spacer(modifier = Modifier.height(32.dp))

        if (selectedZone != null) {
            MainDashboardDetail(selectedZone!!)
        }
    }
}

@Composable
fun PinnedMiniCard(zone: AqiResponse, isSelected: Boolean, onClick: () -> Unit) {
    val aqiColor = getAqiColor(zone.usAqi)
    val containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSelected) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = zone.zoneName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(aqiColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${zone.usAqi}", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MainDashboardDetail(zone: AqiResponse) {
    val aqiColor = getAqiColor(zone.usAqi)
    val aqiBgColor = aqiColor.copy(alpha = 0.15f)

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Now Viewing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            zone.zoneName, 
            style = MaterialTheme.typography.displaySmall, 
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            lineHeight = 40.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = aqiBgColor),
            modifier = Modifier.fillMaxWidth().height(260.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${zone.usAqi}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp),
                        fontWeight = FontWeight.Black,
                        color = aqiColor
                    )
                    Surface(
                        color = aqiColor,
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = "AQI US",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Primary: ${zone.mainPollutant.uppercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Pollutants", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        val pollutants = zone.concentrations ?: emptyMap()
        if (pollutants.isEmpty()) Text("No detailed data.") else FlowRowGrid(pollutants)
    }
}

// ----------------------------------------------------------------
// SECTION 2: EXPLORE
// ----------------------------------------------------------------
@Composable
fun ExploreScreen(
    isLoading: Boolean,
    error: String?,
    zones: List<Zone>,
    pinnedIds: Set<String>,
    query: String,
    onSearchChange: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    onRetry: () -> Unit
) {
    val filteredZones = zones.filter {
        it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
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
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (filteredZones.isEmpty()) {
                    item { Text("No zones found", modifier = Modifier.padding(8.dp)) }
                }
                items(filteredZones) { zone ->
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

@Composable
fun ZoneListItem(zone: Zone, isPinned: Boolean, onPinClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(zone.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    zone.provider ?: "Unknown", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPinClick) {
                Icon(
                    if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if(isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ----------------------------------------------------------------
// SECTION 3: SETTINGS
// ----------------------------------------------------------------
@Composable
fun SettingsScreen(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
        SettingsItem("Sources", "CPCB (Govt. of India) & OpenWeather")
        
        SettingsItem(
            title = "Breathe OSS",
            subtitle = "View Source on GitHub",
            onClick = { uriHandler.openUri("https://github.com/breathe-OSS") }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Code, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Developers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "• sidharthify (GitHub)",
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/sidharthify") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Flashwreck (GitHub)",
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/Flashwreck") }
                )
            }
        }
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
            Icon(Icons.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
fun ErrorCard(msg: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connection Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Text(msg, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyStateCard(onGoToExplore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PushPin, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Pinned Zones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Pin locations to see them here", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(onClick = onGoToExplore) { Text("Go to Explore") }
    }
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun FlowRowGrid(pollutants: Map<String, Double>) {
    val items = pollutants.entries.toList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (key, value) ->
                    PollutantCard(Modifier.weight(1f), formatPollutantName(key), "$value", if (key.lowercase() == "co") "mg/m³" else "µg/m³")
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PollutantCard(modifier: Modifier, name: String, value: String, unit: String) {
    Card(
        modifier = modifier, 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun formatPollutantName(key: String): String {
    return when(key.lowercase()) {
        "pm2_5", "pm2.5" -> "PM2.5"
        "pm10" -> "PM10"
        "no2" -> "NO₂"
        "so2" -> "SO₂"
        "co" -> "CO"
        "o3" -> "O₃"
        else -> key.uppercase()
    }
}

fun getAqiColor(aqi: Int): Color {
    return when (aqi) {
        in 0..50 -> Color(0xFF00E400)
        in 51..100 -> Color(0xFFFFFF00)
        in 101..150 -> Color(0xFFFF7E00)
        in 151..200 -> Color(0xFFFF0000)
        in 201..300 -> Color(0xFF8F3F97)
        else -> Color(0xFF7E0023)
    }
}