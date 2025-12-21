package com.sidharthify.breathe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidharthify.breathe.R
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.util.getAqiColor
import com.sidharthify.breathe.util.getTimeAgo
import com.sidharthify.breathe.util.formatPollutantName

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

    val aqiBgColor = aqiColor.copy(alpha = 0.15f)

    val isOpenMeteo = provider?.contains("Open-Meteo", ignoreCase = true) == true ||
            provider?.contains("OpenMeteo", ignoreCase = true) == true

    val isOpenAQ = provider?.contains("Open-AQ", ignoreCase = true) == true ||
            provider?.contains("OpenAQ", ignoreCase = true) == true

    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Now Viewing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (isOpenMeteo) {
                val logo = if (isDarkTheme) R.drawable.open_meteo_logo else R.drawable.open_meteo_logo_light
                Image(
                    painter = painterResource(id = logo),
                    contentDescription = "Open-Meteo Data",
                    modifier = Modifier
                        .height(24.dp)
                        .padding(start = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, 
                            indication = null
                         ) { uriHandler.openUri("https://open-meteo.com/") },
                    alpha = 0.8f
                )
            }

            if (isOpenAQ) {
                Image(
                    painter = painterResource(id = R.drawable.open_aq_logo),
                    contentDescription = "Open-AQ Data",
                    modifier = Modifier
                        .height(24.dp)
                        .padding(start = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, 
                            indication = null
                         ) { uriHandler.openUri("https://openaq.org/") },
                    alpha = 0.8f
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            zone.zoneName,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            lineHeight = 40.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = aqiBgColor),
            modifier = Modifier.fillMaxWidth().height(260.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animatedAqi",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 90.sp),
                        fontWeight = FontWeight.Black,
                        color = aqiColor
                    )
                    Surface(
                        color = aqiColor,
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = "NAQI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Primary: ${zone.mainPollutant.uppercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    val change1h = zone.trends?.change1h
                    if (change1h != null && change1h != 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isWorse = change1h > 0
                            val icon = if (isWorse) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                            val trendColor = if (isWorse) Color(0xFFFF5252) else Color(0xFF4CAF50) // Red/Green
                            
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${if(isWorse) "+" else ""}$change1h (1h)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val statusText = when {
                        zone.timestampUnix != null -> "Updated ${getTimeAgo(zone.timestampUnix.toLong())}"
                        !zone.lastUpdateStr.isNullOrEmpty() -> "Updated: ${zone.lastUpdateStr}"
                        else -> "Live"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (!zone.history.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            AqiHistoryGraph(history = zone.history)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Pollutants", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val pollutants = zone.concentrations ?: emptyMap()
        if (pollutants.isEmpty()) Text("No detailed data.") else FlowRowGrid(pollutants)
    }
}

@Composable
fun FlowRowGrid(pollutants: Map<String, Double>) {
    val items = pollutants.entries.toList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (key, value) ->
                    PollutantCard(Modifier.weight(1f), formatPollutantName(key), "$value", if (key.lowercase() == "co") "mg/m³" else "µg/m³")
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PollutantCard(modifier: Modifier, name: String, value: String, unit: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}