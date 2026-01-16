package com.sidharthify.breathe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidharthify.breathe.R
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.expressiveClickable // Make sure this import works
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.getTimeAgo
import com.sidharthify.breathe.util.formatPollutantName
import kotlin.math.ceil

@Composable
fun MainDashboardDetail(zone: AqiResponse, provider: String?, isDarkTheme: Boolean) {
    val aqiColor by animateColorAsState(
        targetValue = getAqiColor(zone.nAqi),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "DashboardColor",
    )

    val animatedAqi by animateIntAsState(
        targetValue = zone.nAqi,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "DashboardNumber"
    )

    // Breathe! Breathe in the air, don't be afraid to care! (this is kinda ass)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    val aqiBgColor = aqiColor.copy(alpha = 0.12f)
    val uriHandler = LocalUriHandler.current

    val isOpenMeteo = provider?.contains("Open-Meteo", ignoreCase = true) == true ||
            provider?.contains("OpenMeteo", ignoreCase = true) == true
    val isAirGradient = provider?.contains("AirGradient", ignoreCase = true) == true ||
            provider?.contains("AirGradient", ignoreCase = true) == true

    Column(modifier = Modifier.padding(vertical = 12.dp)) {

        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top 
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Location Pill
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(100),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .expressiveClickable {}
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Now Viewing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = zone.zoneName,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp), 
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 40.sp
                )

                if (isOpenMeteo) {
                    Text(
                        "Satellite & Model Data",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (isAirGradient) {
                     Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Live Ground Sensors",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isOpenMeteo || isAirGradient) {
                val logoRes = if (isAirGradient) R.drawable.air_gradient_logo else if (isDarkTheme) R.drawable.open_meteo_logo else R.drawable.open_meteo_logo_light
                val url = if (isAirGradient) "https://www.airgradient.com/" else "https://open-meteo.com/"
                
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "Source",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(28.dp)
                        .expressiveClickable { uriHandler.openUri(url) },
                    alpha = 0.9f
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // --- Hero Card ---
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(220.dp)
                .expressiveClickable {}
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = aqiBgColor,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { 
                        scaleX = breatheScale
                        scaleY = breatheScale
                    }
            ) {} 

            // Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$animatedAqi",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp),
                        fontWeight = FontWeight.Black,
                        color = aqiColor,
                        letterSpacing = (-3).sp,
                        lineHeight = 84.sp
                    )
                    Surface(
                        color = aqiColor,
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            text = "NAQI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Primary",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        zone.mainPollutant.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val change1h = zone.trends?.change1h
                    if (change1h != null && change1h != 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isWorse = change1h > 0
                            val trendColor = if (isWorse) Color(0xFFFF5252) else Color(0xFF4CAF50)
                            val icon = if (isWorse) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                            
                            Icon(icon, null, tint = trendColor, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${if(isWorse) "+" else ""}$change1h /hr",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val statusText = when {
                        zone.timestampUnix != null -> getTimeAgo(zone.timestampUnix.toLong())
                        else -> "Live"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val pollutants = zone.concentrations ?: emptyMap()
        if (pollutants.isNotEmpty()) {
            Text(
                "Concentrations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            SimpleFlowGrid(
                items = pollutants.entries.toList(),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- History Graph ---
        if (!zone.history.isNullOrEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                AqiHistoryGraph(history = zone.history)
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SimpleFlowGrid(
    items: List<Map.Entry<String, Double>>, 
    modifier: Modifier = Modifier
) {
    val rows = ceil(items.size / 2f).toInt()
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val firstIndex = i * 2
                val secondIndex = firstIndex + 1
                
                if (firstIndex < items.size) {
                    val (k1, v1) = items[firstIndex]
                    PollutantChip(k1, v1, Modifier.weight(1f))
                }
                
                if (secondIndex < items.size) {
                    val (k2, v2) = items[secondIndex]
                    PollutantChip(k2, v2, Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PollutantChip(key: String, value: Double, modifier: Modifier = Modifier) {
    val unit = if (key.lowercase() == "co") "mg/m³" else "µg/m³"
    
    // apply expressiveClickable
    Box(
        modifier = modifier
            .height(80.dp)
            .expressiveClickable {}
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        formatPollutantName(key),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$value",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}