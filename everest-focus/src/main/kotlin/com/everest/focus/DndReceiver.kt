package com.everest.focus

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) return

        val nm = context.getSystemService(NotificationManager::class.java)
        when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> {
                FocusSessionManager.getInstance(context).startFocusIfIdle()
            }
            else -> { /* DND off — do not auto-stop */ }
        }
    }
}
