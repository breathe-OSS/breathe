// SPDX-License-Identifier: MIT
/*
 * LiquidGlass.kt - Liquid glass setting, device capability check and frame watchdog
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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.Choreographer
import java.io.File

enum class GlassMode(val label: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off"),
}

enum class GlassSupport {
    CAPABLE,
    OLD_ANDROID,
    LOW_END,
}

fun glassSupport(context: Context): GlassSupport {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return GlassSupport.OLD_ANDROID
    }
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    if (activityManager.isLowRamDevice) {
        return GlassSupport.LOW_END
    }
    if (Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.S) {
        return GlassSupport.CAPABLE
    }
    val memory = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memory)
    val gigabytes = memory.totalMem / (1024.0 * 1024.0 * 1024.0)
    val cores = Runtime.getRuntime().availableProcessors()
    val topFrequencyGhz = maxCpuFrequencyKhz() / 1_000_000.0
    if (gigabytes >= 5.5 && cores >= 8 && topFrequencyGhz >= 2.4) {
        return GlassSupport.CAPABLE
    }
    return GlassSupport.LOW_END
}

private fun maxCpuFrequencyKhz(): Long {
    val cpus = File("/sys/devices/system/cpu").listFiles { file -> file.name.matches(Regex("cpu\\d+")) } ?: return 0L
    return cpus.maxOfOrNull { cpu ->
        runCatching { File(cpu, "cpufreq/cpuinfo_max_freq").readText().trim().toLong() }.getOrDefault(0L)
    } ?: 0L
}

fun isPowerSaving(context: Context): Boolean {
    val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return power.isPowerSaveMode
}

class FrameWatch(
    private val refreshRate: Float,
    private val durationMillis: Long,
    private val onResult: (jankRatio: Float) -> Unit,
) : Choreographer.FrameCallback {
    private var startNanos = 0L
    private var lastNanos = 0L
    private var frames = 0
    private var janky = 0
    private var running = false

    fun start() {
        if (running) {
            return
        }
        running = true
        startNanos = 0L
        lastNanos = 0L
        frames = 0
        janky = 0
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) {
            return
        }
        if (startNanos == 0L) {
            startNanos = frameTimeNanos
        } else {
            val budget = 1_000_000_000.0 / refreshRate.coerceAtLeast(30f)
            frames += 1
            if (frameTimeNanos - lastNanos > budget * 1.5) {
                janky += 1
            }
        }
        lastNanos = frameTimeNanos
        if (frameTimeNanos - startNanos >= durationMillis * 1_000_000L) {
            running = false
            if (frames > 0) {
                onResult(janky.toFloat() / frames)
            }
            return
        }
        Choreographer.getInstance().postFrameCallback(this)
    }
}
