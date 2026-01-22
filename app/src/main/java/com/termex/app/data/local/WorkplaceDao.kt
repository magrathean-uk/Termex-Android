package com.termex.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkplaceDao {
    @Query("SELECT * FROM workplaces")
    fun getAllWorkplaces(): Flow<List<WorkplaceEntity>>

    @Query("SELECT * FROM workplaces WHERE id = :id LIMIT 1")
    suspend fun getWorkplace(id: String): WorkplaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workplace: WorkplaceEntity)

    @Update
    suspend fun update(workplace: WorkplaceEntity)

    @Delete
    suspend fun delete(workplace: WorkplaceEntity)
}
