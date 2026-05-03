package com.everest.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val manager = FocusSessionManager.getInstance(context)
        when (intent.action) {
            FocusSessionManager.ACTION_FOCUS_COMPLETE -> manager.onFocusTimerExpired()
            FocusSessionManager.ACTION_BREAK_COMPLETE -> manager.onBreakTimerExpired()
        }
    }
}
