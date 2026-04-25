package com.termex.app.core.transfer

import java.security.SecureRandom
import java.util.ArrayDeque
import java.util.Date
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TermexArchiveTransferTest {
    @Test
    fun `export and import round trip payload`() {
        val payload = samplePayload()
        val exporter = TermexArchiveTransfer.exportArchive(
            payload = payload,
            password = "archive-pass",
            appVersion = "3.2.1",
            createdAt = Date(1_700_000_000_000L),
            secureRandom = fixedRandom(
                byteArrayOf(
                    0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                    0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F
                ),
                byteArrayOf(
                    0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
                    0x28, 0x29, 0x2A, 0x2B
                )
            )
        )

        assertNotNull(exporter.document)
        assertEquals(2, exporter.summary.archiveVersion)
        assertEquals(2, exporter.summary.payloadVersion)
        assertEquals(1, exporter.summary.counts.servers)
        assertEquals(1, exporter.summary.counts.keys)
        assertEquals(1, exporter.summary.counts.certificates)
        assertEquals(1, exporter.summary.counts.knownHosts)
        assertEquals(1, exporter.summary.counts.workplaces)
        assertEquals(exporter.bytes.size, exporter.summary.archiveBytes)

        val imported = TermexArchiveTransfer.importArchive(exporter.bytes, "archive-pass")
        assertPayloadEquals(payload.copy(version = 2), imported.payload)
        assertEquals("Imported 1 server, 1 key, 1 certificate, 1 known host, 1 workplace.", imported.summary.summaryText)

        val envelopeBytes = TermexArchiveTransfer.encodeEnvelope(exporter.document.envelope)
        val envelopeOnly = TermexArchiveTransfer.importArchive(envelopeBytes, "archive-pass")
        assertPayloadEquals(payload.copy(version = 2), envelopeOnly.payload)
    }

    @Test
    fun `wrong password fails`() {
        val exporter = TermexArchiveTransfer.exportArchive(
            payload = samplePayload(),
            password = "right-pass",
            appVersion = "3.2.1",
            createdAt = Date(1_700_000_000_000L),
            secureRandom = fixedRandom(
                ByteArray(16) { 1 },
                ByteArray(12) { 2 }
            )
        )

        assertThrows(TermexArchiveTransferException.InvalidPassword::class.java) {
            TermexArchiveTransfer.importArchive(exporter.bytes, "wrong-pass")
        }
    }

    @Test
    fun `future archive version fails`() {
        val exporter = TermexArchiveTransfer.exportArchive(
            payload = samplePayload(),
            password = "archive-pass",
            appVersion = "3.2.1",
            createdAt = Date(1_700_000_000_000L),
            secureRandom = fixedRandom(ByteArray(16) { 3 }, ByteArray(12) { 4 })
        )

        val document = exporter.document.copy(
            manifest = exporter.document.manifest.copy(archiveVersion = 3)
        )
        val bytes = TermexArchiveTransfer.encodeDocument(document)

        val error = assertThrows(TermexArchiveTransferException.FutureArchiveVersion::class.java) {
            TermexArchiveTransfer.importArchive(bytes, "archive-pass")
        }
        assertEquals(3, error.version)
    }

    @Test
    fun `checksum mismatch fails`() {
        val exporter = TermexArchiveTransfer.exportArchive(
            payload = samplePayload(),
            password = "archive-pass",
            appVersion = "3.2.1",
            createdAt = Date(1_700_000_000_000L),
            secureRandom = fixedRandom(ByteArray(16) { 5 }, ByteArray(12) { 6 })
        )

        val mutated = exporter.document.copy(
            manifest = exporter.document.manifest.copy(
                counts = exporter.document.manifest.counts.copy(servers = 99)
            )
        )
        val bytes = TermexArchiveTransfer.encodeDocument(mutated)

        assertThrows(TermexArchiveTransferException.InvalidArchive::class.java) {
            TermexArchiveTransfer.importArchive(bytes, "archive-pass")
        }
    }

    @Test
    fun `summary text handles empty archive`() {
        val summary = TermexArchiveImportSummary()
        assertEquals("Archive imported.", summary.summaryText)
    }

    @Test
    fun `summary text skips zero buckets and pluralizes counts`() {
        val summary = TermexArchiveImportSummary(
            insertedServers = 1,
            updatedKeys = 2,
            insertedKnownHosts = 1
        )

        assertEquals("Imported 1 server, 2 keys, 1 known host.", summary.summaryText)
    }

    private fun samplePayload(): TermexArchivePayloadV2 {
        val serverId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val keyId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val certificateId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val knownHostId = UUID.fromString("44444444-4444-4444-4444-444444444444")
        val workplaceId = UUID.fromString("55555555-5555-5555-5555-555555555555")
        return TermexArchivePayloadV2(
            version = 2,
            createdAt = Date(1_700_000_000_000L),
            appVersion = "3.2.1",
            servers = listOf(
                TermexArchiveServerV2(
                    id = serverId,
                    name = "Prod",
                    hostname = "prod.example.com",
                    port = 22,
                    username = "deploy",
                    authModeRaw = "password",
                    identitiesOnly = true,
                    portForwards = listOf(
                        TermexArchivePortForwardV2(
                            id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
                            type = "local",
                            bindAddress = "127.0.0.1",
                            bindPort = 8080,
                            targetAddress = "db.internal",
                            targetPort = 5432
                        )
                    ),
                    jumpHostID = null,
                    usePersistentTmux = true,
                    tmuxWorkingDirectory = "/srv/app",
                    startupCommand = "bundle exec rake",
                    preferredKeyRecordIDs = listOf(keyId),
                    preferredCertificateIDs = listOf(certificateId),
                    passwordData = byteArrayOf(0x70, 0x61, 0x73, 0x73)
                )
            ),
            keys = listOf(
                TermexArchiveKeyV2(
                    id = keyId,
                    name = "deploy",
                    keyType = "ed25519",
                    publicKeyData = byteArrayOf(1, 2, 3),
                    fingerprint = "SHA256:abc",
                    comment = "deploy key",
                    certificateIDs = listOf(certificateId),
                    privateKeyData = byteArrayOf(4, 5, 6),
                    passphraseData = byteArrayOf(7, 8, 9)
                )
            ),
            certificates = listOf(
                TermexArchiveCertificateV2(
                    id = certificateId,
                    name = "deploy-cert",
                    certType = "user",
                    publicKeyData = byteArrayOf(9, 8, 7),
                    fingerprint = "SHA256:def",
                    comment = "ssh cert",
                    principals = listOf("deploy", "root"),
                    validAfter = Date(1_600_000_000_000L),
                    validBefore = Date(1_900_000_000_000L),
                    signerKeyID = "ca-1"
                )
            ),
            knownHosts = listOf(
                TermexArchiveKnownHostV2(
                    id = knownHostId,
                    hostname = "prod.example.com",
                    port = 22,
                    keyFingerprint = "SHA256:known",
                    keyType = "ssh-ed25519",
                    createdAt = Date(1_650_000_000_000L)
                )
            ),
            workplaces = listOf(
                TermexArchiveWorkplaceV2(
                    id = workplaceId,
                    name = "Platform",
                    serverIDs = listOf(serverId)
                )
            )
        )
    }

    private fun fixedRandom(vararg chunks: ByteArray): SecureRandom {
        return object : SecureRandom() {
            private val queue = ArrayDeque(chunks.map { it.copyOf() })

            override fun nextBytes(bytes: ByteArray) {
                val next = queue.pollFirst() ?: ByteArray(bytes.size)
                require(next.size >= bytes.size) { "Not enough random bytes queued" }
                next.copyInto(bytes, endIndex = bytes.size)
            }
        }
    }

    private fun assertPayloadEquals(expected: TermexArchivePayloadV2, actual: TermexArchivePayloadV2) {
        assertEquals(expected.version, actual.version)
        assertEquals(expected.createdAt, actual.createdAt)
        assertEquals(expected.appVersion, actual.appVersion)
        assertEquals(expected.knownHosts, actual.knownHosts)
        assertEquals(expected.workplaces, actual.workplaces)

        assertEquals(expected.servers.size, actual.servers.size)
        expected.servers.zip(actual.servers).forEach { (left, right) ->
            assertEquals(left.copy(passwordData = null), right.copy(passwordData = null))
            org.junit.Assert.assertArrayEquals(left.passwordData, right.passwordData)
        }

        assertEquals(expected.keys.size, actual.keys.size)
        expected.keys.zip(actual.keys).forEach { (left, right) ->
            assertEquals(left.copy(publicKeyData = null, privateKeyData = null, passphraseData = null), right.copy(publicKeyData = null, privateKeyData = null, passphraseData = null))
            org.junit.Assert.assertArrayEquals(left.publicKeyData, right.publicKeyData)
            org.junit.Assert.assertArrayEquals(left.privateKeyData, right.privateKeyData)
            org.junit.Assert.assertArrayEquals(left.passphraseData, right.passphraseData)
        }

        assertEquals(expected.certificates.size, actual.certificates.size)
        expected.certificates.zip(actual.certificates).forEach { (left, right) ->
            assertEquals(left.id, right.id)
            assertEquals(left.name, right.name)
            assertEquals(left.certType, right.certType)
            assertEquals(left.fingerprint, right.fingerprint)
            assertEquals(left.comment, right.comment)
            assertEquals(left.principals, right.principals)
            assertEquals(left.validAfter, right.validAfter)
            assertEquals(left.validBefore, right.validBefore)
            assertEquals(left.signerKeyID, right.signerKeyID)
            org.junit.Assert.assertArrayEquals(left.publicKeyData, right.publicKeyData)
        }
    }
}
