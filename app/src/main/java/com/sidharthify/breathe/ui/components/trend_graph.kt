package com.sidharthify.breathe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.DailyAqi
import com.sidharthify.breathe.util.computeTrendLabel
import com.sidharthify.breathe.util.smoothTrend

@Composable
fun AqiTrendGraph(daily: List<DailyAqi>) {
    if (daily.isEmpty()) return

    val raw = daily.map { it.aqi }
    val smoothed = smoothTrend(raw)

    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = "7-day air quality trend",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val points = smoothed.mapIndexedNotNull { i, v ->
                v?.let { Offset(i.toFloat(), it) }
            }

            if (points.size < 2) return@Canvas

            val maxY = points.maxOf { it.y }
            val minY = points.minOf { it.y }
            val range = (maxY - minY).takeIf { it != 0f } ?: 1f

            fun scale(p: Offset): Offset {
                val x = p.x / 6f * size.width
                val y = size.height - ((p.y - minY) / range) * size.height
                return Offset(x, y)
            }

            for (i in 0 until points.lastIndex) {
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = scale(points[i]),
                    end = scale(points[i + 1]),
                    strokeWidth = 4f
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = computeTrendLabel(raw),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
