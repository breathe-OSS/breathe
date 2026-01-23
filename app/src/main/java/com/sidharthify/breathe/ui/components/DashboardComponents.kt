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
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.sidharthify.breathe.R
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.LocalAnimationSettings
import com.sidharthify.breathe.expressiveClickable
import com.sidharthify.breathe.util.calculateCigarettes
import com.sidharthify.breathe.util.calculateUsAqi
import com.sidharthify.breathe.util.formatPollutantName
import com.sidharthify.breathe.util.getAqiCategory
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.getTimeAgo
import kotlin.math.ceil

class SoftBurstShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = size.minDimension / 2f
        val polygon =
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                radius = radius,
                innerRadius = radius * 0.7f,
                rounding = CornerRounding(radius * 0.2f),
                centerX = size.width / 2f,
                centerY = size.height / 2f,
            )
        return Outline.Generic(polygon.toPath().asComposePath())
    }
}

class CookieShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = size.minDimension / 2f
        val polygon =
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                radius = radius,
                innerRadius = radius * 0.8f,
                rounding = CornerRounding(radius * 0.4f),
                centerX = size.width / 2f,
                centerY = size.height / 2f,
            )
        return Outline.Generic(polygon.toPath().asComposePath())
    }
}

@Composable
fun MainDashboardDetail(
    zone: AqiResponse,
    provider: String?,
    isDarkTheme: Boolean,
    isUsAqi: Boolean = false,
) {
    val pm25 =
        zone.concentrations?.get("pm2.5")
            ?: zone.concentrations?.get("pm2_5")
            ?: 0.0

    val displayAqi =
        if (!isUsAqi) {
            zone.usAqi ?: if (pm25 > 0) calculateUsAqi(pm25) else 0
        } else {
            zone.nAqi
        }

    val aqiLabel = if (!isUsAqi) "US AQI" else "NAQI"
    val cigarettes = if (pm25 > 0) calculateCigarettes(pm25) else 0.0

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    // If screen is narrow (<390dp), reduce font to prevent line wrap
    val aqiFontSize = if (screenWidth < 390) 64.sp else 84.sp
    val aqiLineHeight = if (screenWidth < 390) 64.sp else 84.sp

    val animationSettings = LocalAnimationSettings.current

    val aqiColor by animateColorAsState(
        targetValue = getAqiColor(displayAqi, !isUsAqi),
        animationSpec = if (animationSettings.colorTransitions) {
            tween(durationMillis = 600, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "DashboardColor",
    )

    val animatedAqi by animateIntAsState(
        targetValue = displayAqi,
        animationSpec = if (animationSettings.numberAnimations) {
            tween(durationMillis = 800, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "DashboardNumber",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animationSettings.pulseEffects) 1.02f else 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breatheScale",
    )

    val aqiBgColor = aqiColor.copy(alpha = 0.12f)
    val uriHandler = LocalUriHandler.current

    val isOpenMeteo =
        provider?.contains("Open-Meteo", ignoreCase = true) == true ||
            provider?.contains("OpenMeteo", ignoreCase = true) == true
    val isAirGradient =
        provider?.contains("AirGradient", ignoreCase = true) == true ||
            provider?.contains("AirGradient", ignoreCase = true) == true

    val softBurstShape = remember { SoftBurstShape() }

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(100),
                    modifier =
                        Modifier
                            .padding(bottom = 12.dp)
                            .expressiveClickable {},
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Now Viewing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = zone.zoneName,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 40.sp,
                )

                if (isAirGradient) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF4CAF50), CircleShape),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Live Ground Sensors",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (isOpenMeteo) {
                    Text(
                        "Satellite & Model Data",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isAirGradient) {
                    Image(
                        painter = painterResource(id = R.drawable.air_gradient_logo),
                        contentDescription = "AirGradient",
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .height(28.dp)
                                .expressiveClickable { uriHandler.openUri("https://www.airgradient.com/") },
                        alpha = 0.9f,
                    )
                }

                if (isOpenMeteo) {
                    val logoRes = if (isDarkTheme) R.drawable.open_meteo_logo else R.drawable.open_meteo_logo_light
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "OpenMeteo",
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .height(28.dp)
                                .expressiveClickable { uriHandler.openUri("https://open-meteo.com/") },
                        alpha = 0.9f,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!zone.warning.isNullOrBlank()) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    ),
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = zone.warning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 20.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .expressiveClickable {},
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = aqiBgColor,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = breatheScale
                            scaleY = breatheScale
                        },
            ) {}

            // Content
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "$animatedAqi",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = aqiFontSize),
                        fontWeight = FontWeight.Black,
                        color = aqiColor,
                        letterSpacing = (-3).sp,
                        lineHeight = aqiLineHeight,
                    )
                    Surface(
                        color = aqiColor,
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            text = aqiLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        "Primary",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        zone.mainPollutant.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
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
                                text = "${if (isWorse) "+" else ""}$change1h /hr",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val statusText =
                        when {
                            zone.timestampUnix != null -> getTimeAgo(zone.timestampUnix.toLong())
                            else -> "Live"
                        }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AQI Category Card with Spectrum
        val aqiCategory = getAqiCategory(displayAqi, !isUsAqi)
        
        // Calculate position on spectrum (0-500 for US, 0-500 for NAQI)
        val maxAqi = if (!isUsAqi) 500f else 500f
        val targetIndicatorPosition = (displayAqi.coerceIn(0, 500) / maxAqi).coerceIn(0f, 1f)
        
        // Animate indicator position
        val animatedIndicatorPosition by animateFloatAsState(
            targetValue = targetIndicatorPosition,
            animationSpec = if (animationSettings.numberAnimations) {
                tween(durationMillis = 800, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 0)
            },
            label = "IndicatorPosition",
        )
        
        val spectrumColorStops = if (!isUsAqi) {
            // US AQI breakpoints
            arrayOf(
                0.00f to Color(0xFF00E400),
                0.10f to Color(0xFF00E400),
                0.10f to Color(0xFFFFFF00),
                0.20f to Color(0xFFFFFF00),
                0.20f to Color(0xFFFF7E00),
                0.30f to Color(0xFFFF7E00),
                0.30f to Color(0xFFFF0000),
                0.40f to Color(0xFFFF0000),
                0.40f to Color(0xFF8F3F97),
                0.60f to Color(0xFF8F3F97),
                0.60f to Color(0xFF7E0023),
                1.00f to Color(0xFF7E0023)
            )
        } else {
            // NAQI breakpoints
            arrayOf(
                0.00f to Color(0xFF55A84F),
                0.10f to Color(0xFF55A84F),
                0.10f to Color(0xFFA3C853),
                0.20f to Color(0xFFA3C853),
                0.20f to Color(0xFFFDD74B),
                0.40f to Color(0xFFFDD74B),
                0.40f to Color(0xFFFB9A34),
                0.60f to Color(0xFFFB9A34),
                0.60f to Color(0xFFE93F33),
                0.80f to Color(0xFFE93F33),
                0.80f to Color(0xFFAF2D24),
                1.00f to Color(0xFFAF2D24)
            )
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            shape = MaterialTheme.shapes.medium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .expressiveClickable {},
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = aqiCategory.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = aqiColor,
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Spectrum meter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                ) {
                    // Gradient bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(colorStops = spectrumColorStops)
                            ),
                    )
                    
                    // Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedIndicatorPosition.coerceAtLeast(0.01f))
                                .align(Alignment.CenterStart),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            // Circle indicator
                            Surface(
                                modifier = Modifier.size(16.dp),
                                shape = CircleShape,
                                color = aqiColor,
                                shadowElevation = 2.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.surface
                                ),
                            ) {}
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cigarette Equivalence Card
        if (cigarettes > 0.1) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .expressiveClickable {},
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = softBurstShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.SmokingRooms,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "≈ $cigarettes cigarettes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Equivalent PM2.5 inhalation today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        val pollutants = zone.concentrations ?: emptyMap()
        if (pollutants.isNotEmpty()) {
            Text(
                "Concentrations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            SimpleFlowGrid(
                items = pollutants.entries.toList(),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!zone.history.isNullOrEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                AqiHistoryGraph(
                    history = zone.history,
                    isUsAqi = isUsAqi,
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SimpleFlowGrid(
    items: List<Map.Entry<String, Double>>,
    modifier: Modifier = Modifier,
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
fun PollutantChip(
    key: String,
    value: Double,
    modifier: Modifier = Modifier,
) {
    val unit = if (key.lowercase() == "co") "mg/m³" else "µg/m³"

    Box(
        modifier =
            modifier
                .height(80.dp)
                .expressiveClickable {},
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        formatPollutantName(key),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$value",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
