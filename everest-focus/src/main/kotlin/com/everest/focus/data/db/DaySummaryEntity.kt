package com.everest.focus.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_summaries")
data class DaySummaryEntity(
    @PrimaryKey val dateKey: String,
    val totalFocusMillis: Long,
    val sessionsCompleted: Int,
    val averageScore: Int,
    val hadAtLeastOneSession: Boolean
)
