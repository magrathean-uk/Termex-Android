package com.termex.app.core.ssh

import com.termex.app.core.ssh.SSHClient.Companion.shouldAddPasswordIdentity
import com.termex.app.domain.Server
import com.termex.app.domain.SSHKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SSHConfigImportSupportTest {

    @Test
    fun `parser keeps identities only and forward agent flags`() {
        val hosts = SSHConfigParser.parse(
            """
            Host prod
                HostName prod.example.com
                User deploy
                IdentityFile ~/.ssh/work
                IdentitiesOnly yes
                ForwardAgent yes
                ProxyJump jump
            """.trimIndent()
        )

        assertEquals(1, hosts.size)
        assertEquals("~/.ssh/work", hosts.first().identityFile)
        assertEquals(true, hosts.first().identitiesOnly)
        assertEquals(true, hosts.first().forwardAgent)
        assertEquals("jump", hosts.first().proxyJump)
    }

    @Test
    fun `matching imported key path uses identity file basename`() {
        val keys = listOf(
            SSHKey(name = "work", path = "/data/user/0/com.termex.app/files/ssh_keys/work"),
            SSHKey(name = "personal", path = "/data/user/0/com.termex.app/files/ssh_keys/personal")
        )

        assertEquals(
            "/data/user/0/com.termex.app/files/ssh_keys/work",
            SSHConfigParser.findMatchingImportedKeyPath("~/.ssh/work", keys)
        )
    }

    @Test
    fun `resolve jump host matches username host and port`() {
        val servers = listOf(
            Server(
                id = "jump-1",
                name = "Jump",
                hostname = "jump.example.com",
                port = 2200,
                username = "deploy"
            ),
            Server(
                id = "jump-2",
                name = "Other",
                hostname = "other.example.com",
                port = 22,
                username = "root"
            )
        )

        assertEquals(
            "jump-1",
            SSHConfigParser.resolveJumpHostId("deploy@jump.example.com:2200", servers)
        )
    }

    @Test
    fun `identities only skips password when key is present`() {
        assertFalse(
            shouldAddPasswordIdentity(
                config = SSHConnectionConfig(
                    hostname = "host",
                    username = "user",
                    password = "secret",
                    identitiesOnly = true
                ),
                hasKey = true,
                hasPassword = true
            )
        )
        assertTrue(
            shouldAddPasswordIdentity(
                config = SSHConnectionConfig(
                    hostname = "host",
                    username = "user",
                    password = "secret",
                    identitiesOnly = true,
                    authPreference = AuthPreference.PASSWORD
                ),
                hasKey = false,
                hasPassword = true
            )
        )
    }
}
