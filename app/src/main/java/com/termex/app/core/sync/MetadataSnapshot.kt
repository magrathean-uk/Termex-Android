package com.termex.app.core.sync

import com.termex.app.domain.AuthMode
import com.termex.app.domain.CertificateType
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.SSHCertificate
import com.termex.app.domain.SSHKey
import com.termex.app.domain.Server
import com.termex.app.domain.Snippet
import com.termex.app.domain.Workplace
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SNAPSHOT_SCHEMA_VERSION = 1

val metadataSnapshotJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
data class MetadataSnapshot(
    val schemaVersion: Int = SNAPSHOT_SCHEMA_VERSION,
    val createdAtMillis: Long = 0L,
    val servers: List<MetadataServer> = emptyList(),
    val keys: List<MetadataKey> = emptyList(),
    val certificates: List<MetadataCertificate> = emptyList(),
    val workplaces: List<MetadataWorkplace> = emptyList(),
    val snippets: List<MetadataSnippet> = emptyList()
) {
    fun normalized(): MetadataSnapshot {
        return copy(
            servers = servers.sortedBy { it.sortKey() },
            keys = keys.sortedBy { it.sortKey() },
            certificates = certificates.sortedBy { it.sortKey() },
            workplaces = workplaces.sortedBy { it.sortKey() },
            snippets = snippets.sortedBy { it.sortKey() }
        )
    }

    fun mergeFrom(remote: MetadataSnapshot): MetadataSnapshot {
        return copy(
            schemaVersion = maxOf(schemaVersion, remote.schemaVersion),
            createdAtMillis = maxOf(createdAtMillis, remote.createdAtMillis),
            servers = mergeById(servers, remote.servers) { it.id },
            keys = mergeById(keys, remote.keys) { it.path },
            certificates = mergeById(certificates, remote.certificates) { it.path },
            workplaces = mergeById(workplaces, remote.workplaces) { it.id },
            snippets = mergeById(snippets, remote.snippets) { it.id }
        ).normalized()
    }

    fun missingSecretCount(): Int {
        val passwordRefs = servers.count { !it.passwordKeychainID.isNullOrBlank() || it.authMode == AuthMode.PASSWORD }
        val keySecrets = keys.size
        val certSecrets = certificates.size
        val knownKeyPaths = keys.mapTo(mutableSetOf()) { it.path }
        val knownCertPaths = certificates.mapTo(mutableSetOf()) { it.path }
        val missingKeyRefs = servers.count { !it.keyId.isNullOrBlank() && it.keyId !in knownKeyPaths }
        val missingCertRefs = servers.count { !it.certificatePath.isNullOrBlank() && it.certificatePath !in knownCertPaths }
        return passwordRefs + keySecrets + certSecrets + missingKeyRefs + missingCertRefs
    }

    companion object {
        fun fromDomain(
            servers: List<Server>,
            keys: List<SSHKey>,
            certificates: List<SSHCertificate>,
            workplaces: List<Workplace>,
            snippets: List<Snippet>,
            createdAtMillis: Long = System.currentTimeMillis()
        ): MetadataSnapshot {
            return MetadataSnapshot(
                schemaVersion = SNAPSHOT_SCHEMA_VERSION,
                createdAtMillis = createdAtMillis,
                servers = servers.map { it.toMetadata() },
                keys = keys.map { it.toMetadata() },
                certificates = certificates.map { it.toMetadata() },
                workplaces = workplaces.map { it.toMetadata() },
                snippets = snippets.map { it.toMetadata() }
            ).normalized()
        }

        fun fromJson(value: String): MetadataSnapshot {
            return metadataSnapshotJson.decodeFromString(MetadataSnapshot.serializer(), value)
        }
    }
}

@Serializable
data class MetadataServer(
    val id: String,
    val name: String,
    val hostname: String,
    val port: Int,
    val username: String,
    val authMode: AuthMode,
    val passwordKeychainID: String? = null,
    val keyId: String? = null,
    val certificatePath: String? = null,
    val workplaceId: String? = null,
    val portForwards: List<MetadataPortForward> = emptyList(),
    val jumpHostId: String? = null,
    val forwardAgent: Boolean = false,
    val isDemo: Boolean = false,
    val identitiesOnly: Boolean = false,
    val persistentSessionEnabled: Boolean = false,
    val startupCommand: String? = null
) {
    fun sortKey(): String = "$name|$id"

    fun toDomain(): Server {
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
            portForwards = portForwards.map { it.toDomain() },
            jumpHostId = jumpHostId,
            forwardAgent = forwardAgent,
            isDemo = isDemo,
            identitiesOnly = identitiesOnly,
            persistentSessionEnabled = persistentSessionEnabled,
            startupCommand = startupCommand
        )
    }
}

@Serializable
data class MetadataKey(
    val name: String,
    val path: String,
    val publicKey: String = "",
    val type: String = "RSA",
    val fingerprint: String = "",
    val lastModifiedMillis: Long = 0L
) {
    fun sortKey(): String = "$name|$path"

    fun toDomain(): SSHKey {
        return SSHKey(
            name = name,
            path = path,
            publicKey = publicKey,
            type = type,
            fingerprint = fingerprint,
            lastModified = Date(lastModifiedMillis)
        )
    }
}

@Serializable
data class MetadataCertificate(
    val id: String,
    val name: String,
    val path: String,
    val principals: List<String> = emptyList(),
    val validAfterMillis: Long? = null,
    val validBeforeMillis: Long? = null,
    val keyId: String = "",
    val caFingerprint: String = "",
    val certificateType: CertificateType = CertificateType.USER
) {
    fun sortKey(): String = "$name|$path|$id"

    fun toDomain(): SSHCertificate {
        return SSHCertificate(
            id = id,
            name = name,
            path = path,
            principals = principals,
            validAfter = validAfterMillis?.let { Date(it) },
            validBefore = validBeforeMillis?.let { Date(it) },
            keyId = keyId,
            caFingerprint = caFingerprint,
            certificateType = certificateType
        )
    }
}

@Serializable
data class MetadataWorkplace(
    val id: String,
    val name: String
) {
    fun sortKey(): String = "$name|$id"

    fun toDomain(): Workplace = Workplace(id = id, name = name)
}

@Serializable
data class MetadataSnippet(
    val id: String,
    val name: String,
    val command: String,
    val createdAtMillis: Long = 0L
) {
    fun sortKey(): String = "$name|$id"

    fun toDomain(): Snippet {
        return Snippet(
            id = id,
            name = name,
            command = command,
            createdAt = Date(createdAtMillis)
        )
    }
}

@Serializable
data class MetadataPortForward(
    val id: String,
    val type: PortForwardType,
    val localPort: Int,
    val remoteHost: String = "localhost",
    val remotePort: Int,
    val enabled: Boolean = true,
    val bindAddress: String = "127.0.0.1"
) {
    fun toDomain(): PortForward {
        return PortForward(
            id = id,
            type = type,
            localPort = localPort,
            remoteHost = remoteHost,
            remotePort = remotePort,
            enabled = enabled,
            bindAddress = bindAddress
        )
    }
}

fun Server.toMetadata(): MetadataServer {
    return MetadataServer(
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
        portForwards = portForwards.map { it.toMetadata() },
        jumpHostId = jumpHostId,
        forwardAgent = forwardAgent,
        isDemo = isDemo,
        identitiesOnly = identitiesOnly,
        persistentSessionEnabled = persistentSessionEnabled,
        startupCommand = startupCommand
    )
}

fun SSHKey.toMetadata(): MetadataKey {
    return MetadataKey(
        name = name,
        path = path,
        publicKey = publicKey,
        type = type,
        fingerprint = fingerprint,
        lastModifiedMillis = lastModified.time
    )
}

fun SSHCertificate.toMetadata(): MetadataCertificate {
    return MetadataCertificate(
        id = id,
        name = name,
        path = path,
        principals = principals,
        validAfterMillis = validAfter?.time,
        validBeforeMillis = validBefore?.time,
        keyId = keyId,
        caFingerprint = caFingerprint,
        certificateType = certificateType
    )
}

fun Workplace.toMetadata(): MetadataWorkplace {
    return MetadataWorkplace(id = id, name = name)
}

fun Snippet.toMetadata(): MetadataSnippet {
    return MetadataSnippet(
        id = id,
        name = name,
        command = command,
        createdAtMillis = createdAt.time
    )
}

fun PortForward.toMetadata(): MetadataPortForward {
    return MetadataPortForward(
        id = id,
        type = type,
        localPort = localPort,
        remoteHost = remoteHost,
        remotePort = remotePort,
        enabled = enabled,
        bindAddress = bindAddress
    )
}

fun MetadataSnapshot.toJson(): String = metadataSnapshotJson.encodeToString(MetadataSnapshot.serializer(), this)

private inline fun <T> mergeById(
    primary: List<T>,
    secondary: List<T>,
    keySelector: (T) -> String
): List<T> {
    val merged = linkedMapOf<String, T>()
    for (item in primary) {
        merged[keySelector(item)] = item
    }
    for (item in secondary) {
        merged[keySelector(item)] = item
    }
    return merged.values.toList()
}
