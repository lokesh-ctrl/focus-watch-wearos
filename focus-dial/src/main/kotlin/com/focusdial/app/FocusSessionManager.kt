package com.focusdial.app

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.focusdial.app.complication.BreakCountdownSource
import com.focusdial.app.complication.DailyTotalSource
import com.focusdial.app.complication.FocusDurationSource
import com.focusdial.app.complication.InterruptionSource
import com.focusdial.app.complication.SessionCountSource
import com.focusdial.app.complication.StatusSource
import com.focusdial.app.data.AdaptiveBreakCalculator
import com.focusdial.app.data.FocusPreferences
import com.focusdial.app.data.FocusState
import com.focusdial.app.data.HealthConnectManager
import com.focusdial.app.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime

class FocusSessionManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex()
    private val preferences = FocusPreferences(context)
    private val historyRepository = HistoryRepository(context)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val healthConnectManager = HealthConnectManager(context)

    private val _state = MutableStateFlow<FocusState>(FocusState.Idle)
    val state: StateFlow<FocusState> = _state.asStateFlow()

    init {
        scope.launch {
            val restored = preferences.restoreState()
            if (restored !is FocusState.Idle) {
                val now = System.currentTimeMillis()
                val adjustedState = when (restored) {
                    is FocusState.Focus -> {
                        val elapsed = now - restored.startTimeMillis
                        if (elapsed >= restored.durationMillis) {
                            startBreakInternal()
                            return@launch
                        }
                        restored
                    }
                    is FocusState.Break -> {
                        val elapsed = now - restored.startTimeMillis
                        if (elapsed >= restored.durationMillis) {
                            FocusState.Idle
                        } else {
                            restored
                        }
                    }
                    is FocusState.Idle -> FocusState.Idle
                }
                _state.value = adjustedState
                if (adjustedState !is FocusState.Idle) {
                    scheduleAlarm(adjustedState)
                    startService()
                }
            }
        }
    }

    fun startFocusIfIdle() {
        scope.launch {
            mutex.withLock {
                if (_state.value is FocusState.Idle) {
                    startFocusInternal()
                }
            }
        }
    }

    fun startFocus() {
        scope.launch {
            mutex.withLock {
                startFocusInternal()
            }
        }
    }

    fun stopSession() {
        scope.launch {
            mutex.withLock {
                val current = _state.value
                if (current is FocusState.Focus) {
                    val elapsed = System.currentTimeMillis() - current.startTimeMillis
                    preferences.addCompletedSession(elapsed)
                    historyRepository.logSession(
                        startTimeMillis = current.startTimeMillis,
                        plannedDurationMillis = current.durationMillis,
                        actualDurationMillis = elapsed,
                        interruptionCount = current.interruptionCount,
                        completed = false
                    )
                }
                cancelAlarm()
                restoreDnd()
                _state.value = FocusState.Idle
                preferences.persistState(FocusState.Idle)
                stopService()
                requestComplicationUpdates()
            }
        }
    }

    fun onFocusTimerExpired() {
        scope.launch {
            mutex.withLock {
                val current = _state.value
                if (current is FocusState.Focus) {
                    preferences.addCompletedSession(current.durationMillis)
                    preferences.incrementConsecutiveSessions()
                    historyRepository.logSession(
                        startTimeMillis = current.startTimeMillis,
                        plannedDurationMillis = current.durationMillis,
                        actualDurationMillis = current.durationMillis,
                        interruptionCount = current.interruptionCount,
                        completed = true
                    )
                    healthConnectManager.logMindfulnessSession(
                        startMillis = current.startTimeMillis,
                        endMillis = System.currentTimeMillis()
                    )
                    startBreakInternal()
                }
            }
        }
    }

    fun onBreakTimerExpired() {
        scope.launch {
            mutex.withLock {
                if (_state.value is FocusState.Break) {
                    restoreDnd()
                    _state.value = FocusState.Idle
                    preferences.persistState(FocusState.Idle)
                    stopService()
                    requestComplicationUpdates()
                }
            }
        }
    }

    fun incrementInterruptions() {
        scope.launch {
            mutex.withLock {
                val current = _state.value
                if (current is FocusState.Focus) {
                    _state.value = current.copy(interruptionCount = current.interruptionCount + 1)
                    requestComplicationUpdates()
                }
            }
        }
    }

    fun toggleSession() {
        scope.launch {
            mutex.withLock {
                when (_state.value) {
                    is FocusState.Idle -> startFocusInternal()
                    is FocusState.Focus, is FocusState.Break -> {
                        val current = _state.value
                        if (current is FocusState.Focus) {
                            val elapsed = System.currentTimeMillis() - current.startTimeMillis
                            preferences.addCompletedSession(elapsed)
                            historyRepository.logSession(
                                startTimeMillis = current.startTimeMillis,
                                plannedDurationMillis = current.durationMillis,
                                actualDurationMillis = elapsed,
                                interruptionCount = current.interruptionCount,
                                completed = false
                            )
                        }
                        cancelAlarm()
                        restoreDnd()
                        _state.value = FocusState.Idle
                        preferences.persistState(FocusState.Idle)
                        stopService()
                        requestComplicationUpdates()
                    }
                }
            }
        }
    }

    val elapsedFocusMillis: Long
        get() {
            val current = _state.value
            if (current is FocusState.Focus) {
                return System.currentTimeMillis() - current.startTimeMillis
            }
            return 0L
        }

    val progressFraction: Float
        get() {
            val current = _state.value
            if (current is FocusState.Focus) {
                val elapsed = System.currentTimeMillis() - current.startTimeMillis
                return (elapsed.toFloat() / current.durationMillis).coerceIn(0f, 1f)
            }
            return 0f
        }

    val remainingFocusMillis: Long
        get() {
            val current = _state.value
            if (current is FocusState.Focus) {
                val remaining = current.durationMillis - (System.currentTimeMillis() - current.startTimeMillis)
                return remaining.coerceAtLeast(0L)
            }
            return 0L
        }

    val remainingBreakMillis: Long
        get() {
            val current = _state.value
            if (current is FocusState.Break) {
                val remaining = current.durationMillis - (System.currentTimeMillis() - current.startTimeMillis)
                return remaining.coerceAtLeast(0L)
            }
            return 0L
        }

    val statusText: String
        get() = when (_state.value) {
            is FocusState.Focus -> "FOCUS"
            is FocusState.Break -> "BREAK"
            is FocusState.Idle -> "IDLE"
        }

    private suspend fun startFocusInternal() {
        cancelAlarm()

        if (preferences.shouldResetConsecutive()) {
            preferences.resetConsecutiveSessions()
        }

        enableDndIfConfigured()

        val durationMinutes = preferences.getFocusDuration()
        val durationMillis = durationMinutes * 60_000L
        val now = System.currentTimeMillis()
        val newState = FocusState.Focus(
            startTimeMillis = now,
            durationMillis = durationMillis
        )
        _state.value = newState
        preferences.persistState(newState)
        scheduleAlarm(newState)
        startService()
        requestComplicationUpdates()
    }

    private suspend fun startBreakInternal() {
        cancelAlarm()
        val baseBreakMinutes = preferences.getBreakDuration()

        val breakMinutes = if (preferences.isAdaptiveBreaksEnabled()) {
            val consecutive = preferences.getConsecutiveSessionsToday()
            val currentHour = LocalTime.now().hour
            AdaptiveBreakCalculator.suggest(baseBreakMinutes, consecutive, currentHour).durationMinutes
        } else {
            baseBreakMinutes
        }

        val breakMillis = breakMinutes * 60_000L
        val now = System.currentTimeMillis()
        val newState = FocusState.Break(
            startTimeMillis = now,
            durationMillis = breakMillis
        )
        _state.value = newState
        preferences.persistState(newState)
        scheduleAlarm(newState)
        requestComplicationUpdates()
    }

    private fun scheduleAlarm(state: FocusState) {
        val (triggerTime, action) = when (state) {
            is FocusState.Focus -> {
                val trigger = state.startTimeMillis + state.durationMillis
                trigger to ACTION_FOCUS_COMPLETE
            }
            is FocusState.Break -> {
                val trigger = state.startTimeMillis + state.durationMillis
                trigger to ACTION_BREAK_COMPLETE
            }
            is FocusState.Idle -> return
        }

        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
        )
    }

    private fun cancelAlarm() {
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun startService() {
        val intent = Intent(context, FocusService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopService() {
        val intent = Intent(context, FocusService::class.java)
        context.stopService(intent)
    }

    private fun requestComplicationUpdates() {
        val sources = listOf(
            ComponentName(context, FocusDurationSource::class.java),
            ComponentName(context, BreakCountdownSource::class.java),
            ComponentName(context, SessionCountSource::class.java),
            ComponentName(context, DailyTotalSource::class.java),
            ComponentName(context, InterruptionSource::class.java),
            ComponentName(context, StatusSource::class.java),
        )
        sources.forEach { source ->
            ComplicationDataSourceUpdateRequester.create(context, source)
                .requestUpdateAll()
        }
        FocusTileService.requestUpdate(context)
    }

    private suspend fun enableDndIfConfigured() {
        if (!preferences.isDndEnabled() || !preferences.isPro()) return
        if (!notificationManager.isNotificationPolicyAccessGranted) return

        val currentFilter = notificationManager.currentInterruptionFilter
        preferences.setPreviousInterruptionFilter(currentFilter)
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
    }

    private suspend fun restoreDnd() {
        if (!preferences.isDndEnabled() || !preferences.isPro()) return
        if (!notificationManager.isNotificationPolicyAccessGranted) return

        val previousFilter = preferences.getPreviousInterruptionFilter()
        if (previousFilter != -1) {
            notificationManager.setInterruptionFilter(previousFilter)
            preferences.setPreviousInterruptionFilter(-1)
        }
    }

    companion object {
        const val ACTION_FOCUS_COMPLETE = "com.focusdial.app.ACTION_FOCUS_COMPLETE"
        const val ACTION_BREAK_COMPLETE = "com.focusdial.app.ACTION_BREAK_COMPLETE"

        @Volatile
        private var instance: FocusSessionManager? = null

        fun getInstance(context: Context): FocusSessionManager {
            return instance ?: synchronized(this) {
                instance ?: FocusSessionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
