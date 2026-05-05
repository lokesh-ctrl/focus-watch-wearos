package com.focusdial.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SessionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val manager = FocusSessionManager.getInstance(context)
        when (intent.action) {
            FocusService.ACTION_PAUSE -> manager.pauseSession()
            FocusService.ACTION_RESUME -> manager.resumeSession()
            FocusService.ACTION_STOP -> manager.stopSession()
        }
    }
}
