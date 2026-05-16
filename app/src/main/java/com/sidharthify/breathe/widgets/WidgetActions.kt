// SPDX-License-Identifier: MIT
/*
 * BreatheWidget.kt
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

package com.sidharthify.breathe.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class NextLocationAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        cycleLocation(context, glanceId, 1)
    }
}

class PrevLocationAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        cycleLocation(context, glanceId, -1)
    }
}

private suspend fun cycleLocation(
    context: Context,
    glanceId: GlanceId,
    direction: Int,
) {
    val appPrefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
    val pinnedIds = appPrefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()
    val size = pinnedIds.size

    if (size <= 1) return

    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        val currentIndex = prefs[BreatheWidgetWorker.PREF_CURRENT_INDEX] ?: 0

        var newIndex = currentIndex + direction

        if (newIndex >= size) newIndex = 0
        if (newIndex < 0) newIndex = size - 1

        prefs.toMutablePreferences().apply {
            this[BreatheWidgetWorker.PREF_CURRENT_INDEX] = newIndex
            this[BreatheWidgetWorker.PREF_STATUS] = "Loading" // Trigger "..." on refresh button
        }
    }

    BreatheWidget().update(context, glanceId)

    WorkManager.getInstance(context).enqueue(
        OneTimeWorkRequest.from(BreatheWidgetWorker::class.java),
    )
}
