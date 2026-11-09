// SPDX-License-Identifier: MIT
/*
 * SensorDetails.kt - Everything we know about one ground sensor, shown in its info sheet
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
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


package com.sidharthify.breathe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.NodeReading
import com.sidharthify.breathe.data.SensorInfo
import com.sidharthify.breathe.util.displayAqi

// a sensor with no pm2.5 is offline. its readings are gone but the sensor
// itself is still there, so the info about it is shown either way with a
// banner on top saying why the numbers are missing
@Composable
fun SensorDetails(
    nodeName: String,
    reading: NodeReading,
    useNaqi: Boolean,
    sensorInfo: SensorInfo?,
    modifier: Modifier = Modifier,
) {
    val isOffline = reading.pm25 == null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                nodeName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (sensorInfo != null) {
                Text(
                    sensorInfo.zone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isOffline) {
            OfflineBanner()
        } else {
            NodeInfoRow("AQI", reading.displayAqi(useNaqi)?.toString() ?: "—")
            NodeInfoRow(
                "AQI Standard",
                if (useNaqi) {
                    "NAQI"
                } else {
                    "US AQI"
                },
            )
            NodeInfoRow("PM2.5", "${reading.pm25} µg/m³")
            NodeInfoRow("PM10", reading.pm10?.let { "$it µg/m³" } ?: "—")
            NodeInfoRow("Temperature", reading.temp?.let { "$it °C" } ?: "—")
            NodeInfoRow("Humidity", reading.humidity?.let { "$it%" } ?: "—")
            AmbientDisclaimer()
        }

        if (sensorInfo != null) {
            NodeInfoRow("Provider", sensorInfo.provider)
            NodeInfoRow("Model", sensorInfo.model)
            NodeInfoRow("Location ID", sensorInfo.locationId.toString())
            NodeInfoRow("Install Date", sensorInfo.installationDate)
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null)
            Column {
                Text(
                    "Sensor offline",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "It hasn't sent a reading lately, so there's nothing to show until it reconnects.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AmbientDisclaimer() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(top = 1.dp),
            )
            Text(
                "The internal sensor measures conditions inside the module to improve PM accuracy, requiring dashboard corrections that may not perfectly match true ambient temperature and humidity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NodeInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
