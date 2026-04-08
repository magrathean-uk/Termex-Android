package com.termex.app.data.local

import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `port forward converter preserves remote bind address`() {
        val original = listOf(
            PortForward(
                id = "pf-1",
                type = PortForwardType.REMOTE,
                localPort = 5432,
                remoteHost = "localhost",
                remotePort = 15432,
                enabled = true,
                bindAddress = "0.0.0.0"
            )
        )

        val json = converters.portForwardsToJson(original)
        val restored = converters.portForwardsFromJson(json)

        assertEquals(1, restored.size)
        assertEquals("0.0.0.0", restored.first().bindAddress)
        assertEquals(PortForwardType.REMOTE, restored.first().type)
    }

    @Test
    fun `port forward converter defaults bind address for legacy payloads`() {
        val legacyJson = """
            [
              {
                "id": "pf-legacy",
                "type": "REMOTE",
                "localPort": 8080,
                "remoteHost": "localhost",
                "remotePort": 80,
                "enabled": true
              }
            ]
        """.trimIndent()

        val restored = converters.portForwardsFromJson(legacyJson)

        assertEquals(1, restored.size)
        assertEquals("127.0.0.1", restored.first().bindAddress)
        assertTrue(restored.first().enabled)
    }
}
