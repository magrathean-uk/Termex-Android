package com.termex.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SessionStateEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SessionStateDatabase : RoomDatabase() {
    abstract fun sessionStateDao(): SessionStateDao

    companion object {
        const val DB_NAME = "termex-session-database"
    }
}
