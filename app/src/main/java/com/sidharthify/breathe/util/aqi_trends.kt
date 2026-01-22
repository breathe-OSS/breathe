package com.sidharthify.breathe.util

fun smoothTrend(values: List<Int?>): List<Float?> {
    return values.mapIndexed { i, _ ->
        val window = listOfNotNull(
            values.getOrNull(i - 1),
            values.getOrNull(i),
            values.getOrNull(i + 1)
        )
        if (window.isEmpty()) null else window.average().toFloat()
    }
}

fun computeTrendLabel(values: List<Int?>): String {
    val clean = values.filterNotNull()
    if (clean.size < 2) return "Not enough data"

    val delta = clean.last() - clean.first()

    return when {
        delta <= -15 -> "Improving ↓"
        delta >= 15  -> "Worsening ↑"
        else         -> "Stable →"
    }
}
