package com.everest.focus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE dateKey = :dateKey ORDER BY startTimeMillis DESC")
    suspend fun getSessionsForDate(dateKey: String): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE startTimeMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)
}
