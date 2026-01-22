package com.termex.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.termex.app.domain.KnownHost
import java.util.Date

@Entity(
    tableName = "known_hosts",
    indices = [Index(value = ["hostname", "port"], unique = true)]
)
data class KnownHostEntity(
    @PrimaryKey val id: String,
    val hostname: String,
    val port: Int,
    val keyType: String,
    val fingerprint: String,
    val publicKey: String,
    val addedAt: Long,
    val lastSeenAt: Long
)

fun KnownHostEntity.toDomain() = KnownHost(
    id = id,
    hostname = hostname,
    port = port,
    keyType = keyType,
    fingerprint = fingerprint,
    publicKey = publicKey,
    addedAt = Date(addedAt),
    lastSeenAt = Date(lastSeenAt)
)

fun KnownHost.toEntity() = KnownHostEntity(
    id = id,
    hostname = hostname,
    port = port,
    keyType = keyType,
    fingerprint = fingerprint,
    publicKey = publicKey,
    addedAt = addedAt.time,
    lastSeenAt = lastSeenAt.time
)
