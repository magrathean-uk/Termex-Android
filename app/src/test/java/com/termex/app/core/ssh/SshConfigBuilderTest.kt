package com.termex.app.core.ssh

import android.content.Context
import com.termex.app.data.crypto.ResolvedPassword
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.data.prefs.KeepAliveInterval
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SshConfigBuilderTest {

    @Test
    fun `buildConfig copies private key and certificate into cached identity pair`() = runTest {
        val root = createTempDir(prefix = "ssh-config")
        val cacheDir = File(root, "cache").apply { mkdirs() }
        val keyFile = File(root, "id_ed25519").apply {
            writeText(
                """
                -----BEGIN OPENSSH PRIVATE KEY-----
                key-body
                -----END OPENSSH PRIVATE KEY-----
                """.trimIndent()
            )
        }
        val certFile = File(root, "id_ed25519-cert.pub").apply {
            writeText("ssh-ed25519-cert-v01@openssh.com cert-body comment")
        }

        val serverRepository = mockk<ServerRepository>(relaxed = true)
        val passwordStore = mockk<SecurePasswordStore>()
        val userPreferencesRepository = mockk<UserPreferencesRepository>()
        val context = mockk<Context>()

        every { context.cacheDir } returns cacheDir
        every { passwordStore.resolvePassword("server-123", null) } returns ResolvedPassword(null, null)
        every { userPreferencesRepository.keepAliveIntervalFlow } returns flowOf(KeepAliveInterval.SECONDS_30)

        val builder = SshConfigBuilder(
            serverRepository = serverRepository,
            passwordStore = passwordStore,
            userPreferencesRepository = userPreferencesRepository,
            context = context
        )
        val server = Server(
            id = "server-123",
            name = "Prod",
            hostname = "10.0.0.5",
            username = "user3",
            authMode = AuthMode.KEY,
            keyId = keyFile.absolutePath,
            certificatePath = certFile.absolutePath
        )

        val config = builder.buildConfig(server) ?: error("config missing")

        assertEquals(30, config.keepAliveIntervalSeconds)
        assertNull(config.privateKey)
        assertTrue(config.identityPath?.contains("termex-identity-server-123") == true)
        assertTrue(config.certificatePath?.contains("termex-identity-server-123-cert.pub") == true)
        assertEquals(keyFile.readText(), File(config.identityPath!!).readText())
        assertEquals(certFile.readText(), File(config.certificatePath!!).readText())
    }
}
