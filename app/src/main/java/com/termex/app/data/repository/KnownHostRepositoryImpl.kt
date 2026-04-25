package com.termex.app.data.repository

import com.termex.app.data.local.KnownHostDao
import com.termex.app.data.local.toDomain
import com.termex.app.data.local.toEntity
import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnownHostRepositoryImpl @Inject constructor(
    private val knownHostDao: KnownHostDao
) : KnownHostRepository {

    override fun getAllKnownHosts(): Flow<List<KnownHost>> {
        return knownHostDao.getAllKnownHosts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getKnownHost(hostname: String, port: Int): KnownHost? {
        return knownHostDao.getKnownHost(hostname, port)?.toDomain()
    }

    override suspend fun addKnownHost(knownHost: KnownHost) {
        knownHostDao.insert(knownHost.toEntity())
    }

    override suspend fun updateKnownHost(knownHost: KnownHost) {
        knownHostDao.update(knownHost.toEntity())
    }

    override suspend fun deleteKnownHost(knownHost: KnownHost) {
        knownHostDao.delete(knownHost.toEntity())
    }

    override suspend fun deleteAll() {
        knownHostDao.deleteAll()
    }
}
