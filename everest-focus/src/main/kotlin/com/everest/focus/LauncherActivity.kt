package com.everest.focus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.everest.focus.data.FocusPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = FocusPreferences(this)

        CoroutineScope(Dispatchers.Main).launch {
            val target = if (prefs.isOnboardingComplete()) {
                SettingsActivity::class.java
            } else {
                OnboardingActivity::class.java
            }
            startActivity(Intent(this@LauncherActivity, target))
            finish()
        }
    }
}
