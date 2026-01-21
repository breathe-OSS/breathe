package com.sidharthify.breathe

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.sidharthify.breathe.data.AnimationSettings
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.navigation.AppScreen
import com.sidharthify.breathe.ui.components.MorphingPill
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

            val isDarkThemeState =
                remember {
                    val saved = if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else systemDark
                    mutableStateOf(saved)
                }

            val isAmoledState =
                remember {
                    mutableStateOf(prefs.getBoolean("is_amoled", false))
                }

            val animationSettingsState =
                remember {
                    mutableStateOf(
                        AnimationSettings(
                            animationsEnabled = prefs.getBoolean("anim_enabled", true),
                            screenTransitions = prefs.getBoolean("anim_screen_transitions", true),
                            colorTransitions = prefs.getBoolean("anim_color_transitions", true),
                            numberAnimations = prefs.getBoolean("anim_number_animations", true),
                            pulseEffects = prefs.getBoolean("anim_pulse_effects", true),
                            morphingPill = prefs.getBoolean("anim_morphing_pill", true),
                            pressFeedback = prefs.getBoolean("anim_press_feedback", true),
                            listAnimations = prefs.getBoolean("anim_list_animations", true),
                        ),
                    )
                }

            val updateAnimationSettings: (AnimationSettings) -> Unit = { settings ->
                animationSettingsState.value = settings
                prefs
                    .edit()
                    .putBoolean("anim_enabled", settings.animationsEnabled)
                    .putBoolean("anim_screen_transitions", settings.screenTransitions)
                    .putBoolean("anim_color_transitions", settings.colorTransitions)
                    .putBoolean("anim_number_animations", settings.numberAnimations)
                    .putBoolean("anim_pulse_effects", settings.pulseEffects)
                    .putBoolean("anim_morphing_pill", settings.morphingPill)
                    .putBoolean("anim_press_feedback", settings.pressFeedback)
                    .putBoolean("anim_list_animations", settings.listAnimations)
                    .apply()
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
                amoledMode = isAmoledState.value,
            ) {
                CompositionLocalProvider(LocalAnimationSettings provides animationSettingsState.value) {
                    BreatheApp(
                        isDarkTheme = isDarkThemeState.value,
                        isAmoled = isAmoledState.value,
                        onThemeToggle = toggleTheme,
                        onAmoledToggle = toggleAmoled,
                        animationSettings = animationSettingsState.value,
                        onAnimationSettingsChange = updateAnimationSettings,
                    )
                }
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
    animationSettings: AnimationSettings,
    onAnimationSettingsChange: (AnimationSettings) -> Unit,
    viewModel: BreatheViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val isUsAqi by viewModel.isUsAqi.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.Home) }

    // navigate to Home first, then exit
    BackHandler(enabled = currentScreen != AppScreen.Home) {
        currentScreen = AppScreen.Home
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition",
                    transitionSpec = {
                        if (!animationSettings.screenTransitions) {
                            fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                        } else {
                            val direction =
                                if (targetState.ordinal > initialState.ordinal) {
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                } else {
                                    AnimatedContentTransitionScope.SlideDirection.Right
                                }

                            (
                                slideIntoContainer(
                                    towards = direction,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
                                ) + fadeIn(animationSpec = tween(400))
                            ).togetherWith(
                                slideOutOfContainer(
                                    towards = direction,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
                                ) + fadeOut(animationSpec = tween(400)),
                            )
                        }
                    },
                ) { screen ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(bottom = 0.dp)
                                .consumeWindowInsets(WindowInsets.navigationBars),
                    ) {
                        Box(modifier = Modifier.padding(bottom = 100.dp)) {
                            when (screen) {
                                AppScreen.Home -> {
                                    HomeScreen(
                                        isLoading = uiState.isLoading,
                                        isDarkTheme = isDarkTheme,
                                        isAmoled = isAmoled,
                                        error = uiState.error,
                                        pinnedZones = uiState.pinnedZones,
                                        zones = uiState.zones,
                                        onGoToExplore = { currentScreen = AppScreen.Explore },
                                        onRetry = { viewModel.refreshData(context) },
                                    )
                                }

                                AppScreen.Map -> {
                                    MapScreen(
                                        zones = uiState.zones,
                                        allAqiData = uiState.allAqiData,
                                        pinnedIds = uiState.pinnedIds,
                                        isDarkTheme = isDarkTheme,
                                        isUsAqi = isUsAqi,
                                        onPinToggle = { id -> viewModel.togglePin(context, id) },
                                    )
                                }

                                AppScreen.Explore -> {
                                    ExploreScreen(
                                        isLoading = uiState.isLoading,
                                        isDarkTheme = isDarkTheme,
                                        error = uiState.error,
                                        zones = uiState.zones,
                                        pinnedIds = uiState.pinnedIds,
                                        query = searchQuery,
                                        onSearchChange = viewModel::onSearchQueryChanged,
                                        onPinToggle = { id -> viewModel.togglePin(context, id) },
                                        onRetry = { viewModel.refreshData(context) },
                                    )
                                }

                                AppScreen.Settings -> {
                                    SettingsScreen(
                                        isDarkTheme = isDarkTheme,
                                        isAmoled = isAmoled,
                                        onThemeToggle = onThemeToggle,
                                        onAmoledToggle = onAmoledToggle,
                                        animationSettings = animationSettings,
                                        onAnimationSettingsChange = onAnimationSettingsChange,
                                        viewModel = viewModel,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Pill
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 24.dp, start = 48.dp, end = 48.dp)
                        .height(72.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(100),
                            ambientColor = Color.Black.copy(alpha = 0.4f),
                            spotColor = Color.Black.copy(alpha = 0.6f),
                        ),
                shape = RoundedCornerShape(100),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    AppScreen.entries.forEach { screen ->
                        val isSelected = currentScreen == screen
                        val targetShape = screen.shape
                        val iconColor by animateColorAsState(
                            targetValue =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(
                                            alpha = 0.5f,
                                        )
                                },
                            animationSpec =
                                if (animationSettings.colorTransitions) {
                                    tween(durationMillis = 300)
                                } else {
                                    tween(durationMillis = 0)
                                },
                            label = "IconColor",
                        )
                        val pillColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            animationSpec =
                                if (animationSettings.colorTransitions) {
                                    tween(durationMillis = 300)
                                } else {
                                    tween(durationMillis = 0)
                                },
                            label = "PillColor",
                        )

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .expressiveClickable { currentScreen = screen },
                            contentAlignment = Alignment.Center,
                        ) {
                            MorphingPill(
                                isSelected = isSelected,
                                from = MaterialShapes.Circle,
                                to = targetShape,
                                color = pillColor,
                                modifier = Modifier.size(50.dp),
                            )

                            Icon(
                                if (isSelected) screen.iconFilled else screen.iconOutlined,
                                contentDescription = screen.label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.expressiveClickable(onClick: () -> Unit): Modifier =
    composed {
        val animationSettings = LocalAnimationSettings.current
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed && animationSettings.pressFeedback) 0.90f else 1f,
            animationSpec =
                if (animationSettings.pressFeedback) {
                    spring(dampingRatio = 0.4f, stiffness = 400f)
                } else {
                    spring(stiffness = 10000f)
                },
            label = "Squish",
        )

        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.pointerInput(Unit) {
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
    content: @Composable () -> Unit,
) {
    val baseScheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                darkColorScheme()
            }

            else -> {
                lightColorScheme()
            }
        }

    val colorScheme =
        if (darkTheme && amoledMode) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainer = Color(0xFF000000), // Very dark
                surfaceContainerHigh = Color(0xFF141414),
                surfaceContainerHighest = Color(0xFF1F1F1F),
            )
        } else {
            baseScheme
        }

    val expressiveShapes =
        Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(48.dp),
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
        content = content,
    )
}
