package com.sidharthify.breathe

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.navigation.AppScreen
import com.sidharthify.breathe.ui.screens.ExploreScreen
import com.sidharthify.breathe.ui.screens.HomeScreen
import com.sidharthify.breathe.ui.screens.MapScreen
import com.sidharthify.breathe.ui.screens.SettingsScreen
import com.sidharthify.breathe.viewmodel.BreatheViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        val prefs = getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()

            val isDarkThemeState = remember {
                val saved = if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else systemDark
                mutableStateOf(saved)
            }
            
            val isAmoledState = remember {
                mutableStateOf(prefs.getBoolean("is_amoled", false))
            }

            val toggleTheme: () -> Unit = {
                val newValue = !isDarkThemeState.value
                isDarkThemeState.value = newValue
                prefs.edit().putBoolean("is_dark_theme", newValue).apply()
            }
            
            val toggleAmoled: () -> Unit = {
                val newValue = !isAmoledState.value
                isAmoledState.value = newValue
                prefs.edit().putBoolean("is_amoled", newValue).apply()
            }

            BreatheTheme(
                darkTheme = isDarkThemeState.value,
                amoledMode = isAmoledState.value
            ) {
                BreatheApp(
                    isDarkTheme = isDarkThemeState.value,
                    isAmoled = isAmoledState.value,
                    onThemeToggle = toggleTheme,
                    onAmoledToggle = toggleAmoled
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BreatheApp(
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    onThemeToggle: () -> Unit,
    onAmoledToggle: () -> Unit,
    viewModel: BreatheViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val isUsAqi by viewModel.isUsAqi.collectAsState()
    
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition",
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }

                        (slideIntoContainer(
                            towards = direction,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)
                        ) + fadeIn(animationSpec = tween(400))).togetherWith(
                            slideOutOfContainer(
                                towards = direction,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)
                            ) + fadeOut(animationSpec = tween(400))
                        )
                    }
                ) { screen ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 0.dp)
                            .consumeWindowInsets(WindowInsets.navigationBars)
                    ) {
                        Box(modifier = Modifier.padding(bottom = 100.dp)) {
                             when (screen) {
                                AppScreen.Home -> HomeScreen(
                                    isLoading = uiState.isLoading,
                                    isDarkTheme = isDarkTheme,
                                    error = uiState.error,
                                    pinnedZones = uiState.pinnedZones,
                                    zones = uiState.zones,
                                    onGoToExplore = { currentScreen = AppScreen.Explore },
                                    onRetry = { viewModel.refreshData(context) }
                                )
                                AppScreen.Map -> MapScreen(
                                    zones = uiState.zones,
                                    allAqiData = uiState.allAqiData,
                                    pinnedIds = uiState.pinnedIds,
                                    isDarkTheme = isDarkTheme,
                                    isUsAqi = isUsAqi,
                                    onPinToggle = { id -> viewModel.togglePin(context, id) }
                                )
                                AppScreen.Explore -> ExploreScreen(
                                    isLoading = uiState.isLoading,
                                    isDarkTheme = isDarkTheme,
                                    error = uiState.error,
                                    zones = uiState.zones,
                                    pinnedIds = uiState.pinnedIds,
                                    query = searchQuery,
                                    onSearchChange = viewModel::onSearchQueryChanged,
                                    onPinToggle = { id -> viewModel.togglePin(context, id) },
                                    onRetry = { viewModel.refreshData(context) }
                                )
                                AppScreen.Settings -> SettingsScreen(
                                    isDarkTheme = isDarkTheme,
                                    isAmoled = isAmoled,
                                    onThemeToggle = onThemeToggle,
                                    onAmoledToggle = onAmoledToggle,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            // Navigation Pill
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp, start = 48.dp, end = 48.dp)
                    .height(72.dp)
                    .shadow(
                        elevation = 12.dp, 
                        shape = RoundedCornerShape(100), 
                        ambientColor = Color.Black.copy(alpha = 0.4f), 
                        spotColor = Color.Black.copy(alpha = 0.6f)
                    ),
                shape = RoundedCornerShape(100),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppScreen.entries.forEach { screen ->
                        val isSelected = currentScreen == screen
                        
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            label = "IconColor"
                        )
                        
                        val pillColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            label = "PillColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .expressiveClickable { currentScreen = screen },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(64.dp)
                                    .background(color = pillColor, shape = RoundedCornerShape(100))
                            )
                            
                            Icon(
                                if (isSelected) screen.iconFilled else screen.iconOutlined,
                                contentDescription = screen.label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.expressiveClickable(onClick: () -> Unit): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "Squish"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        onClick()
                    }
                }
            }
        }
}

@Composable
fun BreatheTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val colorScheme = if (darkTheme && amoledMode) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color(0xFF000000),     // Very dark
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerHighest = Color(0xFF1F1F1F)
        )
    } else {
        baseScheme
    }

    val expressiveShapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp), 
        large = RoundedCornerShape(32.dp),
        extraLarge = RoundedCornerShape(48.dp)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = expressiveShapes,
        content = content
    )
}