package com.focusdial.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_prefs")

class FocusPreferences(private val context: Context) {

    private object Keys {
        val FOCUS_DURATION_MINUTES = intPreferencesKey("focus_duration_minutes")
        val BREAK_DURATION_MINUTES = intPreferencesKey("break_duration_minutes")
        val DAILY_GOAL_SESSIONS = intPreferencesKey("daily_goal_sessions")
        val DAILY_TOTAL_MILLIS = longPreferencesKey("daily_total_millis")
        val COMPLETED_SESSIONS_TODAY = intPreferencesKey("completed_sessions_today")
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
        val PERSISTED_STATE_TYPE = stringPreferencesKey("persisted_state_type")
        val PERSISTED_START_TIME = longPreferencesKey("persisted_start_time")
        val PERSISTED_DURATION = longPreferencesKey("persisted_duration")
        val PERSISTED_INTERRUPTIONS = intPreferencesKey("persisted_interruptions")
        // Theme
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        // Streak
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val STREAK_LAST_ACTIVE_DATE = stringPreferencesKey("streak_last_active_date")
        // Adaptive breaks
        val CONSECUTIVE_SESSIONS_TODAY = intPreferencesKey("consecutive_sessions_today")
        val LAST_SESSION_END_TIME = longPreferencesKey("last_session_end_time")
        val ADAPTIVE_BREAKS_ENABLED = booleanPreferencesKey("adaptive_breaks_enabled")
        // Calendar
        val CALENDAR_ENABLED = booleanPreferencesKey("calendar_enabled")
        // Pro / Billing
        val IS_PRO = booleanPreferencesKey("is_pro")
        // Onboarding
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        // DND
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val PREVIOUS_INTERRUPTION_FILTER = intPreferencesKey("previous_interruption_filter")
    }

    // --- Settings ---

    suspend fun getFocusDuration(): Int {
        return context.dataStore.data.first()[Keys.FOCUS_DURATION_MINUTES] ?: 50
    }

    suspend fun setFocusDuration(minutes: Int) {
        context.dataStore.edit { it[Keys.FOCUS_DURATION_MINUTES] = minutes }
    }

    suspend fun getBreakDuration(): Int {
        return context.dataStore.data.first()[Keys.BREAK_DURATION_MINUTES] ?: 10
    }

    suspend fun setBreakDuration(minutes: Int) {
        context.dataStore.edit { it[Keys.BREAK_DURATION_MINUTES] = minutes }
    }

    suspend fun getDailyGoal(): Int {
        return context.dataStore.data.first()[Keys.DAILY_GOAL_SESSIONS] ?: 4
    }

    suspend fun setDailyGoal(sessions: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL_SESSIONS] = sessions }
    }

    // --- Daily Metrics ---

    suspend fun addCompletedSession(durationMillis: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DAILY_TOTAL_MILLIS] ?: 0L
            prefs[Keys.DAILY_TOTAL_MILLIS] = current + durationMillis
            val sessions = prefs[Keys.COMPLETED_SESSIONS_TODAY] ?: 0
            prefs[Keys.COMPLETED_SESSIONS_TODAY] = sessions + 1
        }
    }

    suspend fun getDailyTotalMillis(): Long {
        checkDayReset()
        return context.dataStore.data.first()[Keys.DAILY_TOTAL_MILLIS] ?: 0L
    }

    suspend fun getCompletedSessions(): Int {
        checkDayReset()
        return context.dataStore.data.first()[Keys.COMPLETED_SESSIONS_TODAY] ?: 0
    }

    // --- State Persistence ---

    suspend fun persistState(state: FocusState) {
        context.dataStore.edit { prefs ->
            when (state) {
                is FocusState.Idle -> {
                    prefs[Keys.PERSISTED_STATE_TYPE] = "IDLE"
                }
                is FocusState.Focus -> {
                    prefs[Keys.PERSISTED_STATE_TYPE] = "FOCUS"
                    prefs[Keys.PERSISTED_START_TIME] = state.startTimeMillis
                    prefs[Keys.PERSISTED_DURATION] = state.durationMillis
                    prefs[Keys.PERSISTED_INTERRUPTIONS] = state.interruptionCount
                }
                is FocusState.Break -> {
                    prefs[Keys.PERSISTED_STATE_TYPE] = "BREAK"
                    prefs[Keys.PERSISTED_START_TIME] = state.startTimeMillis
                    prefs[Keys.PERSISTED_DURATION] = state.durationMillis
                }
            }
        }
    }

    suspend fun restoreState(): FocusState {
        val prefs = context.dataStore.data.first()
        return when (prefs[Keys.PERSISTED_STATE_TYPE]) {
            "FOCUS" -> FocusState.Focus(
                startTimeMillis = prefs[Keys.PERSISTED_START_TIME] ?: return FocusState.Idle,
                durationMillis = prefs[Keys.PERSISTED_DURATION] ?: return FocusState.Idle,
                interruptionCount = prefs[Keys.PERSISTED_INTERRUPTIONS] ?: 0
            )
            "BREAK" -> FocusState.Break(
                startTimeMillis = prefs[Keys.PERSISTED_START_TIME] ?: return FocusState.Idle,
                durationMillis = prefs[Keys.PERSISTED_DURATION] ?: return FocusState.Idle
            )
            else -> FocusState.Idle
        }
    }

    // --- Theme ---

    suspend fun getSelectedTheme(): String {
        return context.dataStore.data.first()[Keys.SELECTED_THEME] ?: "minimal"
    }

    suspend fun setSelectedTheme(id: String) {
        context.dataStore.edit { it[Keys.SELECTED_THEME] = id }
    }

    // --- Streak ---

    suspend fun getCurrentStreak(): Int {
        return context.dataStore.data.first()[Keys.CURRENT_STREAK] ?: 0
    }

    suspend fun setCurrentStreak(count: Int) {
        context.dataStore.edit { it[Keys.CURRENT_STREAK] = count }
    }

    suspend fun getStreakLastActiveDate(): String {
        return context.dataStore.data.first()[Keys.STREAK_LAST_ACTIVE_DATE] ?: ""
    }

    suspend fun setStreakLastActiveDate(date: String) {
        context.dataStore.edit { it[Keys.STREAK_LAST_ACTIVE_DATE] = date }
    }

    // --- Adaptive Breaks ---

    suspend fun getConsecutiveSessionsToday(): Int {
        checkDayReset()
        return context.dataStore.data.first()[Keys.CONSECUTIVE_SESSIONS_TODAY] ?: 0
    }

    suspend fun incrementConsecutiveSessions() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CONSECUTIVE_SESSIONS_TODAY] ?: 0
            prefs[Keys.CONSECUTIVE_SESSIONS_TODAY] = current + 1
            prefs[Keys.LAST_SESSION_END_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun shouldResetConsecutive(): Boolean {
        val lastEnd = context.dataStore.data.first()[Keys.LAST_SESSION_END_TIME] ?: 0L
        val gap = System.currentTimeMillis() - lastEnd
        return gap > 2 * 60 * 60 * 1000L
    }

    suspend fun resetConsecutiveSessions() {
        context.dataStore.edit { it[Keys.CONSECUTIVE_SESSIONS_TODAY] = 0 }
    }

    suspend fun isAdaptiveBreaksEnabled(): Boolean {
        return context.dataStore.data.first()[Keys.ADAPTIVE_BREAKS_ENABLED] ?: true
    }

    suspend fun setAdaptiveBreaksEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADAPTIVE_BREAKS_ENABLED] = enabled }
    }

    // --- Calendar ---

    suspend fun isCalendarEnabled(): Boolean {
        return context.dataStore.data.first()[Keys.CALENDAR_ENABLED] ?: false
    }

    suspend fun setCalendarEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CALENDAR_ENABLED] = enabled }
    }

    // --- Pro ---

    suspend fun isPro(): Boolean {
        return context.dataStore.data.first()[Keys.IS_PRO] ?: false
    }

    suspend fun setPro(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_PRO] = enabled }
    }

    // --- Onboarding ---

    suspend fun isOnboardingComplete(): Boolean {
        return context.dataStore.data.first()[Keys.ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = true }
    }

    // --- DND ---

    suspend fun isDndEnabled(): Boolean {
        return context.dataStore.data.first()[Keys.DND_ENABLED] ?: false
    }

    suspend fun setDndEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DND_ENABLED] = enabled }
    }

    suspend fun getPreviousInterruptionFilter(): Int {
        return context.dataStore.data.first()[Keys.PREVIOUS_INTERRUPTION_FILTER] ?: -1
    }

    suspend fun setPreviousInterruptionFilter(filter: Int) {
        context.dataStore.edit { it[Keys.PREVIOUS_INTERRUPTION_FILTER] = filter }
    }

    // --- Day Reset ---

    private suspend fun checkDayReset() {
        val prefs = context.dataStore.data.first()
        val lastReset = prefs[Keys.LAST_RESET_DATE]
        val today = LocalDate.now().toString()
        if (lastReset != today) {
            context.dataStore.edit { mutablePrefs ->
                mutablePrefs[Keys.DAILY_TOTAL_MILLIS] = 0L
                mutablePrefs[Keys.COMPLETED_SESSIONS_TODAY] = 0
                mutablePrefs[Keys.CONSECUTIVE_SESSIONS_TODAY] = 0
                mutablePrefs[Keys.LAST_RESET_DATE] = today
            }
        }
    }
}
