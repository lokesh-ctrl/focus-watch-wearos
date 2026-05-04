package com.focusdial.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {

    private val prefs = FocusPreferences(context)

    fun isAvailable(): Boolean {
        return try {
            HealthConnectClient.getOrCreate(context)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        )
    }

    suspend fun logMindfulnessSession(startMillis: Long, endMillis: Long) {
        if (!isAvailable()) return
        if (!prefs.isHealthConnectEnabled()) return
        if (!prefs.isPro()) return

        try {
            val client = HealthConnectClient.getOrCreate(context)
            val startInstant = Instant.ofEpochMilli(startMillis)
            val endInstant = Instant.ofEpochMilli(endMillis)
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startInstant)
            val record = ExerciseSessionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
                title = "Focus Session",
                notes = "Deep work session logged by Focus Dial"
            )
            client.insertRecords(listOf(record))
        } catch (_: Exception) {
            // Health Connect not available or permission denied
        }
    }
}
