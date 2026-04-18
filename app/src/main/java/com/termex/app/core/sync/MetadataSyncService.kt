package com.termex.app.core.sync

import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.data.prefs.SyncStatus
import com.termex.app.data.repository.CertificateMetadataStore
import com.termex.app.data.repository.KeyMetadataStore
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.SnippetRepository
import com.termex.app.domain.WorkplaceRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class MetadataSyncService @Inject constructor(
    private val serverRepository: ServerRepository,
    private val keyRepository: KeyRepository,
    private val certificateRepository: CertificateRepository,
    private val workplaceRepository: WorkplaceRepository,
    private val snippetRepository: SnippetRepository,
    private val keyMetadataStore: KeyMetadataStore,
    private val certificateMetadataStore: CertificateMetadataStore,
    private val securePasswordStore: SecurePasswordStore
) {
    suspend fun buildSnapshot(
        createdAtMillis: Long = System.currentTimeMillis()
    ): MetadataSnapshot = withContext(Dispatchers.IO) {
        MetadataSnapshot.fromDomain(
            servers = serverRepository.getAllServers().first(),
            keys = keyRepository.getAllKeys().first(),
            certificates = certificateRepository.getAllCertificates().first(),
            workplaces = workplaceRepository.getAllWorkplaces().first(),
            snippets = snippetRepository.getAllSnippets().first(),
            createdAtMillis = createdAtMillis
        )
    }

    suspend fun syncWithGoogleDrive(
        accessToken: String,
        googleAccountEmail: String? = null
    ): SyncStatus = withContext(Dispatchers.IO) {
        val client = GoogleDriveAppDataFolderClient(accessToken = accessToken)
        val local = buildSnapshot()
        val remote = client.readSnapshot()
        val merged = remote?.let(local::mergeFrom) ?: local.normalized()

        applyRemoteSnapshot(merged)
        client.writeSnapshot(merged)

        buildStatus(
            snapshot = merged,
            googleAccountEmail = googleAccountEmail,
            message = "Synced ${merged.servers.size} servers, ${merged.keys.size} keys, ${merged.certificates.size} certificates, ${merged.workplaces.size} workplaces, and ${merged.snippets.size} snippets."
        )
    }

    suspend fun applyRemoteSnapshot(snapshot: MetadataSnapshot) = withContext(Dispatchers.IO) {
        snapshot.workplaces.forEach { remote ->
            val workplace = remote.toDomain()
            if (workplaceRepository.getWorkplace(workplace.id) == null) {
                workplaceRepository.addWorkplace(workplace)
            } else {
                workplaceRepository.updateWorkplace(workplace)
            }
        }

        snapshot.snippets.forEach { remote ->
            snippetRepository.addSnippet(remote.toDomain())
        }

        val keyPlaceholders = snapshot.keys
            .map { it.toDomain() }
            .filter { key -> !File(key.path).exists() }
        keyMetadataStore.replaceAll(keyPlaceholders)

        val certificatePlaceholders = snapshot.certificates
            .map { it.toDomain() }
            .filter { certificate -> !File(certificate.path).exists() }
        certificateMetadataStore.replaceAll(certificatePlaceholders)

        snapshot.servers.forEach { remote ->
            val server = remote.toDomain()
            if (serverRepository.getServer(server.id) == null) {
                serverRepository.addServer(server)
            } else {
                serverRepository.updateServer(server)
            }
        }
    }

    fun buildStatus(
        snapshot: MetadataSnapshot,
        googleAccountEmail: String? = null,
        message: String = "Not synced yet."
    ): SyncStatus {
        return SyncStatus(
            message = message,
            syncedAtMillis = System.currentTimeMillis(),
            missingSecretCount = snapshot.missingSecretCount(),
            googleAccountEmail = googleAccountEmail
        )
    }

    suspend fun findMissingSecretIssues(): List<MissingSecretIssue> = withContext(Dispatchers.IO) {
        serverRepository.getAllServers().first()
            .filterNot { it.isDemo }
            .flatMap { server ->
                buildList {
                    if (server.passwordKeychainID != null && securePasswordStore.getPassword(server.passwordKeychainID) == null) {
                        add(
                            MissingSecretIssue(
                                kind = MissingSecretKind.PASSWORD,
                                label = server.displayName,
                                detail = "Saved password is unavailable on this device.",
                                serverId = server.id
                            )
                        )
                    }
                    if (!server.keyId.isNullOrBlank() && !File(server.keyId).exists()) {
                        add(
                            MissingSecretIssue(
                                kind = MissingSecretKind.PRIVATE_KEY,
                                label = server.displayName,
                                detail = "Configured private key is missing on this device.",
                                serverId = server.id,
                                keyPath = server.keyId
                            )
                        )
                    }
                    if (!server.certificatePath.isNullOrBlank() && !File(server.certificatePath).exists()) {
                        add(
                            MissingSecretIssue(
                                kind = MissingSecretKind.CERTIFICATE,
                                label = server.displayName,
                                detail = "Configured certificate is missing on this device.",
                                serverId = server.id,
                                certificatePath = server.certificatePath
                            )
                        )
                    }
                }
            }
    }
}
