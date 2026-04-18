package com.termex.app.core.sync

import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.data.repository.CertificateMetadataStore
import com.termex.app.data.repository.KeyMetadataStore
import com.termex.app.domain.AuthMode
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.SnippetRepository
import com.termex.app.domain.WorkplaceRepository
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetadataSyncServiceTest {

    @Test
    fun `find missing secret issues reports password key and certificate gaps`() = runTest {
        val missingKey = File("/tmp/termex-missing-key")
        val missingCertificate = File("/tmp/termex-missing-cert.pub")
        val serverRepository = mockk<ServerRepository>()
        val securePasswordStore = mockk<SecurePasswordStore>()
        val service = MetadataSyncService(
            serverRepository = serverRepository,
            keyRepository = mockk<KeyRepository>(),
            certificateRepository = mockk<CertificateRepository>(),
            workplaceRepository = mockk<WorkplaceRepository>(),
            snippetRepository = mockk<SnippetRepository>(),
            keyMetadataStore = mockk<KeyMetadataStore>(relaxed = true),
            certificateMetadataStore = mockk<CertificateMetadataStore>(relaxed = true),
            securePasswordStore = securePasswordStore
        )

        every { serverRepository.getAllServers() } returns flowOf(
            listOf(
                Server(
                    id = "server-1",
                    name = "Prod",
                    hostname = "prod.example.com",
                    username = "deploy",
                    authMode = AuthMode.KEY,
                    passwordKeychainID = "pw-1",
                    keyId = missingKey.absolutePath,
                    certificatePath = missingCertificate.absolutePath
                )
            )
        )
        every { securePasswordStore.getPassword("pw-1") } returns null

        val issues = service.findMissingSecretIssues()

        assertEquals(3, issues.size)
        assertTrue(issues.any { it.kind == MissingSecretKind.PASSWORD && it.serverId == "server-1" })
        assertTrue(issues.any { it.kind == MissingSecretKind.PRIVATE_KEY && it.keyPath == missingKey.absolutePath })
        assertTrue(issues.any { it.kind == MissingSecretKind.CERTIFICATE && it.certificatePath == missingCertificate.absolutePath })
    }
}
