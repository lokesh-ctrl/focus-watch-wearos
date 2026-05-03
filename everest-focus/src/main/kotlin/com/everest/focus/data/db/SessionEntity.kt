package com.everest.focus.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMillis: Long,
    val durationMillis: Long,
    val actualDurationMillis: Long,
    val interruptionCount: Int,
    val focusScore: Int,
    val completed: Boolean,
    val dateKey: String
)
