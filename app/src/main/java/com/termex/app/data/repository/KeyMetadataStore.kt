package com.termex.app.data.repository

import android.content.Context
import com.termex.app.domain.SSHKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class StoredKeyMetadata(
    val name: String,
    val path: String,
    val publicKey: String,
    val type: String,
    val fingerprint: String,
    val lastModifiedMillis: Long
)

@Singleton
class KeyMetadataStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val metadataDir = File(context.filesDir, "ssh_key_metadata").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }
    private val changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun changesFlow(): Flow<Unit> = changes

    fun getAll(): List<SSHKey> {
        val files = metadataDir.listFiles()?.sortedBy { it.name } ?: emptyList()
        return files.mapNotNull { file ->
            runCatching {
                json.decodeFromString<StoredKeyMetadata>(file.readText())
            }.getOrNull()?.toDomain()
        }
    }

    fun upsert(key: SSHKey) {
        fileForPath(key.path).writeText(
            json.encodeToString(
                StoredKeyMetadata(
                    name = key.name,
                    path = key.path,
                    publicKey = key.publicKey,
                    type = key.type,
                    fingerprint = key.fingerprint,
                    lastModifiedMillis = key.lastModified.time
                )
            )
        )
        changes.tryEmit(Unit)
    }

    fun replaceAll(keys: List<SSHKey>) {
        val targetFiles = keys.associateBy { fileForPath(it.path).name }
        metadataDir.listFiles()?.forEach { file ->
            if (file.name !in targetFiles) {
                file.delete()
            }
        }
        keys.forEach(::upsert)
        changes.tryEmit(Unit)
    }

    fun deleteByPath(path: String?) {
        if (path.isNullOrBlank()) return
        fileForPath(path).delete()
        changes.tryEmit(Unit)
    }

    private fun fileForPath(path: String): File {
        return File(metadataDir, "${path.sha256Hex()}.json")
    }

    private fun StoredKeyMetadata.toDomain(): SSHKey {
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

internal fun String.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
