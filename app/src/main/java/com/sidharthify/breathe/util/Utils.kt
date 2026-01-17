package com.sidharthify.breathe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

fun createBlobIcon(context: Context, color: Int): Drawable {
    val radius = 32f
    val bitmap = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true }

    paint.color = color
    canvas.drawCircle(radius, radius, radius, paint)

    return BitmapDrawable(context.resources, bitmap)
}

fun formatPollutantName(key: String): String {
    return when(key.lowercase()) {
        "pm2_5", "pm2.5" -> "PM2.5"
        "pm10" -> "PM10"
        "no2" -> "NO₂"
        "so2" -> "SO₂"
        "co" -> "CO"
        "o3" -> "O₃"
        else -> key.uppercase()
    }
}

fun getAqiColor(aqi: Int, isUsStandard: Boolean = false): Color {
    if (isUsStandard) {
        return when (aqi) {
            in 0..50 -> Color(0xFF00E400)
            in 51..100 -> Color(0xFFFFFF00)
            in 101..150 -> Color(0xFFFF7E00)
            in 151..200 -> Color(0xFFFF0000)
            in 201..300 -> Color(0xFF8F3F97)
            else -> Color(0xFF7E0023)
        }
    }

    // NAQI (Indian) Colors
    return when (aqi) {
        in 0..50 -> Color(0xFF55A84F)
        in 51..100 -> Color(0xFFA3C853)
        in 101..200 -> Color(0xFFFDD74B)
        in 201..300 -> Color(0xFFFB9A34)
        in 301..400 -> Color(0xFFE93F33)
        else -> Color(0xFFAF2D24)
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        else -> ">1d ago"
    }
}

fun calculateCigarettes(pm25: Double): Double {
    // 22 µg/m³ ≈ 1 cigarette
    val cigs = pm25 / 22.0
    return (cigs * 10).roundToInt() / 10.0
}

fun calculateUsAqi(pm25: Double): Int {
    // US EPA Breakpoints (2024)
    return when {
        pm25 <= 9.0 -> linearInterp(pm25, 0.0, 9.0, 0, 50)
        pm25 <= 35.4 -> linearInterp(pm25, 9.1, 35.4, 51, 100)
        pm25 <= 55.4 -> linearInterp(pm25, 35.5, 55.4, 101, 150)
        pm25 <= 125.4 -> linearInterp(pm25, 55.5, 125.4, 151, 200)
        pm25 <= 225.4 -> linearInterp(pm25, 125.5, 225.4, 201, 300)
        pm25 <= 325.4 -> linearInterp(pm25, 225.5, 325.4, 301, 400)
        else -> linearInterp(pm25, 325.5, 500.0, 401, 500)
    }
}

private fun linearInterp(c: Double, cLow: Double, cHigh: Double, iLow: Int, iHigh: Int): Int {
    return (( (iHigh - iLow) / (cHigh - cLow) ) * (c - cLow) + iLow).roundToInt()
}