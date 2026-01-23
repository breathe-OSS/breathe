package com.sidharthify.breathe.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.data.AnimationSettings
import com.sidharthify.breathe.expressiveClickable
import com.sidharthify.breathe.viewmodel.BreatheViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

    var easterEggCounter by remember { mutableIntStateOf(0) }
    var isMadness by remember { mutableStateOf(false) }
    var madnessIntensity by remember { mutableIntStateOf(0) }
    val isUsAqi by viewModel.isUsAqi.collectAsState()
    var versionLabel by remember { mutableStateOf("Current Version: v3.1-14") }

    val currentVersion = "v3.1-14"

    // Reset tap counter if inactive
    LaunchedEffect(easterEggCounter) {
        if (easterEggCounter > 0 && !isMadness) {
            delay(2000)
            easterEggCounter = 0
        }
    }

    // Remember when you were young?
    val infiniteTransition = rememberInfiniteTransition(label = "madness")

    // Hue rotation for psychedelic colors
    val hueRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hueRotation",
    )

    // You shone like the sun.
    val madnessRotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "rotation",
    )

    // Shine on, you crazy diamond!
    val madnessScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )

    // Now there's a look in your eyes, like black holes in the sky.
    val madnessDrift by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "drift",
    )

    // Vertical wobble
    val madnessVertical by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "vertical",
    )

    // Skew effect
    val madnessSkew by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "skew",
    )

    var showDataSourceDialog by remember { mutableStateOf(false) }

    fun checkForUpdates() {
        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/breathe-OSS/breathe/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "Breathe-App")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestTag = json.getString("tag_name")

                    val cleanCurrent = currentVersion.replace("v", "", ignoreCase = true).trim()
                    val cleanLatest = latestTag.replace("v", "", ignoreCase = true).trim()

                    withContext(Dispatchers.Main) {
                        if (cleanLatest != cleanCurrent) {
                            Toast.makeText(context, "Update available: $latestTag", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "You are on the latest version ($latestTag)", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "GitHub API Rate Limited. Try later.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to check updates", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showDataSourceDialog) {
        AlertDialog(
            onDismissRequest = { showDataSourceDialog = false },
            title = { Text("Data Sources") },
            text = {
                Column {
                    Text("Jammu & Kashmir regions (excl. Srinagar and Jammu)", fontWeight = FontWeight.Bold)
                    Text("Air quality pollutants data sourced from Open-Meteo.")
                    Text(
                        "open-meteo.com",
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier.expressiveClickable {
                                uriHandler.openUri("https://open-meteo.com")
                            },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Srinagar and Jammu", fontWeight = FontWeight.Bold)
                    Text("PM10 and PM2.5 sourced from AirGradient ground sensor, and others from Open-Meteo")
                    Row {
                        Text(
                            "airgradient.com",
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.expressiveClickable {
                                    uriHandler.openUri("https://www.airgradient.com")
                                },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "open-meteo.com",
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.expressiveClickable {
                                    uriHandler.openUri("https://open-meteo.com")
                                },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDataSourceDialog = false }) { Text("Close") } },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 100.dp)
                // Shine on you crazy diamond!
                .graphicsLayer {
                    if (isMadness) {
                        val intensity = madnessIntensity.coerceIn(1, 3)
                        scaleX = 1f + (madnessScale - 1f) * intensity * 0.5f
                        scaleY = 1f + (madnessScale - 1f) * intensity * 0.5f
                        rotationZ = madnessRotation * intensity * 0.7f
                        translationX = madnessDrift * intensity * 0.6f
                        translationY = madnessVertical * intensity * 0.4f
                        rotationX = madnessSkew * intensity * 0.3f
                    }
                },
    ) {
        // Rainbow tint overlay when in madness mode
        val textColor = if (isMadness) {
            Color.hsl(hueRotation, 0.7f, 0.6f)
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        
        Text(
            "Settings", 
            style = MaterialTheme.typography.displayMedium, 
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(24.dp))

        // #### appearance #### //
        SettingsGroup(title = "Appearance", isAmoled = isAmoled) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .expressiveClickable { onThemeToggle() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Toggle app appearance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onThemeToggle() },
                    thumbContent =
                        if (isDarkTheme) {
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

            AnimatedVisibility(
                visible = isDarkTheme,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .expressiveClickable { onAmoledToggle() }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AMOLED Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Pure black background",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = isAmoled,
                            onCheckedChange = { onAmoledToggle() },
                            thumbContent =
                                if (isAmoled) {
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
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // #### general #### //
        SettingsGroup(title = "General", isAmoled = isAmoled) {
            SettingsSwitch(
                title = "Indian NAQI",
                subtitle = "Switch to Indian National AQI",
                checked = isUsAqi,
                onCheckedChange = { viewModel.toggleAqiStandard(context) },
                showDivider = true,
            )

            val standardText = if (isUsAqi) "Indian National Air Quality Index (NAQI)" else "US EPA (2024 Standard)"

            SettingsItem("Data Standards", standardText, showDivider = true)
            SettingsItem(
                "Data Sources",
                "OpenMeteo & AirGradient ground sensors",
                onClick = { showDataSourceDialog = true },
                showDivider = true,
            )
            SettingsItem("Breathe OSS", "View Source on GitHub", onClick = {
                uriHandler.openUri("https://github.com/breathe-OSS/breathe")
            }, showDivider = false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // #### performance #### //
        var performanceExpanded by remember { mutableStateOf(false) }

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
                showDivider = true,
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .expressiveClickable { performanceExpanded = !performanceExpanded }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Advanced Animation Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (performanceExpanded) "Tap to collapse" else "Fine-tune individual animations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (performanceExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = performanceExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsSwitch(
                        title = "Screen Transitions",
                        subtitle = "Slide animations between screens",
                        checked = animationSettings.screenTransitions,
                        onCheckedChange = { onAnimationSettingsChange(animationSettings.copy(screenTransitions = it)) },
                        showDivider = true,
                    )

                    SettingsSwitch(
                        title = "Color Transitions",
                        subtitle = "Smooth AQI color changes",
                        checked = animationSettings.colorTransitions,
                        onCheckedChange = { onAnimationSettingsChange(animationSettings.copy(colorTransitions = it)) },
                        showDivider = true,
                    )

                    SettingsSwitch(
                        title = "Number Animations",
                        subtitle = "Animated AQI value counting",
                        checked = animationSettings.numberAnimations,
                        onCheckedChange = { onAnimationSettingsChange(animationSettings.copy(numberAnimations = it)) },
                        showDivider = true,
                    )

                    SettingsSwitch(
                        title = "Pulse Effects",
                        subtitle = "Breathing animation on cards",
                        checked = animationSettings.pulseEffects,
                        onCheckedChange = { onAnimationSettingsChange(animationSettings.copy(pulseEffects = it)) },
                        showDivider = true,
                    )

                    SettingsSwitch(
                        title = "Morphing Navigation",
                        subtitle = "Shape morphing in nav bar",
                        checked = animationSettings.morphingPill,
                        onCheckedChange = { onAnimationSettingsChange(animationSettings.copy(morphingPill = it)) },
                        showDivider = true,
                    )

                    SettingsSwitch(
                        title = "Press Feedback",
                        subtitle = "Squish effect on tap",
                        checked = animationSettings.pressFeedback,
                        onCheckedChange = { onAnimationSettingsChange(animationSettings.copy(pressFeedback = it)) },
                        showDivider = true,
                    )

                    SettingsSwitch(
                        title = "List Animations",
                        subtitle = "Animated list items in Explore",
                        checked = animationSettings.listAnimations,
                        onCheckedChange = { onAnimationSettingsChange(animationSettings.copy(listAnimations = it)) },
                        showDivider = false,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // #### team group #### //
        SettingsGroup(title = "We, the People of Breathe", isAmoled = isAmoled) {
            SettingsItem("Sidharth \"Siddhi\" Sharma", "Lead Developer (@sidharthify)", onClick = {
                uriHandler.openUri("https://github.com/sidharthify")
            }, showDivider = true)
            SettingsItem("Aaditya Gupta", "Developer (@Flashwreck)", onClick = {
                uriHandler.openUri("https://github.com/Flashwreck")
            }, showDivider = true)
            SettingsItem("Veer P.S Singh", "Contributor (@Lostless1907)", onClick = {
                uriHandler.openUri("https://github.com/Lostless1907")
            }, showDivider = false)
            SettingsItem("Suvesh Moza", "Contributor (@suveshmoza)", onClick = {
                uriHandler.openUri("https://github.com/suveshmoza")
            }, showDivider = false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // #### updates #### //
        Text(
            "Updates (from GitHub)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = MaterialTheme.shapes.large,
            border = if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Syd
                Box(
                    modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!isMadness) {
                                easterEggCounter++
                                if (easterEggCounter >= 7) {
                                    easterEggCounter = 0
                                    madnessIntensity = 2

                                    // Tribute
                                    isMadness = true
                                    versionLabel = "For Syd (1946-2006)"
                                    Toast.makeText(context, "Shine on, you crazy diamond!", Toast.LENGTH_LONG).show()

                                    // Calm down after a while
                                    scope.launch {
                                        delay(12000)
                                        versionLabel = "For Syd (1946-2006)"
                                        delay(8000)
                                        isMadness = false
                                        madnessIntensity = 0
                                        versionLabel = "Current Version: $currentVersion"
                                    }
                                }
                            }
                        },
                ) {
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isMadness) Color.hsl(hueRotation, 0.8f, 0.65f) 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isMadness) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.expressiveClickable { checkForUpdates() },
                ) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.CloudSync, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check for Updates")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "View all releases",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.expressiveClickable { uriHandler.openUri("https://github.com/breathe-OSS/breathe/releases/") },
                )
            }
        }
    }
}

// #### helpers #### //

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
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val modifier =
        if (onClick != null) {
            Modifier
                .fillMaxWidth()
                .expressiveClickable { onClick.invoke() }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        }

    Column {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp),
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
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .expressiveClickable { onCheckedChange(!checked) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
