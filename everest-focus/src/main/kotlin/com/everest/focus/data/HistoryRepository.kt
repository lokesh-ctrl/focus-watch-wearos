package com.everest.focus.data

import android.content.Context
import com.everest.focus.data.db.DaySummaryEntity
import com.everest.focus.data.db.FocusDatabase
import com.everest.focus.data.db.SessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HistoryRepository(context: Context) {

    private val db = FocusDatabase.getInstance(context)
    private val sessionDao = db.sessionDao()
    private val daySummaryDao = db.daySummaryDao()
    private val prefs = FocusPreferences(context)

    suspend fun logSession(
        startTimeMillis: Long,
        plannedDurationMillis: Long,
        actualDurationMillis: Long,
        interruptionCount: Int,
        completed: Boolean
    ) {
        val score = FocusScoreCalculator.calculate(
            plannedDurationMillis, actualDurationMillis, interruptionCount, completed
        )
        val dateKey = LocalDate.ofInstant(
            Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault()
        ).toString()

        sessionDao.insert(
            SessionEntity(
                startTimeMillis = startTimeMillis,
                durationMillis = plannedDurationMillis,
                actualDurationMillis = actualDurationMillis,
                interruptionCount = interruptionCount,
                focusScore = score,
                completed = completed,
                dateKey = dateKey
            )
        )

        updateDaySummary(dateKey)
        updateStreak(dateKey)
        pruneOldData()
    }

    suspend fun getWeeklyTotalMillis(): Long {
        val sevenDaysAgo = LocalDate.now().minusDays(7).toString()
        val summaries = daySummaryDao.getRecentSummaries(7)
        return summaries
            .filter { it.dateKey >= sevenDaysAgo }
            .sumOf { it.totalFocusMillis }
    }

    suspend fun getCurrentStreak(): Int {
        return prefs.getCurrentStreak()
    }

    suspend fun getDailyAverageScore(): Int {
        val today = LocalDate.now().toString()
        return daySummaryDao.getForDate(today)?.averageScore ?: 0
    }

    private suspend fun updateDaySummary(dateKey: String) {
        val sessions = sessionDao.getSessionsForDate(dateKey)
        if (sessions.isEmpty()) return

        val totalMillis = sessions.sumOf { it.actualDurationMillis }
        val count = sessions.size
        val avgScore = sessions.map { it.focusScore }.average().toInt()

        daySummaryDao.upsert(
            DaySummaryEntity(
                dateKey = dateKey,
                totalFocusMillis = totalMillis,
                sessionsCompleted = count,
                averageScore = avgScore,
                hadAtLeastOneSession = true
            )
        )
    }

    private suspend fun updateStreak(dateKey: String) {
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val lastActiveDate = prefs.getStreakLastActiveDate()
        val currentStreak = prefs.getCurrentStreak()

        when (lastActiveDate) {
            today -> { /* already counted today */ }
            yesterday -> {
                prefs.setCurrentStreak(currentStreak + 1)
                prefs.setStreakLastActiveDate(today)
            }
            else -> {
                prefs.setCurrentStreak(1)
                prefs.setStreakLastActiveDate(today)
            }
        }
    }

    private suspend fun pruneOldData() {
        val retentionDays = if (prefs.isPro()) 30L else 7L
        val cutoffDate = LocalDate.now().minusDays(retentionDays).toString()
        val cutoffMillis = LocalDate.now().minusDays(retentionDays)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        daySummaryDao.deleteOlderThan(cutoffDate)
        sessionDao.deleteOlderThan(cutoffMillis)
    }
}
