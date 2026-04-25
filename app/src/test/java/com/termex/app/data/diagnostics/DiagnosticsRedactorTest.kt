package com.termex.app.data.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `redacts env style assignments with quoted values`() {
        val raw = """
            PGPASSWORD='pg secret'
            AWS_SECRET_ACCESS_KEY="aws-secret"
            API_KEY=abc123
            TERM=xterm-256color
        """.trimIndent()

        val redacted = DiagnosticsRedactor.redact(raw).orEmpty()

        assertTrue(redacted.contains("PGPASSWORD= [REDACTED]"))
        assertTrue(redacted.contains("AWS_SECRET_ACCESS_KEY= [REDACTED]"))
        assertTrue(redacted.contains("API_KEY= [REDACTED]"))
        assertTrue(redacted.contains("TERM=xterm-256color"))
        assertFalse(redacted.contains("pg secret"))
        assertFalse(redacted.contains("aws-secret"))
        assertFalse(redacted.contains("abc123"))
    }

    @Test
    fun `redacts bearer cookies and json secrets`() {
        val raw = """
            Cookie: session=abcdef; csrftoken=123456
            Set-Cookie: refresh=verysecret; HttpOnly
            {"token":"json-token","private_key":"inline-key","host":"example.com"}
        """.trimIndent()

        val redacted = DiagnosticsRedactor.redact(raw).orEmpty()

        assertTrue(redacted.contains("Cookie: [REDACTED]"))
        assertTrue(redacted.contains("Set-Cookie: [REDACTED]"))
        assertTrue(redacted.contains("\"token\":\"[REDACTED]\""))
        assertTrue(redacted.contains("\"private_key\":\"[REDACTED]\""))
        assertTrue(redacted.contains("\"host\":\"example.com\""))
        assertFalse(redacted.contains("json-token"))
        assertFalse(redacted.contains("inline-key"))
    }

    @Test
    fun `redacts command line secret flags and sshpass`() {
        val raw = """
            curl --token abc123 https://example.com
            deploy --password='hidden value'
            sshpass -p hunter2 ssh root@example.com
        """.trimIndent()

        val redacted = DiagnosticsRedactor.redact(raw).orEmpty()

        assertTrue(redacted.contains("--token [REDACTED]"))
        assertTrue(redacted.contains("--password=[REDACTED]"))
        assertTrue(redacted.contains("sshpass -p [REDACTED]"))
        assertFalse(redacted.contains("abc123"))
        assertFalse(redacted.contains("hidden value"))
        assertFalse(redacted.contains("hunter2"))
    }

    @Test
    fun `redacts inline ssh config secret-bearing lines`() {
        val raw = """
            Host prod
              HostName prod.example.com
              User root
              IdentityFile /home/g/.ssh/id_ed25519
              CertificateFile /home/g/.ssh/id_ed25519-cert.pub
              ProxyCommand sshpass -p jumpsecret ssh -W %h:%p bastion
        """.trimIndent()

        val redacted = DiagnosticsRedactor.redact(raw).orEmpty()

        assertTrue(redacted.contains("HostName prod.example.com"))
        assertTrue(redacted.contains("User root"))
        assertTrue(redacted.contains("IdentityFile [REDACTED]"))
        assertTrue(redacted.contains("CertificateFile [REDACTED]"))
        assertTrue(redacted.contains("ProxyCommand [REDACTED]"))
        assertFalse(redacted.contains("id_ed25519"))
        assertFalse(redacted.contains("jumpsecret"))
    }

    @Test
    fun `redacts multi line command output without removing safe context`() {
        val raw = """
            ${'$'} export GITHUB_TOKEN=ghp_secret
            connected to prod.example.com
            mysql://root:rootpass@db.example.com/app
            done
        """.trimIndent()

        val redacted = DiagnosticsRedactor.redact(raw).orEmpty()

        assertTrue(redacted.contains("connected to prod.example.com"))
        assertTrue(redacted.contains("mysql://[REDACTED]@db.example.com/app"))
        assertTrue(redacted.contains("GITHUB_TOKEN= [REDACTED]"))
        assertFalse(redacted.contains("ghp_secret"))
        assertFalse(redacted.contains("rootpass"))
    }
}
