// SPDX-License-Identifier: MIT
/*
 * MainActivity.kt - The main entry point of the app, setting up theming, navigation, and hosting the different screens
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
 * Copyright (C) 2026 Suvesh Moza <hellosuvesh@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.data.AnimationSettings
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.navigation.AppScreen
import com.sidharthify.breathe.navigation.LocalIsTabletLandscape
import com.sidharthify.breathe.navigation.LocalNavBarClearance
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
                    .edit {
                        putBoolean("anim_enabled", settings.animationsEnabled)
                            .putBoolean("anim_screen_transitions", settings.screenTransitions)
                            .putBoolean("anim_color_transitions", settings.colorTransitions)
                            .putBoolean("anim_number_animations", settings.numberAnimations)
                            .putBoolean("anim_pulse_effects", settings.pulseEffects)
                            .putBoolean("anim_morphing_pill", settings.morphingPill)
                            .putBoolean("anim_press_feedback", settings.pressFeedback)
                            .putBoolean("anim_list_animations", settings.listAnimations)
                    }
            }

            val toggleTheme: () -> Unit = {
                val newValue = !isDarkThemeState.value
                isDarkThemeState.value = newValue
                prefs.edit { putBoolean("is_dark_theme", newValue) }
            }

            val toggleAmoled: () -> Unit = {
                val newValue = !isAmoledState.value
                isAmoledState.value = newValue
                prefs.edit { putBoolean("is_amoled", newValue) }
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

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    val motionScheme = MaterialTheme.motionScheme

    val configuration = LocalConfiguration.current
    val useNavRail =
        configuration.smallestScreenWidthDp >= 600 &&
            configuration.screenWidthDp > configuration.screenHeightDp

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val navBarClearance =
        if (useNavRail) {
            navBarPadding.calculateBottomPadding() + 24.dp
        } else {
            navBarPadding.calculateBottomPadding() + 120.dp
        }
    val navRailClearance =
        if (useNavRail) {
            navBarPadding.calculateEndPadding(LocalLayoutDirection.current) + 96.dp
        } else {
            0.dp
        }

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
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(end = navRailClearance),
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition",
                    transitionSpec = {
                        if (!animationSettings.screenTransitions) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val isForward = targetState.ordinal > initialState.ordinal
                            val direction =
                                if (useNavRail && isForward) {
                                    AnimatedContentTransitionScope.SlideDirection.Up
                                } else if (useNavRail) {
                                    AnimatedContentTransitionScope.SlideDirection.Down
                                } else if (isForward) {
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                } else {
                                    AnimatedContentTransitionScope.SlideDirection.Right
                                }
                            val spatial = motionScheme.defaultSpatialSpec<IntOffset>()

                            // M3 lateral: peer screens slide in unison, no fade.
                            slideIntoContainer(
                                towards = direction,
                                animationSpec = spatial,
                            ) togetherWith slideOutOfContainer(
                                towards = direction,
                                animationSpec = spatial,
                            )
                        }
                    },
                ) { screen ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .consumeWindowInsets(WindowInsets.navigationBars),
                    ) {
                        CompositionLocalProvider(
                            LocalNavBarClearance provides navBarClearance,
                            LocalIsTabletLandscape provides useNavRail,
                        ) {
                            when (screen) {
                                AppScreen.Home -> {
                                    HomeScreen(
                                        isLoading = uiState.isLoading,
                                        isDarkTheme = isDarkTheme,
                                        isAmoled = isAmoled,
                                        error = uiState.error,
                                        pinnedZones = uiState.pinnedZones,
                                        zones = uiState.zones,
                                        sensorInfos = uiState.sensorInfos,
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
                                        sensorInfos = uiState.sensorInfos,
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

            if (useNavRail) {
                VerticalFloatingToolbar(
                    expanded = true,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(end = 24.dp)
                            .width(72.dp)
                            .zIndex(1f),
                    shape = RoundedCornerShape(100),
                    contentPadding = PaddingValues(8.dp),
                    expandedShadowElevation = 12.dp,
                ) {
                    AppScreen.entries.forEach { screen ->
                        NavigationItem(
                            screen = screen,
                            isSelected = currentScreen == screen,
                            animationSettings = animationSettings,
                            onClick = { currentScreen = screen },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                        )
                    }
                }
            } else {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    1f to MaterialTheme.colorScheme.surface,
                                ),
                            ),
                )

                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 24.dp, start = 48.dp, end = 48.dp)
                            .height(72.dp)
                            .zIndex(1f),
                    shape = RoundedCornerShape(100),
                    contentPadding = PaddingValues(8.dp),
                    expandedShadowElevation = 12.dp,
                ) {
                    AppScreen.entries.forEach { screen ->
                        NavigationItem(
                            screen = screen,
                            isSelected = currentScreen == screen,
                            animationSettings = animationSettings,
                            onClick = { currentScreen = screen },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigationItem(
    screen: AppScreen,
    isSelected: Boolean,
    animationSettings: AnimationSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        animationSpec =
            if (isSelected && animationSettings.colorTransitions) {
                tween(durationMillis = 150)
            } else {
                tween(durationMillis = 0)
            },
        label = "IconColor",
    )
    val pillColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            },
        animationSpec =
            if (isSelected && animationSettings.colorTransitions) {
                tween(durationMillis = 150)
            } else {
                tween(durationMillis = 0)
            },
        label = "PillColor",
    )

    Box(
        modifier = modifier.expressiveClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        MorphingPill(
            isSelected = isSelected,
            from = MaterialShapes.Circle,
            to = screen.shape,
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

fun Modifier.expressiveClickable(onClick: () -> Unit): Modifier =
    composed {
        val animationSettings = LocalAnimationSettings.current
        val maxSquishPx = with(LocalDensity.current) { 32.dp.toPx() }
        var isPressed by remember { mutableStateOf(false) }
        var longestSidePx by remember { mutableIntStateOf(0) }
        val pressedScale =
            if (longestSidePx > 0) {
                (1f - maxSquishPx / longestSidePx).coerceAtLeast(0.90f)
            } else {
                0.90f
            }
        val scale by animateFloatAsState(
            targetValue = if (isPressed && animationSettings.pressFeedback) pressedScale else 1f,
            animationSpec =
                if (animationSettings.pressFeedback) {
                    spring(dampingRatio = 0.4f, stiffness = 400f)
                } else {
                    spring(stiffness = 10000f)
                },
            label = "Squish",
        )

        this
            .onSizeChanged { longestSidePx = maxOf(it.width, it.height) }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = expressiveShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
