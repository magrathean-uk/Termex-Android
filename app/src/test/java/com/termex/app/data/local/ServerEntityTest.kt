package com.termex.app.data.local

import com.termex.app.domain.AuthMode
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.Server
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerEntityTest {

    @Test
    fun `server round trip preserves certificate and tmux fields`() {
        val server = Server(
            id = "server-123",
            name = "Prod",
            hostname = "10.0.0.5",
            port = 2222,
            username = "user3",
            authMode = AuthMode.KEY,
            keyId = "/keys/id_ed25519",
            certificatePath = "/certs/id_ed25519-cert.pub",
            persistentSessionEnabled = true,
            startupCommand = "cd /srv/app",
            portForwards = listOf(
                PortForward(
                    id = "pf-1",
                    type = PortForwardType.LOCAL,
                    localPort = 8080,
                    remoteHost = "127.0.0.1",
                    remotePort = 80
                )
            )
        )

        val restored = server.toEntity().toDomain()

        assertEquals(server.certificatePath, restored.certificatePath)
        assertTrue(restored.persistentSessionEnabled)
        assertEquals(server.startupCommand, restored.startupCommand)
        assertEquals(server.portForwards, restored.portForwards)
    }
}
