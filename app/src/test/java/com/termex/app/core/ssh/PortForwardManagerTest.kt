package com.termex.app.core.ssh

import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortForwardManagerTest {

    @Test
    fun `remote forward uses bind address for remote socket`() {
        val sshClient = mockk<SSHClient>()
        val remoteAddress = slot<SshdSocketAddress>()
        val localAddress = slot<SshdSocketAddress>()

        every {
            sshClient.startRemotePortForwarding(capture(remoteAddress), capture(localAddress))
        } returns SshdSocketAddress("0.0.0.0", 15432)

        val manager = PortForwardManager()
        manager.setClient("server-a", sshClient)

        val forward = PortForward(
            type = PortForwardType.REMOTE,
            localPort = 5432,
            remoteHost = "db.internal",
            remotePort = 15432,
            bindAddress = "0.0.0.0"
        )

        manager.initializeForwards("server-a", listOf(forward))
        manager.startForward("server-a", forward)

        verify(exactly = 1) {
            sshClient.startRemotePortForwarding(any(), any())
        }
        assertEquals("0.0.0.0", remoteAddress.captured.hostName)
        assertEquals(15432, remoteAddress.captured.port)
        assertEquals("db.internal", localAddress.captured.hostName)
        assertEquals(5432, localAddress.captured.port)
    }

    @Test
    fun `dynamic forward honors bind address`() {
        val sshClient = mockk<SSHClient>()
        val localAddress = slot<SshdSocketAddress>()

        every {
            sshClient.startDynamicPortForwarding(capture(localAddress))
        } returns SshdSocketAddress("0.0.0.0", 1080)

        val manager = PortForwardManager()
        manager.setClient("server-a", sshClient)

        val forward = PortForward(
            type = PortForwardType.DYNAMIC,
            localPort = 1080,
            remoteHost = "localhost",
            remotePort = 0,
            bindAddress = "0.0.0.0"
        )

        manager.initializeForwards("server-a", listOf(forward))
        manager.startForward("server-a", forward)

        verify(exactly = 1) { sshClient.startDynamicPortForwarding(any()) }
        assertEquals("0.0.0.0", localAddress.captured.hostName)
        assertEquals(1080, localAddress.captured.port)
    }

    @Test
    fun `stop forward uses same session client`() {
        val clientA = mockk<SSHClient>(relaxed = true)
        val clientB = mockk<SSHClient>(relaxed = true)
        every {
            clientA.startLocalPortForwarding(any(), any())
        } returns SshdSocketAddress("127.0.0.1", 8080)

        val manager = PortForwardManager()
        val forward = PortForward(localPort = 8080, remoteHost = "db.internal", remotePort = 5432)

        manager.setClient("server-a", clientA)
        manager.setClient("server-b", clientB)
        manager.initializeForwards("server-a", listOf(forward))
        manager.startForward("server-a", forward)
        manager.stopForward("server-a", forward.id)

        verify(exactly = 1) { clientA.stopLocalPortForwarding(any()) }
        verify(exactly = 0) { clientB.stopLocalPortForwarding(any()) }
    }

    @Test
    fun `session flow only exposes matching server forwards`() = runBlocking {
        val manager = PortForwardManager()
        val serverAForward = PortForward(id = "a", localPort = 1000, remoteHost = "a", remotePort = 2000)
        val serverBForward = PortForward(id = "b", localPort = 1001, remoteHost = "b", remotePort = 2001)

        manager.initializeForwards("server-a", listOf(serverAForward))
        manager.initializeForwards("server-b", listOf(serverBForward))

        val sessionA = manager.activeForwards("server-a").first()
        val sessionB = manager.activeForwards("server-b").first()

        assertEquals(listOf(serverAForward), sessionA.map { it.config })
        assertEquals(listOf(serverBForward), sessionB.map { it.config })
        assertFalse(sessionA.any { it.config.id == "b" })
        assertTrue(sessionB.any { it.config.id == "b" })
    }
}
