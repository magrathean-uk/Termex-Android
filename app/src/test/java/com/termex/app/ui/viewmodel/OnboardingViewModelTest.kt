package com.termex.app.ui.viewmodel

import android.content.Context
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.domain.AuthMode
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.SSHCertificate
import com.termex.app.domain.SSHKey
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `finish saves configured server with existing jump host and startup command`() = runTest {
        val context = mockk<Context>()
        val serverRepository = mockk<ServerRepository>(relaxed = true)
        val keyRepository = mockk<KeyRepository>()
        val certificateRepository = mockk<CertificateRepository>()
        val passwordStore = mockk<SecurePasswordStore>()

        val existingJump = Server(
            id = "jump-1",
            name = "Bastion",
            hostname = "jump.example.com",
            username = "jump",
            authMode = AuthMode.KEY,
            keyId = "/keys/jump"
        )
        val key = SSHKey(name = "id_ed25519", path = "/keys/id_ed25519", type = "ED25519")
        val certificate = SSHCertificate(name = "id_ed25519-cert.pub", path = "/certs/id_ed25519-cert.pub")

        every { context.filesDir } returns File("/tmp/termex-onboarding")
        every { serverRepository.getAllServers() } returns MutableStateFlow(listOf(existingJump))
        every { keyRepository.getAllKeys() } returns MutableStateFlow(listOf(key))
        every { certificateRepository.getAllCertificates() } returns MutableStateFlow(listOf(certificate))
        every { keyRepository.getKeyPath(any()) } returns "/keys/id_ed25519"
        every { passwordStore.savePasswordForServer(any(), any()) } returns "ignored"

        val viewModel = OnboardingViewModel(
            context = context,
            serverRepository = serverRepository,
            keyRepository = keyRepository,
            certificateRepository = certificateRepository,
            passwordStore = passwordStore
        )

        viewModel.updateHost("prod.example.com")
        viewModel.updatePort("22")
        viewModel.updateUsername("root")
        viewModel.updateServerName("Prod")
        viewModel.updateAuthMode(AuthMode.KEY)
        viewModel.selectKey(IdentityImportTarget.DESTINATION_KEY, key.path)
        viewModel.selectCertificate(IdentityImportTarget.DESTINATION_CERTIFICATE, certificate.path)
        viewModel.updateJumpHostMode(JumpHostMode.EXISTING)
        viewModel.updateExistingJumpHost(existingJump.id)
        viewModel.addPortForward(
            PortForward(
                type = PortForwardType.LOCAL,
                localPort = 8080,
                remoteHost = "127.0.0.1",
                remotePort = 80
            )
        )
        viewModel.updatePersistentSessionEnabled(true)
        viewModel.updateStartupCommand("cd /srv/app")

        viewModel.finish(demoModeActivated = false)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            serverRepository.addServer(
                withArg { server ->
                    assertEquals("Prod", server.name)
                    assertEquals("prod.example.com", server.hostname)
                    assertEquals(AuthMode.KEY, server.authMode)
                    assertEquals(key.path, server.keyId)
                    assertEquals(certificate.path, server.certificatePath)
                    assertEquals(existingJump.id, server.jumpHostId)
                    assertEquals("cd /srv/app", server.startupCommand)
                    assertTrue(server.persistentSessionEnabled)
                    assertEquals(1, server.portForwards.size)
                }
            )
        }
        assertTrue(viewModel.draft.value.completionReady)
    }

    @Test
    fun `finish skips save when no server draft exists`() = runTest {
        val context = mockk<Context>()
        val serverRepository = mockk<ServerRepository>(relaxed = true)
        val keyRepository = mockk<KeyRepository>()
        val certificateRepository = mockk<CertificateRepository>()
        val passwordStore = mockk<SecurePasswordStore>()

        every { context.filesDir } returns File("/tmp/termex-onboarding")
        every { serverRepository.getAllServers() } returns MutableStateFlow(emptyList())
        every { keyRepository.getAllKeys() } returns MutableStateFlow(emptyList())
        every { certificateRepository.getAllCertificates() } returns MutableStateFlow(emptyList())

        val viewModel = OnboardingViewModel(
            context = context,
            serverRepository = serverRepository,
            keyRepository = keyRepository,
            certificateRepository = certificateRepository,
            passwordStore = passwordStore
        )

        viewModel.finish(demoModeActivated = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { serverRepository.addServer(any()) }
        assertTrue(viewModel.draft.value.completionReady)
    }
}
