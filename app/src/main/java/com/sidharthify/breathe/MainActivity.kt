package com.sidharthify.breathe

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

enum class AppScreen(val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Map("Map", Icons.Filled.Map, Icons.Outlined.Map),
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
            
            val prefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
            val pinnedSet = prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()

            try {
                val zonesList = RetrofitClient.api.getZones().zones

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

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.localizedMessage}"
                )
            }
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
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            val context = LocalContext.current
            
            DisposableEffect(isDarkTheme) {
                val window = (context as Activity).window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)

                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme

                onDispose { }
            }

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
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 300),
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.Home -> HomeScreen(
                        isLoading = state.isLoading,
                        error = state.error,
                        pinnedZones = state.pinnedZones,
                        onGoToExplore = { currentScreen = AppScreen.Explore },
                        onRetry = { viewModel.init(context) }
                    )
                    AppScreen.Map -> MapScreen(
                        zones = state.zones,
                        allAqiData = state.allAqiData,
                        pinnedIds = state.pinnedIds,
                        isDarkTheme = isDarkTheme,
                        onPinToggle = { id -> viewModel.togglePin(context, id) }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    zones: List<Zone>,
    allAqiData: List<AqiResponse>,
    pinnedIds: Set<String>,
    isDarkTheme: Boolean,
    onPinToggle: (String) -> Unit
) {
    val context = LocalContext.current
    val startPoint = GeoPoint(34.0837, 74.7973) 
    
    var selectedZoneData by remember { mutableStateOf<AqiResponse?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    Scaffold { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(9.0)
                        controller.setCenter(startPoint)
                    }
                },
                update = { mapView ->
                    val tilesOverlay = mapView.overlayManager.tilesOverlay
                    if (isDarkTheme) {
                        val inverseMatrix = ColorMatrix(
                            floatArrayOf(
                                -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                                0.0f, -1.0f, 0.0f, 0.0f, 255f,
                                0.0f, 0.0f, -1.0f, 0.0f, 255f,
                                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                            )
                        )
                        tilesOverlay.setColorFilter(ColorMatrixColorFilter(inverseMatrix))
                    } else {
                        tilesOverlay.setColorFilter(null)
                    }

                    mapView.overlays.clear()
                    zones.forEach { zone ->
                        if (zone.lat != null && zone.lon != null) {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(zone.lat, zone.lon)
                            marker.title = zone.name
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                            val data = allAqiData.find { it.zoneId == zone.id }
                            
                            val colorInt = if (data != null) {
                                getAqiColor(data.nAqi).toArgb()
                            } else {
                                android.graphics.Color.GRAY
                            }

                            marker.icon = createBlobIcon(context, colorInt)

                            marker.setOnMarkerClickListener { _, _ ->
                                if (data != null) {
                                    selectedZoneData = data
                                }
                                true
                            }
                            mapView.overlays.add(marker)
                        }
                    }
                    mapView.invalidate()
                }
            )
        }

        if (selectedZoneData != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedZoneData = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 48.dp)
                ) {
                    MainDashboardDetail(selectedZoneData!!)
                    
                    val isPinned = pinnedIds.contains(selectedZoneData!!.zoneId)
                    Box(Modifier.padding(horizontal = 24.dp)) {
                        OutlinedButton(
                            onClick = { onPinToggle(selectedZoneData!!.zoneId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isPinned) "Unpin from Home" else "Pin to Home")
                        }
                    }
                }
            }
        }
    }
}

fun createBlobIcon(context: Context, color: Int): Drawable {
    val radius = 32f 
    val bitmap = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true }

    paint.color = color
    canvas.drawCircle(radius, radius, radius, paint)

    return BitmapDrawable(context.resources, bitmap)
}

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
    val aqiColor = getAqiColor(zone.nAqi)
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
                    text = "${zone.nAqi}", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MainDashboardDetail(zone: AqiResponse) {
    val aqiColor = getAqiColor(zone.nAqi)
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
                        text = "${zone.nAqi}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 90.sp),
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
                            text = "NAQI",
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

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val statusText = when {
                        zone.timestampUnix != null -> "Updated ${getTimeAgo(zone.timestampUnix.toLong())}"
                        !zone.lastUpdateStr.isNullOrEmpty() -> "Updated: ${zone.lastUpdateStr}"
                        else -> "Live"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

@Composable
fun SettingsScreen(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val uriHandler = LocalUriHandler.current

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
        SettingsItem("Sources", "CPCB (Govt. of India) & OpenMeteo")

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
            title = "Veer P.S Singh - Logo designer",
            subtitle = "@Lostless1907",
            onClick = { uriHandler.openUri("https://github.com/Lostless1907") }
        )
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
        in 0..50 -> Color(0xFF55A84F)
        in 51..100 -> Color(0xFFA3C853)
        in 101..200 -> Color(0xFFFDD74B)
        in 201..300 -> Color(0xFFFB9A34)
        in 301..400 -> Color(0xFFE93F33)
        else -> Color(0xFFAF2D24)
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        else -> ">1d ago"
    }
}