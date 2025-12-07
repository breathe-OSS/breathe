import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        enableEdgeToEdge()

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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
            AnimatedContent(
                targetState = currentScreen,
                label = "ScreenTransition",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(150)) + 
                     scaleIn(initialScale = 0.95f, animationSpec = tween(150))) 
                    .togetherWith(
                        fadeOut(animationSpec = tween(150))
                    )
                }
            ) { screen ->
                when (screen) {
                    AppScreen.Home -> HomeScreen(
                        isLoading = state.isLoading,
                        error = state.error,
                        pinnedZones = state.pinnedZones,
                        zones = state.zones,
                        isDarkTheme = isDarkTheme,
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
                        isDarkTheme = isDarkTheme,
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