package com.termex.app.core.ssh

import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.junit.Assert.assertEquals
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
        manager.setClient(sshClient)

        val forward = PortForward(
            type = PortForwardType.REMOTE,
            localPort = 5432,
            remoteHost = "localhost",
            remotePort = 15432,
            bindAddress = "0.0.0.0"
        )

        manager.initializeForwards(listOf(forward))
        manager.startForward(forward)

        verify(exactly = 1) {
            sshClient.startRemotePortForwarding(any(), any())
        }
        assertEquals("0.0.0.0", remoteAddress.captured.hostName)
        assertEquals(15432, remoteAddress.captured.port)
        assertEquals("127.0.0.1", localAddress.captured.hostName)
        assertEquals(5432, localAddress.captured.port)
    }
}
