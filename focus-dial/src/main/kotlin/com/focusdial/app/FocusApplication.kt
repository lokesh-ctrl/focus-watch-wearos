package com.focusdial.app

import android.app.Application

class FocusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FocusSessionManager.getInstance(this)
    }
}
