// SPDX-License-Identifier: MIT
/*
 * RawReadings.kt - Switch, banner and explainer for raw sensor readings
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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sidharthify.breathe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.util.RAW_EXPLAINER_POINTS

@Composable
fun RawExplainerPoints(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RAW_EXPLAINER_POINTS.forEach { point ->
            Column {
                Text(
                    point.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    point.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
fun RawModeSwitch(
    rawMode: Boolean,
    rawAvailable: Boolean,
    onRawModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        ToggleButton(
            checked = !rawMode,
            onCheckedChange = { checked ->
                if (checked) {
                    onRawModeChange(false)
                }
            },
            modifier = Modifier.weight(1f).height(40.dp),
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
        ) {
            Text("Calibrated", style = MaterialTheme.typography.labelLarge)
        }
        ToggleButton(
            checked = rawMode,
            enabled = rawAvailable || rawMode,
            onCheckedChange = { checked ->
                if (checked) {
                    onRawModeChange(true)
                }
            },
            modifier = Modifier.weight(1f).height(40.dp),
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
        ) {
            Text("Raw", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun RawBanner(
    message: String,
    showPoints: Boolean,
    onHide: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Raw readings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (showPoints && expanded) {
                RawExplainerPoints(modifier = Modifier.padding(top = 12.dp))
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                if (showPoints) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(
                            if (expanded) {
                                "Show less"
                            } else {
                                "What changes"
                            },
                        )
                    }
                }
                if (onHide != null) {
                    TextButton(onClick = onHide) {
                        Text("Don't show again")
                    }
                }
            }
        }
    }
}
