package com.termex.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {
    
    @Query("SELECT * FROM session_states ORDER BY lastActiveAt DESC")
    fun getAllSessions(): Flow<List<SessionStateEntity>>
    
    @Query("SELECT * FROM session_states WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): SessionStateEntity?
    
    @Query("SELECT * FROM session_states WHERE serverId = :serverId ORDER BY lastActiveAt DESC LIMIT 1")
    suspend fun getLatestSessionForServer(serverId: String): SessionStateEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionStateEntity)
    
    @Update
    suspend fun updateSession(session: SessionStateEntity)
    
    @Query("DELETE FROM session_states WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
    
    @Query("DELETE FROM session_states")
    suspend fun deleteAllSessions()
    
    @Query("DELETE FROM session_states WHERE lastActiveAt < :timestamp")
    suspend fun deleteOldSessions(timestamp: Long)
}
