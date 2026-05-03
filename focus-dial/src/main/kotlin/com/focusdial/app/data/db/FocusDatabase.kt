package com.focusdial.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, DaySummaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun daySummaryDao(): DaySummaryDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null

        fun getInstance(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focus_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
