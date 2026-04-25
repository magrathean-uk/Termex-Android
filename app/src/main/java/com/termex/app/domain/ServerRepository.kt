package com.termex.app.domain

import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getAllServers(): Flow<List<Server>>
    suspend fun getServer(id: String): Server?
    suspend fun addServer(server: Server)
    suspend fun updateServer(server: Server)
    suspend fun deleteServer(server: Server)
}
