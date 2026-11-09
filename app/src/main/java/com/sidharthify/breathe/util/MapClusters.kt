// SPDX-License-Identifier: MIT
/*
 * MapClusters.kt - Groups map bubbles that would sit on top of each other at the current zoom
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


package com.sidharthify.breathe.util

import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

data class MapCluster<T>(
    val members: List<T>,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Zoomed out, a dozen Srinagar and Jammu sensors land in the same few
 * pixels and turn into a pile of half hidden numbers. This walks the items
 * from highest priority down and drops each one into the first cluster
 * whose seed is closer than minDistancePx on screen, or starts a new one.
 *
 * Distances are worked out on the web mercator plane osmdroid draws on
 * (256px tiles, not scaled for dpi), so they match the screen without
 * needing the map's projection and don't change when the map is panned.
 */
fun <T> clusterForZoom(
    items: List<T>,
    zoom: Double,
    minDistancePx: Float,
    latitude: (T) -> Double,
    longitude: (T) -> Double,
    priority: (T) -> Int,
): List<MapCluster<T>> {
    val worldPx = 256.0 * 2.0.pow(zoom)

    fun toPx(item: T): Pair<Double, Double> {
        val x = (longitude(item) + 180.0) / 360.0 * worldPx
        val latRad = Math.toRadians(latitude(item))
        val y = (1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / PI) / 2.0 * worldPx
        return x to y
    }

    val seeds = mutableListOf<Pair<Double, Double>>()
    val groups = mutableListOf<MutableList<T>>()

    items.sortedByDescending(priority).forEach { item ->
        val (x, y) = toPx(item)
        val index = seeds.indexOfFirst { (sx, sy) -> hypot(sx - x, sy - y) < minDistancePx }
        if (index >= 0) {
            groups[index] += item
        } else {
            seeds += x to y
            groups += mutableListOf(item)
        }
    }

    return groups.map { members ->
        MapCluster(
            members = members,
            latitude = members.map(latitude).average(),
            longitude = members.map(longitude).average(),
        )
    }
}
