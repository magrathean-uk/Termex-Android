package com.termex.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KnownHostDao {
    @Query("SELECT * FROM known_hosts ORDER BY lastSeenAt DESC")
    fun getAllKnownHosts(): Flow<List<KnownHostEntity>>

    @Query("SELECT * FROM known_hosts WHERE hostname = :hostname AND port = :port LIMIT 1")
    suspend fun getKnownHost(hostname: String, port: Int): KnownHostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(knownHost: KnownHostEntity)

    @Update
    suspend fun update(knownHost: KnownHostEntity)

    @Delete
    suspend fun delete(knownHost: KnownHostEntity)

    @Query("DELETE FROM known_hosts")
    suspend fun deleteAll()
}
