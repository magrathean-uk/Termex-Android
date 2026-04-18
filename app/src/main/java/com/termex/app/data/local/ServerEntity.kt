package com.termex.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.termex.app.domain.AuthMode
import com.termex.app.domain.PortForward
import com.termex.app.domain.Server

@Entity(
    tableName = "servers",
    indices = [Index(value = ["workplaceId"])]
)
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val hostname: String,
    val port: Int,
    val username: String,
    val authMode: AuthMode,
    val passwordKeychainID: String?,
    val keyId: String?,
    @ColumnInfo(defaultValue = "NULL") val certificatePath: String? = null,
    val workplaceId: String?,
    val portForwardsData: String? = null,
    val jumpHostId: String? = null,
    val forwardAgent: Boolean = false,
    val isDemo: Boolean = false,
    @ColumnInfo(defaultValue = "0") val identitiesOnly: Boolean = false,
    @ColumnInfo(defaultValue = "0") val persistentSessionEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val startupCommand: String? = null
)

fun ServerEntity.toDomain(): Server {
    val portForwards = Converters().portForwardsFromJson(portForwardsData)
    return Server(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        username = username,
        authMode = authMode,
        passwordKeychainID = passwordKeychainID,
        keyId = keyId,
        certificatePath = certificatePath,
        workplaceId = workplaceId,
        portForwards = portForwards,
        jumpHostId = jumpHostId,
        forwardAgent = forwardAgent,
        isDemo = isDemo,
        identitiesOnly = identitiesOnly,
        persistentSessionEnabled = persistentSessionEnabled,
        startupCommand = startupCommand
    )
}

fun Server.toEntity(): ServerEntity {
    val portForwardsJson = Converters().portForwardsToJson(portForwards)
    return ServerEntity(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        username = username,
        authMode = authMode,
        passwordKeychainID = passwordKeychainID,
        keyId = keyId,
        certificatePath = certificatePath,
        workplaceId = workplaceId,
        portForwardsData = portForwardsJson,
        jumpHostId = jumpHostId,
        forwardAgent = forwardAgent,
        isDemo = isDemo,
        identitiesOnly = identitiesOnly,
        persistentSessionEnabled = persistentSessionEnabled,
        startupCommand = startupCommand
    )
}
