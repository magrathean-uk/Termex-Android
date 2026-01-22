package com.termex.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.termex.app.domain.AuthMode
import com.termex.app.domain.PortForward
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
    val workplaceId: String?,
    val portForwardsData: String? = null,
    val jumpHostId: String? = null,
    val forwardAgent: Boolean = false,
    val isDemo: Boolean = false
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
        workplaceId = workplaceId,
        portForwards = portForwards,
        jumpHostId = jumpHostId,
        forwardAgent = forwardAgent,
        isDemo = isDemo
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
        workplaceId = workplaceId,
        portForwardsData = portForwardsJson,
        jumpHostId = jumpHostId,
        forwardAgent = forwardAgent,
        isDemo = isDemo
    )
}
