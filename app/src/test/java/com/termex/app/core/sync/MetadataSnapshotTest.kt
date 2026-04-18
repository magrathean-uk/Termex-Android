package com.termex.app.core.sync

import com.termex.app.domain.AuthMode
import com.termex.app.domain.CertificateType
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.SSHCertificate
import com.termex.app.domain.SSHKey
import com.termex.app.domain.Server
import com.termex.app.domain.Snippet
import com.termex.app.domain.Workplace
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataSnapshotTest {
    @Test
    fun `snapshot round trips through json`() {
        val snapshot = MetadataSnapshot.fromDomain(
            servers = listOf(
                Server(
                    id = "server-1",
                    name = "Alpha",
                    hostname = "alpha.example.com",
                    username = "root",
                    authMode = AuthMode.KEY,
                    keyId = "/keys/id_ed25519",
                    portForwards = listOf(
                        PortForward(
                            id = "pf-1",
                            type = PortForwardType.LOCAL,
                            localPort = 8080,
                            remoteHost = "localhost",
                            remotePort = 80
                        )
                    )
                )
            ),
            keys = listOf(
                SSHKey(
                    name = "id_ed25519",
                    path = "/keys/id_ed25519",
                    publicKey = "ssh-ed25519 AAAA",
                    type = "ED25519",
                    fingerprint = "SHA256:abc",
                    lastModified = Date(1_700_000_000_000)
                )
            ),
            certificates = listOf(
                SSHCertificate(
                    id = "cert-1",
                    name = "alpha-cert",
                    path = "/certs/alpha-cert",
                    principals = listOf("root"),
                    validAfter = Date(1_700_000_000_000),
                    validBefore = Date(1_700_000_100_000),
                    keyId = "/keys/id_ed25519",
                    caFingerprint = "SHA256:def",
                    certificateType = CertificateType.USER
                )
            ),
            workplaces = listOf(
                Workplace(id = "work-1", name = "Ops")
            ),
            snippets = listOf(
                Snippet(id = "snip-1", name = "List", command = "ls", createdAt = Date(1_700_000_000_000))
            ),
            createdAtMillis = 1_700_000_000_000
        )

        val restored = MetadataSnapshot.fromJson(snapshot.toJson())

        assertEquals(snapshot, restored)
        assertEquals(2, restored.missingSecretCount())
        assertTrue(restored.servers.single().portForwards.single().bindAddress == "127.0.0.1")
    }

    @Test
    fun `merge prefers remote values`() {
        val local = MetadataSnapshot(
            createdAtMillis = 1,
            servers = listOf(
                MetadataServer(
                    id = "server-1",
                    name = "Local",
                    hostname = "local.example.com",
                    port = 22,
                    username = "root",
                    authMode = AuthMode.KEY
                )
            ),
            snippets = listOf(
                MetadataSnippet(
                    id = "snippet-local",
                    name = "Local Snippet",
                    command = "echo local"
                )
            )
        )
        val remote = MetadataSnapshot(
            createdAtMillis = 2,
            servers = listOf(
                MetadataServer(
                    id = "server-1",
                    name = "Remote",
                    hostname = "remote.example.com",
                    port = 2222,
                    username = "admin",
                    authMode = AuthMode.PASSWORD
                ),
                MetadataServer(
                    id = "server-2",
                    name = "Remote 2",
                    hostname = "remote-2.example.com",
                    port = 22,
                    username = "root",
                    authMode = AuthMode.KEY
                )
            ),
            snippets = listOf(
                MetadataSnippet(
                    id = "snippet-remote",
                    name = "Remote Snippet",
                    command = "echo remote"
                )
            )
        )

        val merged = local.mergeFrom(remote)

        assertEquals("Remote", merged.servers.first { it.id == "server-1" }.name)
        assertEquals(2, merged.servers.size)
        assertEquals(2, merged.snippets.size)
        assertEquals(2, merged.createdAtMillis)
    }

    @Test
    fun `missing secret count includes unresolved refs`() {
        val snapshot = MetadataSnapshot(
            createdAtMillis = 1,
            servers = listOf(
                MetadataServer(
                    id = "server-password",
                    name = "Password",
                    hostname = "password.example.com",
                    port = 22,
                    username = "root",
                    authMode = AuthMode.PASSWORD
                ),
                MetadataServer(
                    id = "server-key",
                    name = "Key",
                    hostname = "key.example.com",
                    port = 22,
                    username = "root",
                    authMode = AuthMode.KEY,
                    keyId = "/missing/key"
                ),
                MetadataServer(
                    id = "server-cert",
                    name = "Cert",
                    hostname = "cert.example.com",
                    port = 22,
                    username = "root",
                    authMode = AuthMode.KEY,
                    certificatePath = "/missing/cert"
                )
            ),
            keys = listOf(
                MetadataKey(
                    name = "id_ed25519",
                    path = "/keys/id_ed25519"
                )
            ),
            certificates = listOf(
                MetadataCertificate(
                    id = "cert-1",
                    name = "alpha-cert",
                    path = "/certs/alpha-cert"
                )
            )
        )

        assertEquals(5, snapshot.missingSecretCount())
    }
}
