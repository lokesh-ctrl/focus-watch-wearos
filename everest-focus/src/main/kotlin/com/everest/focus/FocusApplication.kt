package com.everest.focus

import android.app.Application

class FocusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FocusSessionManager.getInstance(this)
    }
}
