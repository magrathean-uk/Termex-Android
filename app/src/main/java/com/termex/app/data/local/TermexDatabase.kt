package com.termex.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ServerEntity::class, WorkplaceEntity::class, SnippetEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TermexDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun workplaceDao(): WorkplaceDao
    abstract fun snippetDao(): SnippetDao
}
