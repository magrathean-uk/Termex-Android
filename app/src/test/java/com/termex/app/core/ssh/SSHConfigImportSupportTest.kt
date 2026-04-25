package com.termex.app.core.ssh

import com.termex.app.core.ssh.SSHClient.Companion.shouldAddPasswordIdentity
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.Server
import com.termex.app.domain.SSHCertificate
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
    fun `parser keeps port forwards and server conversion preserves them`() {
        val hosts = SSHConfigParser.parse(
            """
            Host prod
                HostName prod.example.com
                User deploy
                LocalForward 8080 db.internal:5432
                RemoteForward 0.0.0.0:9090 localhost:9091
                DynamicForward 1080
            """.trimIndent()
        )

        assertEquals(1, hosts.size)
        assertEquals(3, hosts.first().portForwards.size)
        assertEquals(
            listOf(
                PortForwardType.LOCAL,
                PortForwardType.REMOTE,
                PortForwardType.DYNAMIC
            ),
            hosts.first().portForwards.map { it.type }
        )

        val servers = SSHConfigParser.toServers(hosts)
        assertEquals(1, servers.size)
        assertEquals(hosts.first().portForwards, servers.first().portForwards)
    }

    @Test
    fun `parser keeps certificate file and server conversion preserves it`() {
        val hosts = SSHConfigParser.parse(
            """
            Host prod
                HostName prod.example.com
                User deploy
                IdentityFile ~/.ssh/work
                CertificateFile ~/.ssh/work-cert.pub
            """.trimIndent()
        )

        assertEquals(1, hosts.size)
        assertEquals("~/.ssh/work-cert.pub", hosts.first().certificateFile)

        val servers = SSHConfigParser.toServers(hosts)
        assertEquals(1, servers.size)
        assertEquals("~/.ssh/work-cert.pub", servers.first().certificatePath)
        assertEquals(com.termex.app.domain.AuthMode.KEY, servers.first().authMode)
    }

    @Test
    fun `matching imported certificate path uses certificate basename`() {
        val certificates = listOf(
            SSHCertificate(name = "work-cert.pub", path = "/data/user/0/com.termex.app/files/certs/work-cert.pub"),
            SSHCertificate(name = "personal-cert.pub", path = "/data/user/0/com.termex.app/files/certs/personal-cert.pub")
        )

        assertEquals(
            "/data/user/0/com.termex.app/files/certs/work-cert.pub",
            SSHConfigParser.findMatchingImportedCertificatePath("~/.ssh/work-cert.pub", certificates)
        )
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
    fun `port forward codec round trips`() {
        val encoded = SSHConfigParser.encodePortForwards(
            listOf(
                PortForward(
                    type = PortForwardType.LOCAL,
                    localPort = 8080,
                    remoteHost = "db.internal",
                    remotePort = 5432
                ),
                PortForward(
                    type = PortForwardType.REMOTE,
                    localPort = 9091,
                    remoteHost = "localhost",
                    remotePort = 9090,
                    bindAddress = "0.0.0.0"
                )
            )
        )

        val decoded = SSHConfigParser.decodePortForwards(encoded)

        assertEquals(2, decoded.size)
        assertEquals(8080, decoded.first().localPort)
        assertEquals("db.internal", decoded.first().remoteHost)
        assertEquals(PortForwardType.REMOTE, decoded[1].type)
        assertEquals("0.0.0.0", decoded[1].bindAddress)
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
