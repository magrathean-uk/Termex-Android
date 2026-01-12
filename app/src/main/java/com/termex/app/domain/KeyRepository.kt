package com.termex.app.domain

import kotlinx.coroutines.flow.Flow

interface KeyRepository {
    fun getAllKeys(): Flow<List<SSHKey>>
    suspend fun generateKey(name: String, type: String = "RSA", bits: Int = 2048)
    suspend fun importKey(name: String, privateKeyContent: String, publicKeyContent: String? = null)
    suspend fun deleteKey(key: SSHKey)
    fun getKeyPath(name: String): String
}
