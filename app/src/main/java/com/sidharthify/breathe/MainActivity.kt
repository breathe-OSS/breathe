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
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.data.AnimationSettings
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.navigation.AppScreen
import com.sidharthify.breathe.navigation.LocalIsTabletLandscape
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.sidharthify.breathe.navigation.LiquidNavBar
import com.sidharthify.breathe.ui.LocalGlassEnabled
import com.sidharthify.breathe.ui.glass
import com.sidharthify.breathe.util.FrameWatch
import com.sidharthify.breathe.util.GlassMode
import com.sidharthify.breathe.util.GlassSupport
import com.sidharthify.breathe.util.glassSupport
import com.sidharthify.breathe.util.isPowerSaving
import kotlinx.coroutines.delay
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.ui.components.MorphingPill
import com.sidharthify.breathe.ui.expressiveClickable
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
        // tile servers throttle the generic osmdroid agent, so say who we are
        Configuration.getInstance().userAgentValue = packageName

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
    // only poll while someone can actually see the numbers
    LifecycleStartEffect(viewModel) {
        viewModel.onForeground()
        onStopOrDispose {
            viewModel.onBackground()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val useNaqi by viewModel.useNaqi.collectAsState()
    val rawMode by viewModel.rawMode.collectAsState()
    val rawHintHidden by viewModel.rawHintHidden.collectAsState()
    val glassMode by viewModel.glassMode.collectAsState()
    val glassAutoOff by viewModel.glassAutoOff.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val glassSupport = remember { glassSupport(context) }
    val powerSaving = remember { isPowerSaving(context) }
    val glassOn =
        when (glassMode) {
            GlassMode.ON -> true
            GlassMode.OFF -> false
            GlassMode.AUTO -> glassSupport == GlassSupport.CAPABLE && !glassAutoOff && !powerSaving && animationSettings.animationsEnabled
        }
    val backdrop = rememberLayerBackdrop()

    LaunchedEffect(glassOn, glassMode) {
        if (!glassOn || glassMode != GlassMode.AUTO) {
            return@LaunchedEffect
        }
        delay(3000)
        val rate = view.display?.refreshRate ?: 60f
        FrameWatch(rate, 15_000L) { ratio -> viewModel.reportGlassFrames(ratio) }.start()
    }

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    val motionScheme = MaterialTheme.motionScheme

    // landscape tablets get the toolbar as a rail on the right, everything else
    // gets the floating pill at the bottom
    val configuration = LocalConfiguration.current
    val useNavRail =
        configuration.smallestScreenWidthDp >= 600 &&
            configuration.screenWidthDp > configuration.screenHeightDp

    // how much room screens leave at the bottom (and the end, with the rail) so
    // the last bit of content can scroll out from under the floating toolbar
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

    // back goes to Home first and only leaves the app from there
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
                        .then(
                            if (glassOn) {
                                Modifier.layerBackdrop(backdrop)
                            } else {
                                Modifier
                            },
                        ).windowInsetsPadding(WindowInsets.statusBars)
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
                            LocalGlassEnabled provides glassOn,
                        ) {
                            when (screen) {
                                AppScreen.Home -> {
                                    HomeScreen(
                                        isLoading = uiState.isLoading,
                                        isDarkTheme = isDarkTheme,
                                        isAmoled = isAmoled,
                                        error = uiState.error,
                                        pinnedZones = uiState.pinnedZones,
                                        allAqiData = uiState.allAqiData,
                                        pinnedIds = uiState.pinnedIds,
                                        zones = uiState.zones,
                                        sensorInfos = uiState.sensorInfos,
                                        onGoToExplore = { currentScreen = AppScreen.Explore },
                                        onRetry = { viewModel.refreshData() },
                                    )
                                }

                                AppScreen.Map -> {
                                    MapScreen(
                                        isLeaving = transition.targetState != EnterExitState.Visible,
                                        zones = uiState.zones,
                                        allAqiData = uiState.allAqiData,
                                        pinnedIds = uiState.pinnedIds,
                                        isDarkTheme = isDarkTheme,
                                        useNaqi = useNaqi,
                                        sensorInfos = uiState.sensorInfos,
                                        onPinToggle = { id -> viewModel.togglePin(id) },
                                        rawMode = rawMode,
                                        rawHintHidden = rawHintHidden,
                                        onRawModeChange = { viewModel.setRawMode(it) },
                                        onHideRawHint = { viewModel.hideRawHint() },
                                    )
                                }

                                AppScreen.Explore -> {
                                    ExploreScreen(
                                        isLoading = uiState.isLoading,
                                        isDarkTheme = isDarkTheme,
                                        error = uiState.error,
                                        zones = uiState.zones,
                                        allAqiData = uiState.allAqiData,
                                        districtAqi = uiState.districtAqi,
                                        pinnedIds = uiState.pinnedIds,
                                        useNaqi = useNaqi,
                                        sensorInfos = uiState.sensorInfos,
                                        query = searchQuery,
                                        onSearchChange = viewModel::onSearchQueryChanged,
                                        onPinToggle = { id -> viewModel.togglePin(id) },
                                        onRetry = { viewModel.refreshData() },
                                        rawMode = rawMode,
                                        rawHintHidden = rawHintHidden,
                                        onRawModeChange = { viewModel.setRawMode(it) },
                                        onHideRawHint = { viewModel.hideRawHint() },
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

            val navBar: @Composable (Modifier) -> Unit = { barModifier ->
                LiquidNavBar(
                    screens = AppScreen.entries,
                    current = currentScreen,
                    animationSettings = animationSettings,
                    vertical = useNavRail,
                    onSelect = { currentScreen = it },
                    modifier = barModifier,
                )
            }

            if (useNavRail) {
                val railModifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(end = 24.dp)
                        .width(72.dp)
                        .zIndex(1f)
                if (glassOn) {
                    Box(
                        modifier =
                            railModifier
                                .glass(backdrop, RoundedCornerShape(100))
                                .padding(8.dp),
                    ) {
                        navBar(Modifier.fillMaxWidth().height(72.dp * AppScreen.entries.size))
                    }
                } else {
                    VerticalFloatingToolbar(
                        expanded = true,
                        modifier = railModifier,
                        shape = RoundedCornerShape(100),
                        contentPadding = PaddingValues(8.dp),
                        expandedShadowElevation = 12.dp,
                    ) {
                        navBar(Modifier.fillMaxWidth().height(72.dp * AppScreen.entries.size))
                    }
                }
            } else {
                val pillModifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 24.dp, start = 48.dp, end = 48.dp)
                        .height(72.dp)
                        .zIndex(1f)
                if (glassOn) {
                    Box(
                        modifier =
                            pillModifier
                                .glass(backdrop, RoundedCornerShape(100))
                                .padding(8.dp),
                    ) {
                        navBar(Modifier.fillMaxSize())
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
                        modifier = pillModifier,
                        shape = RoundedCornerShape(100),
                        contentPadding = PaddingValues(8.dp),
                        expandedShadowElevation = 12.dp,
                    ) {
                        navBar(Modifier.weight(1f).fillMaxHeight())
                    }
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

    // amoled keeps the dynamic accent colours and only swaps the backgrounds
    // for true black, plus two near blacks so cards still separate
    val colorScheme =
        if (darkTheme && amoledMode) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainer = Color(0xFF000000),
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
