package com.focusdial.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.focusdial.app.data.FocusPreferences
import kotlinx.coroutines.runBlocking

class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = FocusPreferences(this)

        val target = runBlocking {
            if (prefs.isOnboardingComplete()) {
                SettingsActivity::class.java
            } else {
                OnboardingActivity::class.java
            }
        }
        startActivity(Intent(this, target))
        finish()
    }
}
