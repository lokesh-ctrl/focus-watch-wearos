package com.everest.focus

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.everest.focus.data.FocusState

class NotificationCounterService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return

        val manager = FocusSessionManager.getInstance(this)
        if (manager.state.value is FocusState.Focus) {
            manager.incrementInterruptions()
        }
    }
}
