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
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        val currentIndex = prefs[BreatheWidgetWorker.PREF_CURRENT_INDEX] ?: 0
        val newIndex = (currentIndex + direction).coerceAtLeast(0)
        
        prefs.toMutablePreferences().apply {
            this[BreatheWidgetWorker.PREF_CURRENT_INDEX] = newIndex
            // Set status to loading to give instant feedback
            this[BreatheWidgetWorker.PREF_STATUS] = "Loading"
        }
    }
    // refresh UI immediately to show "Loading..."
    BreatheWidget().update(context, glanceId)

    WorkManager.getInstance(context).enqueue(
        OneTimeWorkRequest.from(BreatheWidgetWorker::class.java)
    )
}