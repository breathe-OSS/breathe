package com.sidharthify.breathe

import android.content.Context
import android.content.Intent

fun forceWidgetUpdate(context: android.content.Context) {
    val intent = android.content.Intent(context, com.sidharthify.breathe.widgets.BreatheWidgetReceiver::class.java).apply {
        action = "com.sidharthify.breathe.FORCE_WIDGET_UPDATE"
    }
    context.sendBroadcast(intent)
}