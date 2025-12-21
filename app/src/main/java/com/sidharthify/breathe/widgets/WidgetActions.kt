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
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        cycleLocation(context, glanceId, 1)
    }
}

class PrevLocationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        cycleLocation(context, glanceId, -1)
    }
}

private suspend fun cycleLocation(context: Context, glanceId: GlanceId, direction: Int) {
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
        OneTimeWorkRequest.from(BreatheWidgetWorker::class.java)
    )
}