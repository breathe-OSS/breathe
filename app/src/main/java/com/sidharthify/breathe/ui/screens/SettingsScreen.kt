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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.viewmodel.BreatheViewModel
import com.sidharthify.breathe.expressiveClickable
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
    viewModel: BreatheViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var easterEggCounter by remember { mutableIntStateOf(0) }
    var isMadness by remember { mutableStateOf(false) }
    val isUsAqi by viewModel.isUsAqi.collectAsState()
    var versionLabel by remember { mutableStateOf("Current Version: v3.0-11") }
    
    val currentVersion = "v3.0-11" 

    // Reset tap counter if inactive
    LaunchedEffect(easterEggCounter) {
        if (easterEggCounter > 0) {
            delay(2000)
            easterEggCounter = 0
        }
    }

    // Remember when you were young?
    val infiniteTransition = rememberInfiniteTransition(label = "madness")
    
    // You shone like the sun.
    val madnessRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    // Shine on, you crazy diamond!
    val madnessScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Now there's a look in your eyes, like black holes in the sky.
    val madnessDrift by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
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
                    Text("open-meteo.com", color = MaterialTheme.colorScheme.primary, modifier = Modifier.expressiveClickable { uriHandler.openUri("https://open-meteo.com") })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Srinagar and Jammu", fontWeight = FontWeight.Bold)
                    Text("PM10 and PM2.5 sourced from AirGradient ground sensor, and others from Open-Meteo")
                    Row {
                        Text("airgradient.com", color = MaterialTheme.colorScheme.primary, modifier = Modifier.expressiveClickable { uriHandler.openUri("https://www.airgradient.com") })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("open-meteo.com", color = MaterialTheme.colorScheme.primary, modifier = Modifier.expressiveClickable { uriHandler.openUri("https://open-meteo.com") })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDataSourceDialog = false }) { Text("Close") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 100.dp)
            // Shine on you crazy diamond!
            .graphicsLayer {
                if (isMadness) {
                    scaleX = madnessScale
                    scaleY = madnessScale
                    rotationZ = madnessRotation
                    translationX = madnessDrift
                }
            }
    ) {
        Text("Settings", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // #### appearance #### //
        SettingsGroup(title = "Appearance", isAmoled = isAmoled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .expressiveClickable { onThemeToggle() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Toggle app appearance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() },thumbContent = if (isDarkTheme) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                })
            }

            AnimatedVisibility(
                visible = isDarkTheme,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .expressiveClickable { onAmoledToggle() }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AMOLED Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Pure black background", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isAmoled, onCheckedChange = { onAmoledToggle() },thumbContent = if (isAmoled) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // #### general #### //
        SettingsGroup(title = "General", isAmoled = isAmoled) {
            SettingsSwitch(
                title = "US AQI Standard",
                subtitle = "Use United States EPA calculation",
                checked = isUsAqi,
                onCheckedChange = { viewModel.toggleAqiStandard(context) },
                showDivider = true
            )

            val standardText = if (isUsAqi) "US EPA (2024 Standard)" else "Indian National Air Quality Index (NAQI)"
            
            SettingsItem("Data Standards", standardText, showDivider = true)
            SettingsItem("Data Sources", "OpenMeteo & AirGradient ground sensors", onClick = { showDataSourceDialog = true }, showDivider = true)
            SettingsItem("Breathe OSS", "View Source on GitHub", onClick = { uriHandler.openUri("https://github.com/breathe-OSS/breathe") }, showDivider = false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // #### team group #### //
        SettingsGroup(title = "We, the People of Breathe", isAmoled = isAmoled) {
            SettingsItem("Sidharth \"Siddhi\" Sharma", "Lead Developer (@sidharthify)", onClick = { uriHandler.openUri("https://github.com/sidharthify") }, showDivider = true)
            SettingsItem("Aaditya Gupta", "Developer (@Flashwreck)", onClick = { uriHandler.openUri("https://github.com/Flashwreck") }, showDivider = true)
            SettingsItem("Veer P.S Singh", "Contributor (@Lostless1907)", onClick = { uriHandler.openUri("https://github.com/Lostless1907") }, showDivider = false)
            SettingsItem("Suvesh Moza", "Contributor (@suveshmoza)", onClick = { uriHandler.openUri("https://github.com/suveshmoza") }, showDivider = false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // #### updates #### //
        Text("Updates", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = MaterialTheme.shapes.large,
            border = if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Syd
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        easterEggCounter++
                        if (easterEggCounter >= 7) {
                            easterEggCounter = 0
                            
                            // Tribute
                            isMadness = true
                            Toast.makeText(context, "Shine on, you crazy diamond!", Toast.LENGTH_LONG).show()
                            
                            // Revert after 10 seconds (enough madness)
                            scope.launch {
                                delay(10000)
                                isMadness = false
                                versionLabel = "Current Version: $currentVersion"
                            }
                        }
                    }
                ) {
                    Text(
                        text = versionLabel, 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }                
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier.expressiveClickable { checkForUpdates() }
                ) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier.expressiveClickable { uriHandler.openUri("https://github.com/breathe-OSS/breathe/releases/") }
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
            modifier = Modifier.fillMaxWidth()
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
    showDivider: Boolean = true
) {
    val modifier = if (onClick != null) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(onClick != null) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
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
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveClickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange,thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            })
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}