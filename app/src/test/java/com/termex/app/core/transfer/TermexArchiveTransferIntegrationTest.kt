package com.termex.app.core.transfer

import android.content.Context
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.SSHCertificate
import com.termex.app.domain.SSHKey
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.Workplace
import com.termex.app.domain.WorkplaceRepository
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TermexArchiveTransferIntegrationTest {

    @Test
    fun `import keeps local conflicting known host fingerprint`() = runTest {
        val tempDir = createTempDir(prefix = "termex-archive-")
        val context = mockk<Context>()
        every { context.filesDir } returns tempDir

        val knownHostRepository = InMemoryKnownHostRepository(
            listOf(
                KnownHost(
                    id = "local-known-host",
                    hostname = "prod.example.com",
                    port = 22,
                    keyType = "ssh-ed25519",
                    fingerprint = "SHA256:local",
                    publicKey = "local-public",
                    addedAt = Date(10L),
                    lastSeenAt = Date(10L)
                )
            )
        )

        val transfer = TermexArchiveTransfer(
            context = context,
            serverRepository = InMemoryServerRepository(),
            keyRepository = InMemoryKeyRepository(tempDir),
            certificateRepository = InMemoryCertificateRepository(),
            knownHostRepository = knownHostRepository,
            workplaceRepository = InMemoryWorkplaceRepository(),
            securePasswordStore = mockk<SecurePasswordStore>(relaxed = true)
        )

        val payload = TermexArchivePayloadV2(
            version = 2,
            createdAt = Date(100L),
            appVersion = "1.0.0",
            knownHosts = listOf(
                TermexArchiveKnownHostV2(
                    id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
                    hostname = "prod.example.com",
                    port = 22,
                    keyFingerprint = "SHA256:remote",
                    keyType = "ssh-ed25519",
                    createdAt = Date(20L)
                )
            )
        )
        val archive = TermexArchiveTransfer.exportArchive(
            payload = payload,
            password = "archive-pass",
            appVersion = "1.0.0"
        )

        val result = transfer.importArchive(archive.bytes, "archive-pass")

        assertEquals(0, result.summary.insertedKnownHosts)
        assertEquals(0, result.summary.updatedKnownHosts)
        assertEquals("SHA256:local", knownHostRepository.current().single().fingerprint)
    }
}

private class InMemoryServerRepository : ServerRepository {
    private val state = MutableStateFlow<List<Server>>(emptyList())

    override fun getAllServers(): Flow<List<Server>> = state

    override suspend fun getServer(id: String): Server? = state.value.firstOrNull { it.id == id }

    override suspend fun addServer(server: Server) {
        state.value = state.value + server
    }

    override suspend fun updateServer(server: Server) {
        state.value = state.value.map { if (it.id == server.id) server else it }
    }

    override suspend fun deleteServer(server: Server) {
        state.value = state.value.filterNot { it.id == server.id }
    }
}

private class InMemoryKeyRepository(private val rootDir: File) : KeyRepository {
    private val state = MutableStateFlow<List<SSHKey>>(emptyList())
    private val keysDir = File(rootDir, "keys").apply { mkdirs() }

    override fun getAllKeys(): Flow<List<SSHKey>> = state

    override suspend fun generateKey(name: String, type: String, bits: Int) = Unit

    override suspend fun importKey(name: String, privateKeyContent: String, publicKeyContent: String?) {
        val path = getKeyPath(name)
        File(path).writeText(privateKeyContent)
        publicKeyContent?.let { File("$path.pub").writeText(it) }
        val key = SSHKey(name = name, path = path, publicKey = publicKeyContent.orEmpty(), type = "ED25519")
        state.value = state.value.filterNot { it.name == name } + key
    }

    override suspend fun deleteKey(key: SSHKey) {
        state.value = state.value.filterNot { it.path == key.path }
    }

    override fun getKeyPath(name: String): String = File(keysDir, name).absolutePath
}

private class InMemoryCertificateRepository : CertificateRepository {
    private val state = MutableStateFlow<List<SSHCertificate>>(emptyList())

    override fun getAllCertificates(): Flow<List<SSHCertificate>> = state

    override suspend fun importCertificate(name: String, content: String) {
        state.value = state.value.filterNot { it.name == name } + SSHCertificate(name = name, path = name)
    }

    override suspend fun deleteCertificate(certificate: SSHCertificate) {
        state.value = state.value.filterNot { it.id == certificate.id }
    }

    override fun getCertificatePath(name: String): String = name
}

private class InMemoryKnownHostRepository(initial: List<KnownHost> = emptyList()) : KnownHostRepository {
    private val state = MutableStateFlow(initial)

    override fun getAllKnownHosts(): Flow<List<KnownHost>> = state

    override suspend fun getKnownHost(hostname: String, port: Int): KnownHost? {
        return state.value.firstOrNull { it.hostname == hostname && it.port == port }
    }

    override suspend fun addKnownHost(knownHost: KnownHost) {
        state.value = state.value + knownHost
    }

    override suspend fun updateKnownHost(knownHost: KnownHost) {
        state.value = state.value.map { if (it.id == knownHost.id) knownHost else it }
    }

    override suspend fun deleteKnownHost(knownHost: KnownHost) {
        state.value = state.value.filterNot { it.id == knownHost.id }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }

    fun current(): List<KnownHost> = state.value
}

private class InMemoryWorkplaceRepository : WorkplaceRepository {
    private val state = MutableStateFlow<List<Workplace>>(emptyList())

    override fun getAllWorkplaces(): Flow<List<Workplace>> = state

    override suspend fun getWorkplace(id: String): Workplace? = state.value.firstOrNull { it.id == id }

    override suspend fun addWorkplace(workplace: Workplace) {
        state.value = state.value + workplace
    }

    override suspend fun updateWorkplace(workplace: Workplace) {
        state.value = state.value.map { if (it.id == workplace.id) workplace else it }
    }

    override suspend fun deleteWorkplace(workplace: Workplace) {
        state.value = state.value.filterNot { it.id == workplace.id }
    }

    override fun getServersForWorkplace(workplaceId: String): Flow<List<Server>> = flowOf(emptyList())
}
