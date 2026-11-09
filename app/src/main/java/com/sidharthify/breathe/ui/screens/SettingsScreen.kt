// SPDX-License-Identifier: MIT
/*
 * SettingsScreen.kt - Appearance, AQI standard, animation toggles, credits and the update check
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
 * Copyright (C) 2026 FlashWreck <theghost3370@gmail.com>
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

package com.sidharthify.breathe.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.data.AnimationSettings
import com.sidharthify.breathe.expressiveClickable
import com.sidharthify.breathe.navigation.LocalNavBarClearance
import com.sidharthify.breathe.viewmodel.BreatheViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration.Companion.seconds

private const val RELEASES_URL = "https://github.com/breathe-OSS/breathe/releases/"
private const val LATEST_RELEASE_API = "https://api.github.com/repos/breathe-OSS/breathe/releases/latest"

// matches the tags on github, versionName 4.0 + versionCode 21 is v4.0-21
private fun currentVersionTag(context: Context): String {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    return "v${info.versionName}-$code"
}

private sealed interface UpdateCheck {
    data class Done(val latestTag: String) : UpdateCheck

    data object RateLimited : UpdateCheck

    data object Failed : UpdateCheck
}

private suspend fun fetchLatestReleaseTag(): UpdateCheck =
    withContext(Dispatchers.IO) {
        try {
            val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Breathe-App")
            try {
                if (connection.responseCode != 200) {
                    return@withContext UpdateCheck.RateLimited
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                UpdateCheck.Done(JSONObject(body).getString("tag_name"))
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            UpdateCheck.Failed
        }
    }

private fun String.withoutV(): String = removePrefix("v").removePrefix("V").trim()

// seven taps on the version starts it, it wobbles for 20 seconds and then
// settles. the transition only exists while it's running, so the settings
// screen isn't animating six values every frame for nothing
private class SydMadness(
    val hue: State<Float>,
    val rotation: State<Float>,
    val scale: State<Float>,
    val drift: State<Float>,
    val vertical: State<Float>,
    val skew: State<Float>,
)

@Composable
private fun InfiniteTransition.wave(
    from: Float,
    to: Float,
    millis: Int,
    easing: Easing = FastOutSlowInEasing,
    repeatMode: RepeatMode = RepeatMode.Reverse,
): State<Float> =
    animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(tween(millis, easing = easing), repeatMode),
        label = "madness",
    )

@Composable
private fun rememberSydMadness(): SydMadness {
    val transition = rememberInfiniteTransition(label = "madness")
    return SydMadness(
        hue = transition.wave(0f, 360f, 3000, LinearEasing, RepeatMode.Restart),
        rotation = transition.wave(-8f, 8f, 1500),
        scale = transition.wave(0.92f, 1.08f, 2000),
        drift = transition.wave(-25f, 25f, 1800),
        vertical = transition.wave(-10f, 10f, 2200),
        skew = transition.wave(-5f, 5f, 2800, LinearEasing),
    )
}

private fun Modifier.madness(madness: SydMadness?): Modifier {
    if (madness == null) {
        return this
    }
    return graphicsLayer {
        val scale = 1f + (madness.scale.value - 1f)
        scaleX = scale
        scaleY = scale
        rotationZ = madness.rotation.value * 1.4f
        translationX = madness.drift.value * 1.2f
        translationY = madness.vertical.value * 0.8f
        rotationX = madness.skew.value * 0.6f
    }
}

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    onThemeToggle: () -> Unit,
    onAmoledToggle: () -> Unit,
    animationSettings: AnimationSettings,
    onAnimationSettingsChange: (AnimationSettings) -> Unit,
    viewModel: BreatheViewModel = viewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val useNaqi by viewModel.useNaqi.collectAsState()
    val isOverviewHome by viewModel.isOverviewHome.collectAsState()

    val currentVersion = remember { currentVersionTag(context) }
    var easterEggTaps by remember { mutableIntStateOf(0) }
    var isMadness by remember { mutableStateOf(false) }
    var showDataSourceDialog by remember { mutableStateOf(false) }
    var animationsExpanded by rememberSaveable { mutableStateOf(false) }

    val madness =
        if (isMadness) {
            rememberSydMadness()
        } else {
            null
        }

    // taps have to come within two seconds of each other
    LaunchedEffect(easterEggTaps) {
        if (easterEggTaps > 0 && !isMadness) {
            delay(2.seconds)
            easterEggTaps = 0
        }
    }

    fun onVersionTapped() {
        if (isMadness) {
            return
        }
        easterEggTaps++
        if (easterEggTaps < 7) {
            return
        }
        easterEggTaps = 0
        isMadness = true
        Toast.makeText(context, "Shine on, you crazy diamond!", Toast.LENGTH_LONG).show()
        scope.launch {
            delay(20.seconds)
            isMadness = false
        }
    }

    fun checkForUpdates() {
        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
        scope.launch {
            val message =
                when (val result = fetchLatestReleaseTag()) {
                    is UpdateCheck.Done -> {
                        if (result.latestTag.withoutV() != currentVersion.withoutV()) {
                            "Update available: ${result.latestTag}"
                        } else {
                            "You are on the latest version (${result.latestTag})"
                        }
                    }
                    UpdateCheck.RateLimited -> "GitHub API Rate Limited. Try later."
                    UpdateCheck.Failed -> "Failed to check updates"
                }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    if (showDataSourceDialog) {
        DataSourcesDialog(
            onOpen = { uriHandler.openUri(it) },
            onDismiss = { showDataSourceDialog = false },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth()
                .widthIn(max = 840.dp)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = LocalNavBarClearance.current)
                .madness(madness),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color =
                if (madness != null) {
                    Color.hsl(madness.hue.value, 0.7f, 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )

        SettingsGroup(title = "Appearance", isAmoled = isAmoled) {
            SettingsSwitch(
                title = "Dark Theme",
                subtitle = "Toggle app appearance",
                checked = isDarkTheme,
                onCheckedChange = { onThemeToggle() },
            )
            AnimatedVisibility(
                visible = isDarkTheme,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SettingsSwitch(
                    title = "AMOLED Mode",
                    subtitle = "Pure black background",
                    checked = isAmoled,
                    onCheckedChange = { onAmoledToggle() },
                )
            }
        }

        SettingsGroup(title = "General", isAmoled = isAmoled) {
            SettingsSwitch(
                title = "Indian NAQI",
                subtitle = "Switch to Indian National AQI",
                checked = useNaqi,
                onCheckedChange = { viewModel.toggleAqiStandard() },
            )
            SettingsSwitch(
                title = "Pinned Overview",
                subtitle = "Show pinned locations as cards on Home",
                checked = isOverviewHome,
                onCheckedChange = { viewModel.toggleOverviewHome() },
            )
            SettingsItem(
                "Data Standards",
                if (useNaqi) {
                    "Indian National Air Quality Index (NAQI)"
                } else {
                    "US EPA (2024 Standard)"
                },
            )
            SettingsItem(
                "Data Sources",
                "OpenMeteo & AirGradient ground sensors",
                onClick = { showDataSourceDialog = true },
            )
            SettingsItem("Breathe OSS", "View Source on GitHub", onClick = {
                uriHandler.openUri("https://github.com/breathe-OSS/breathe")
            })
            SettingsItem("About Breathe", "View our philosophy and vision", onClick = {
                uriHandler.openUri("https://about.breatheoss.app")
            })
        }

        SettingsGroup(title = "Performance", isAmoled = isAmoled) {
            SettingsSwitch(
                title = "Animations",
                subtitle = "Enable all animations",
                checked = animationSettings.animationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        onAnimationSettingsChange(AnimationSettings())
                    } else {
                        onAnimationSettingsChange(AnimationSettings.Disabled)
                    }
                },
            )
            SettingsRow(onClick = { animationsExpanded = !animationsExpanded }) {
                SettingsText(
                    "Advanced Animation Settings",
                    if (animationsExpanded) {
                        "Tap to collapse"
                    } else {
                        "Fine-tune individual animations"
                    },
                    Modifier.weight(1f),
                )
                Icon(
                    if (animationsExpanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = animationsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                AnimationToggles(animationSettings, onAnimationSettingsChange)
            }
        }

        SettingsGroup(title = "We, the People of Breathe", isAmoled = isAmoled) {
            TEAM.forEach { member ->
                SettingsItem(member.name, "${member.role} (@${member.github})", onClick = {
                    uriHandler.openUri("https://github.com/${member.github}")
                })
            }
        }

        SettingsGroup(title = "Updates (from GitHub)", isAmoled = isAmoled) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .settingsItemBackground(isAmoled)
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // no ripple, it's meant to be found
                Text(
                    text =
                        if (madness != null) {
                            "For Syd (1946-2006)"
                        } else {
                            "Current Version: $currentVersion"
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (madness != null) {
                            Color.hsl(madness.hue.value, 0.8f, 0.65f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    fontWeight =
                        if (madness != null) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onVersionTapped() },
                        ),
                )
                FilledTonalButton(onClick = { checkForUpdates() }) {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check for Updates")
                }
                TextButton(onClick = { uriHandler.openUri(RELEASES_URL) }) {
                    Text("View all releases")
                }
            }
        }
    }
}

private data class TeamMember(
    val name: String,
    val role: String,
    val github: String,
)

private val TEAM =
    listOf(
        TeamMember("Sidharth \"Siddhi\" Sharma", "Lead Developer", "sidharthify"),
        TeamMember("Aaditya Gupta", "Developer", "Flashwreck"),
        TeamMember("Sannidhya Sharma", "Developer", "SleeperOfSaturn"),
        TeamMember("Aditya Choudhary", "Contributor", "empirea9"),
        TeamMember("Veer P.S Singh", "Contributor", "Lostless1907"),
        TeamMember("Suvesh Moza", "Contributor", "suveshmoza"),
    )

@Composable
private fun AnimationToggles(
    settings: AnimationSettings,
    onChange: (AnimationSettings) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SegmentGap)) {
        SettingsSwitch(
            title = "Screen Transitions",
            subtitle = "Slide animations between screens",
            checked = settings.screenTransitions,
            onCheckedChange = { onChange(settings.copy(screenTransitions = it)) },
        )
        SettingsSwitch(
            title = "Color Transitions",
            subtitle = "Smooth AQI color changes",
            checked = settings.colorTransitions,
            onCheckedChange = { onChange(settings.copy(colorTransitions = it)) },
        )
        SettingsSwitch(
            title = "Number Animations",
            subtitle = "Animated AQI value counting",
            checked = settings.numberAnimations,
            onCheckedChange = { onChange(settings.copy(numberAnimations = it)) },
        )
        SettingsSwitch(
            title = "Pulse Effects",
            subtitle = "Breathing animation on cards",
            checked = settings.pulseEffects,
            onCheckedChange = { onChange(settings.copy(pulseEffects = it)) },
        )
        SettingsSwitch(
            title = "Morphing Navigation",
            subtitle = "Shape morphing in nav bar",
            checked = settings.morphingPill,
            onCheckedChange = { onChange(settings.copy(morphingPill = it)) },
        )
        SettingsSwitch(
            title = "Press Feedback",
            subtitle = "Squish effect on tap",
            checked = settings.pressFeedback,
            onCheckedChange = { onChange(settings.copy(pressFeedback = it)) },
        )
        SettingsSwitch(
            title = "List Animations",
            subtitle = "Animated list items in Explore",
            checked = settings.listAnimations,
            onCheckedChange = { onChange(settings.copy(listAnimations = it)) },
        )
    }
}

@Composable
private fun DataSourcesDialog(
    onOpen: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Data Sources") },
        text = {
            Column {
                Text("Non AirGradient enabled zones", fontWeight = FontWeight.Bold)
                Text("Air quality pollutants data sourced from Open-Meteo.")
                TextButton(onClick = { onOpen("https://open-meteo.com") }) {
                    Text("open-meteo.com")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("AirGradient enabled zones", fontWeight = FontWeight.Bold)
                Text("PM10 and PM2.5 sourced from AirGradient ground sensor, and others from Open-Meteo")
                Row {
                    TextButton(onClick = { onOpen("https://www.airgradient.com") }) {
                        Text("airgradient.com")
                    }
                    TextButton(onClick = { onOpen("https://open-meteo.com") }) {
                        Text("open-meteo.com")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

// md3 expressive segmented list. the group clips to the big outer corners
// and every row inside gets small corners with a thin gap between them,
// same as the android 16 settings app
private val SegmentGap = 2.dp
private val SegmentCorner = RoundedCornerShape(4.dp)

// amoled turns surfaceContainer pure black, rows would vanish into the background
@Composable
private fun Modifier.settingsItemBackground(isAmoled: Boolean): Modifier {
    val color =
        if (isAmoled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    return clip(SegmentCorner).background(color)
}

private val LocalSettingsAmoled = compositionLocalOf { false }

@Composable
fun SettingsGroup(
    title: String,
    isAmoled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        )
        CompositionLocalProvider(LocalSettingsAmoled provides isAmoled) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large),
                verticalArrangement = Arrangement.spacedBy(SegmentGap),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    onClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    val base =
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .settingsItemBackground(LocalSettingsAmoled.current)
    val clickable =
        if (onClick != null) {
            base.expressiveClickable(onClick)
        } else {
            base
        }
    Row(
        modifier = clickable.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SettingsText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    SettingsRow(onClick = onClick) {
        SettingsText(title, subtitle, Modifier.weight(1f))
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(onClick = { onCheckedChange(!checked) }) {
        SettingsText(title, subtitle, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent =
                if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
        )
    }
}
