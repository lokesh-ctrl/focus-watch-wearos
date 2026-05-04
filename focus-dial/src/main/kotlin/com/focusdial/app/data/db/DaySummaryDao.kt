package com.focusdial.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DaySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DaySummaryEntity)

    @Query("SELECT * FROM day_summaries WHERE dateKey = :dateKey")
    suspend fun getForDate(dateKey: String): DaySummaryEntity?

    @Query("SELECT * FROM day_summaries ORDER BY dateKey DESC LIMIT :limit")
    suspend fun getRecentSummaries(limit: Int): List<DaySummaryEntity>

    @Query("SELECT * FROM day_summaries WHERE dateKey >= :afterDate ORDER BY dateKey ASC")
    suspend fun getSummariesAfter(afterDate: String): List<DaySummaryEntity>

    @Query("DELETE FROM day_summaries WHERE dateKey < :beforeKey")
    suspend fun deleteOlderThan(beforeKey: String)
}
