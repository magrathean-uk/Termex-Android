package com.termex.app.core.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IdentityFileBuilderTest {

    @Test
    fun `buildIdentityFiles copies key and cert into cache pair`() {
        val root = createTempDir(prefix = "identity-builder")
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

        val output = IdentityFileBuilder.buildIdentityFiles(
            parentDir = File(root, "out"),
            serverId = "server-123",
            privateKeyPath = keyFile.absolutePath,
            certificatePath = certFile.absolutePath
        )

        assertTrue(output.privateKeyFile.exists())
        assertTrue(output.privateKeyFile.name.contains("server-123"))
        assertEquals(keyFile.readText(), output.privateKeyFile.readText())

        assertTrue(output.certificateFile.exists())
        assertTrue(output.certificateFile.name.contains("server-123"))
        assertEquals(certFile.readText(), output.certificateFile.readText())
    }
}
