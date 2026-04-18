package com.termex.app.data.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsRedactorTest {

    @Test
    fun `redacts secrets credentials and embedded tokens`() {
        val raw = """
            password=supersecret
            Authorization: Bearer abc.def.ghi
            postgres://alice:supersecret@example.com/db
            private key = /tmp/id_ed25519
        """.trimIndent()

        val redacted = DiagnosticsRedactor.redact(raw).orEmpty()

        assertTrue(redacted.contains("password= [REDACTED]"))
        assertTrue(redacted.contains("Authorization: Bearer [REDACTED]"))
        assertTrue(redacted.contains("postgres://[REDACTED]@example.com/db"))
        assertTrue(redacted.contains("private key= [REDACTED]"))
    }

    @Test
    fun `redacts pem blocks`() {
        val raw = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            abc123
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()

        assertEquals("[REDACTED PEM BLOCK]", DiagnosticsRedactor.redact(raw))
    }
}
