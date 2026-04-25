package com.termex.app.data.repository

import com.termex.app.data.local.SessionStateDao
import com.termex.app.data.local.SessionStateEntity
import com.termex.app.domain.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionStateDao: SessionStateDao
) {
    
    fun getAllSessions(): Flow<List<SessionState>> =
        sessionStateDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    
    suspend fun getSession(sessionId: String): SessionState? =
        sessionStateDao.getSession(sessionId)?.toDomain()
    
    suspend fun getLatestSessionForServer(serverId: String): SessionState? =
        sessionStateDao.getLatestSessionForServer(serverId)?.toDomain()
    
    suspend fun saveSession(session: SessionState) {
        sessionStateDao.insertSession(session.toEntity())
    }
    
    suspend fun updateSession(session: SessionState) {
        sessionStateDao.updateSession(session.toEntity())
    }
    
    suspend fun deleteSession(sessionId: String) {
        sessionStateDao.deleteSession(sessionId)
    }
    
    suspend fun deleteAllSessions() {
        sessionStateDao.deleteAllSessions()
    }
    
    suspend fun cleanupOldSessions(maxAgeMillis: Long) {
        val cutoffTime = System.currentTimeMillis() - maxAgeMillis
        sessionStateDao.deleteOldSessions(cutoffTime)
    }
    
    private fun SessionStateEntity.toDomain() = SessionState(
        id = id,
        serverId = serverId,
        terminalBuffer = terminalBuffer,
        workingDirectory = workingDirectory,
        connectedAt = connectedAt,
        lastActiveAt = lastActiveAt
    )
    
    private fun SessionState.toEntity() = SessionStateEntity(
        id = id,
        serverId = serverId,
        terminalBuffer = terminalBuffer,
        workingDirectory = workingDirectory,
        connectedAt = connectedAt,
        lastActiveAt = lastActiveAt
    )
}
