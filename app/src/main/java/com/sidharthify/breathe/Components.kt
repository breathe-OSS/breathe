package com.sidharthify.breathe

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PinnedMiniCard(zone: AqiResponse, isSelected: Boolean, onClick: () -> Unit) {
    val aqiColor by animateColorAsState(
        targetValue = getAqiColor(zone.nAqi),
        animationSpec = tween(durationMillis = 500),
        label = "MiniCardColor"
    )

    val containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSelected) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = zone.zoneName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(aqiColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${zone.nAqi}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MainDashboardDetail(zone: AqiResponse, provider: String?) {
    val aqiColor by animateColorAsState(
        targetValue = getAqiColor(zone.nAqi),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "DashboardColor"
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
                Image(
                    painter = painterResource(id = R.drawable.open_meteo_logo),
                    contentDescription = "Open-Meteo Data",
                    modifier = Modifier
                        .height(24.dp)
                        .padding(start = 8.dp),
                    alpha = 0.8f
                )
            }

            if (isOpenAQ) {
                Image(
                    painter = painterResource(id = R.drawable.open_aq_logo),
                    contentDescription = "Open-AQ Data",
                    modifier = Modifier
                        .height(64.dp)
                        .padding(start = 8.dp),
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

@Composable
fun AqiHistoryGraph(history: List<HistoryPoint>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) return

    val graphColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                "24 Hour Trend",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier.fillMaxWidth().height(150.dp)
            ) {
                val labelWidth = 35.dp.toPx()
                val width = size.width - labelWidth
                val height = size.height

                val maxAqi = history.maxOf { it.aqi }.toFloat().coerceAtLeast(100f)
                val minAqi = history.minOf { it.aqi }.toFloat().coerceAtMost(0f)
                val range = maxAqi - minAqi

                fun getX(index: Int): Float = labelWidth + (index.toFloat() / (history.size - 1)) * width
                fun getY(aqi: Int): Float = height - ((aqi - minAqi) / range * height)

                val path = Path()

                history.forEachIndexed { i, point ->
                    val x = getX(i)
                    val y = getY(point.aqi)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = getX(i - 1)
                        val prevY = getY(history[i - 1].aqi)

                        val controlX = prevX + (x - prevX) / 2
                        path.cubicTo(controlX, prevY, controlX, y, x, y)
                    }
                }

                val fillPath = Path()
                fillPath.addPath(path)
                fillPath.lineTo(size.width, height)
                fillPath.lineTo(labelWidth, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(graphColor.copy(alpha = 0.3f), graphColor.copy(alpha = 0.0f))
                    )
                )

                drawPath(
                    path = path,
                    color = graphColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    val textPaint = Paint().apply {
                        color = labelColor
                        textSize = 30f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    textPaint.textAlign = Paint.Align.LEFT

                    // Max AQI
                    nativeCanvas.drawText(
                        "${maxAqi.toInt()}",
                        0f,
                        30f,
                        textPaint
                    )

                    val midAqi = (maxAqi + minAqi) / 2
                    nativeCanvas.drawText(
                        "${midAqi.toInt()}",
                        0f,
                        height / 2 + 10f,
                        textPaint
                    )

                    // Min AQI
                    nativeCanvas.drawText(
                        "${minAqi.toInt()}",
                        0f,
                        height - 10f,
                        textPaint
                    )

                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.typeface = Typeface.DEFAULT

                    val indicesToLabel = listOf(0, history.size / 2, history.size - 1)
                    indicesToLabel.forEach { i ->
                        if (i < history.size) {
                            val date = Date(history[i].ts * 1000)
                            val label = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)

                            textPaint.textAlign = when(i) {
                                0 -> Paint.Align.LEFT
                                history.size - 1 -> Paint.Align.RIGHT
                                else -> Paint.Align.CENTER
                            }

                            val xPos = getX(i)
                            nativeCanvas.drawText(label, xPos, height + 45f, textPaint)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ErrorCard(msg: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connection Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Text(msg, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyStateCard(onGoToExplore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PushPin, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Pinned Zones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Pin locations to see them here", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(onClick = onGoToExplore) { Text("Go to Explore") }
    }
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ZoneListItem(zone: Zone, isPinned: Boolean, onPinClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(zone.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    zone.provider ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPinClick) {
                Icon(
                    if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if(isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}