package com.focusdial.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.focusdial.app.data.FocusState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FocusService : LifecycleService() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var vibrator: Vibrator
    private var previousState: String = ""

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        vibrator = getSystemService(Vibrator::class.java)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(
            NOTIFICATION_ID,
            buildOngoingNotification("Focus session active"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        observeState()
        return START_STICKY
    }

    private fun observeState() {
        lifecycleScope.launch {
            val manager = FocusSessionManager.getInstance(this@FocusService)
            manager.state.collectLatest { state ->
                when (state) {
                    is FocusState.Focus -> {
                        if (previousState == "BREAK" || previousState == "IDLE") {
                            vibrateStart()
                        }
                        previousState = "FOCUS"
                        updateLoop(manager)
                    }
                    is FocusState.Break -> {
                        if (previousState == "FOCUS") {
                            vibrateBreak()
                            showBreakNotification()
                        }
                        previousState = "BREAK"
                        updateLoop(manager)
                    }
                    is FocusState.Idle -> {
                        if (previousState == "BREAK") {
                            vibrateEnd()
                        }
                        previousState = "IDLE"
                        stopSelf()
                    }
                }
            }
        }
    }

    private suspend fun updateLoop(manager: FocusSessionManager) {
        while (true) {
            val state = manager.state.value
            val text = when (state) {
                is FocusState.Focus -> {
                    val remaining = manager.remainingFocusMillis
                    val minutes = (remaining / 60_000).toInt()
                    "Focus: ${minutes}min remaining"
                }
                is FocusState.Break -> {
                    val remaining = manager.remainingBreakMillis
                    val minutes = (remaining / 60_000).toInt()
                    "Break: ${minutes}min left"
                }
                is FocusState.Idle -> break
            }
            updateOngoingNotification(text)
            delay(60_000L)
        }
    }

    private fun vibrateStart() {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateBreak() {
        val pattern = longArrayOf(0, 200, 100, 200, 100, 400)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun vibrateEnd() {
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun showBreakNotification() {
        val tapIntent = Intent(this, ToggleSessionActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_BREAK)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Time for a break!")
            .setContentText("Great focus session. Stretch and rest.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        notificationManager.notify(BREAK_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannels() {
        val focusChannel = NotificationChannel(
            CHANNEL_FOCUS, "Focus Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        val breakChannel = NotificationChannel(
            CHANNEL_BREAK, "Break Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
        }

        notificationManager.createNotificationChannels(listOf(focusChannel, breakChannel))
    }

    private fun buildOngoingNotification(text: String): Notification {
        val tapIntent = Intent(this, ToggleSessionActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Focus Dial")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher, "Stop", pendingIntent)
            .build()
    }

    private fun updateOngoingNotification(text: String) {
        notificationManager.notify(NOTIFICATION_ID, buildOngoingNotification(text))
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val BREAK_NOTIFICATION_ID = 1002
        private const val CHANNEL_FOCUS = "focus_timer"
        const val CHANNEL_BREAK = "break_alert"
    }
}
