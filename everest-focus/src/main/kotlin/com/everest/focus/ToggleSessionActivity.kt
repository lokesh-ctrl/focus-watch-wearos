package com.everest.focus

import android.app.Activity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import com.everest.focus.calendar.CalendarHelper
import com.everest.focus.data.FocusPreferences
import com.everest.focus.data.FocusState
import kotlinx.coroutines.runBlocking

class ToggleSessionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vibrator = getSystemService(Vibrator::class.java)
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))

        val manager = FocusSessionManager.getInstance(this)

        if (manager.state.value is FocusState.Idle) {
            val prefs = FocusPreferences(this)
            val calendarEnabled = runBlocking { prefs.isCalendarEnabled() }

            if (calendarEnabled) {
                val helper = CalendarHelper(this)
                val event = helper.getNextEvent()
                val focusMinutes = runBlocking { prefs.getFocusDuration() }
                val suggested = helper.shouldSuggestShorterFocus(event, focusMinutes)
                if (suggested != null) {
                    runBlocking { prefs.setFocusDuration(suggested) }
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), -1))
                }
            }
        }

        manager.toggleSession()
        finish()
    }
}
