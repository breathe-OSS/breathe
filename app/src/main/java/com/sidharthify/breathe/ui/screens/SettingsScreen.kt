package com.sidharthify.breathe.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.viewmodel.BreatheViewModel

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: BreatheViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    
    var showDataSourceDialog by remember { mutableStateOf(false) }

    val currentVersion = try {
        // com.sidharthify.breathe.BuildConfig.VERSION_NAME
        "v2.3-6"
    } catch (e: Exception) {
        "Unknown"
    }

    if (showDataSourceDialog) {
        AlertDialog(
            onDismissRequest = { showDataSourceDialog = false },
            title = { Text("Data Sources") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Jammu & Kashmir regions (excl. Srinagar)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Air quality and weather data sourced from Open-Meteo.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "https://open-meteo.com/",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { uriHandler.openUri("https://open-meteo.com/") }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column {
                        Text(
                            text = "Srinagar region",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Air quality data sourced from OpenAQ, using measurements published by the Central Pollution Control Board (CPCB).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "cpcb.gov.in/",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { uriHandler.openUri("https://cpcb.gov.in/") }
                            )
                            Text(
                                text = "openaq.org",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { uriHandler.openUri("https://openaq.org/") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDataSourceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

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

        SettingsItem(
            title = "Data Sources", 
            subtitle = "OpenMeteo & OpenAQ",
            onClick = { showDataSourceDialog = true }
        )

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
            title = "Veer P.S Singh",
            subtitle = "@Lostless1907",
            onClick = { uriHandler.openUri("https://github.com/Lostless1907") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Updates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Version: $currentVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.checkForUpdates(context, currentVersion)
                    }
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check for Updates")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "View all releases",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/breathe-OSS/breathe/releases/")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
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
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}