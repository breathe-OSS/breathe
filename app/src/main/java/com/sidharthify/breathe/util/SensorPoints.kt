// SPDX-License-Identifier: MIT
/*
 * SensorPoints.kt - Puts each ground sensor on the map using its reading and its coordinates
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

import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.NodeReading
import com.sidharthify.breathe.data.SensorInfo

data class SensorPoint(
    val zoneId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val reading: NodeReading,
    val info: SensorInfo,
)

/**
 * Readings come from each zone's nodes map and coordinates from
 * /sensor-info, matched by sensor name. Names are only unique inside a
 * zone, so the zone name is tried first and a bare name match is the
 * fallback. A sensor with no coordinates can't go on the map and is left out.
 */
fun buildSensorPoints(
    zones: List<AqiResponse>,
    sensorInfos: List<SensorInfo>,
): List<SensorPoint> {
    val byZoneAndName = sensorInfos.associateBy { it.zone to it.name }
    val byName = sensorInfos.groupBy { it.name }

    return zones.flatMap { zone ->
        zone.nodes.orEmpty().mapNotNull { (name, reading) ->
            val info =
                byZoneAndName[zone.zoneName to name]
                    ?: byName[name]?.singleOrNull()
                    ?: return@mapNotNull null
            SensorPoint(
                zoneId = zone.zoneId,
                name = name,
                latitude = info.latitude,
                longitude = info.longitude,
                reading = reading,
                info = info,
            )
        }
    }
}
