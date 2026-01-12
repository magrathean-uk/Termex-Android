package com.termex.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val hostname: String,
    val port: Int,
    val username: String,
    val authMode: AuthMode,
    val passwordKeychainID: String?,
    val keyId: String?,
    val workplaceId: String?
)

fun ServerEntity.toDomain() = Server(
    id = id,
    name = name,
    hostname = hostname,
    port = port,
    username = username,
    authMode = authMode,
    passwordKeychainID = passwordKeychainID,
    keyId = keyId,
    workplaceId = workplaceId
)

fun Server.toEntity() = ServerEntity(
    id = id,
    name = name,
    hostname = hostname,
    port = port,
    username = username,
    authMode = authMode,
    passwordKeychainID = passwordKeychainID,
    keyId = keyId,
    workplaceId = workplaceId
)
