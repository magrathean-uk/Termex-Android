package com.termex.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: String): ServerEntity?

    @Query("SELECT * FROM servers WHERE workplaceId = :workplaceId ORDER BY name ASC")
    fun getServersByWorkplace(workplaceId: String): Flow<List<ServerEntity>>

    @Query("UPDATE servers SET workplaceId = NULL WHERE workplaceId = :workplaceId")
    suspend fun clearWorkplace(workplaceId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Delete
    suspend fun deleteServer(server: ServerEntity)
}
