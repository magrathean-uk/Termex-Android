package com.termex.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkplaceDao {
    @Query("SELECT * FROM workplaces")
    fun getAllWorkplaces(): Flow<List<WorkplaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkplace(workplace: WorkplaceEntity)

    @Delete
    suspend fun deleteWorkplace(workplace: WorkplaceEntity)
}
