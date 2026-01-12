package com.termex.app.data.repository

import com.termex.app.data.local.ServerDao
import com.termex.app.data.local.toDomain
import com.termex.app.data.local.toEntity
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao
) : ServerRepository {
    
    override fun getAllServers(): Flow<List<Server>> {
        return serverDao.getAllServers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getServer(id: String): Server? {
        return serverDao.getServerById(id)?.toDomain()
    }

    override suspend fun addServer(server: Server) {
        serverDao.insertServer(server.toEntity())
    }
    
    override suspend fun updateServer(server: Server) {
        serverDao.updateServer(server.toEntity())
    }

    override suspend fun deleteServer(server: Server) {
        serverDao.deleteServer(server.toEntity())
    }
}
