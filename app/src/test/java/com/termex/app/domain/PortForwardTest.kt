package com.termex.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PortForwardTest {

    @Test
    fun `remote forward display uses imported remote host`() {
        val forward = PortForward(
            type = PortForwardType.REMOTE,
            localPort = 5432,
            remoteHost = "db.internal",
            remotePort = 15432,
            bindAddress = "0.0.0.0"
        )

        assertEquals("R:0.0.0.0:15432 -> db.internal:5432", forward.displayString)
    }
}
