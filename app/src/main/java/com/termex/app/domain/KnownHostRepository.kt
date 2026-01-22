package com.termex.app.domain

import kotlinx.coroutines.flow.Flow

interface KnownHostRepository {
    fun getAllKnownHosts(): Flow<List<KnownHost>>
    suspend fun getKnownHost(hostname: String, port: Int): KnownHost?
    suspend fun addKnownHost(knownHost: KnownHost)
    suspend fun updateKnownHost(knownHost: KnownHost)
    suspend fun deleteKnownHost(knownHost: KnownHost)
    suspend fun deleteAll()
}
