package com.termex.app.core.transfer

import android.content.Context
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.domain.AuthMode
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.Workplace
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class TermexArchiveTransfer @Inject constructor(
    @ApplicationContext context: Context,
    private val serverRepository: ServerRepository,
    private val keyRepository: KeyRepository,
    private val certificateRepository: CertificateRepository,
    private val knownHostRepository: KnownHostRepository,
    private val workplaceRepository: WorkplaceRepository,
    private val securePasswordStore: SecurePasswordStore
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val mutex = Mutex()
    private val certsDir = File(context.filesDir, "ssh_certs").apply { mkdirs() }

    suspend fun exportArchive(password: String): TermexArchiveExportResult = mutex.withLock {
        if (password.isBlank()) {
            throw TermexArchiveTransferException.EmptyPassword
        }

        val payload = buildPayload()
        val plaintext = json.encodeToString(payload).toByteArray(StandardCharsets.UTF_8)
        if (plaintext.size > MAX_ARCHIVE_BYTES) {
            throw TermexArchiveTransferException.ArchiveTooLarge
        }

        val envelope = encrypt(plaintext, password)
        val manifest = makeManifest(payload, plaintext)
        val document = TermexArchiveDocumentV2(manifest = manifest, envelope = envelope)
        val bytes = json.encodeToString(document).toByteArray(StandardCharsets.UTF_8)
        return TermexArchiveExportResult(
            document = document,
            bytes = bytes,
            summary = TermexArchiveExportSummary(
                archiveVersion = ARCHIVE_VERSION,
                payloadVersion = PAYLOAD_VERSION,
                counts = manifest.counts,
                archiveBytes = bytes.size
            )
        )
    }

    suspend fun importArchive(bytes: ByteArray, password: String): TermexArchiveImportResult = mutex.withLock {
        if (password.isBlank()) {
            throw TermexArchiveTransferException.EmptyPassword
        }
        if (bytes.size > MAX_ARCHIVE_BYTES) {
            throw TermexArchiveTransferException.ArchiveTooLarge
        }

        val payload = decodePayload(bytes, password)
        val summary = applyPayload(payload)
        return TermexArchiveImportResult(payload = payload, summary = summary)
    }

    private suspend fun buildPayload(): TermexArchivePayloadV2 {
        val servers = serverRepository.getAllServers().first().sortedBy { it.displayName.lowercase() }
        val keys = keyRepository.getAllKeys().first().sortedBy { it.name.lowercase() }
        val certificates = certificateRepository.getAllCertificates().first().sortedBy { it.name.lowercase() }
        val knownHosts = knownHostRepository.getAllKnownHosts().first()
        val workplaces = workplaceRepository.getAllWorkplaces().first().sortedBy { it.name.lowercase() }

        val keyIdsByPath = keys.associate { it.path to stableUuid("key:${it.name}:${it.path}") }
        val certIdsByPath = certificates.associate { it.path to stableUuid("cert:${it.name}:${it.path}") }

        val archivedServers = servers.map { server ->
            TermexArchiveServerV2(
                id = server.id.toUuidOrStable("server:${server.displayName}:${server.hostname}:${server.port}"),
                name = server.name,
                hostname = server.hostname,
                port = server.port,
                username = server.username,
                authModeRaw = server.authMode.name.lowercase(),
                identitiesOnly = server.identitiesOnly,
                portForwards = server.portForwards.map { forward ->
                    TermexArchivePortForwardV2(
                        id = forward.id.toUuidOrStable("forward:${server.id}:${forward.displayString}"),
                        type = forward.type.name.lowercase(),
                        bindAddress = forward.bindAddress,
                        bindPort = forward.localPort,
                        targetAddress = forward.remoteHost,
                        targetPort = forward.remotePort
                    )
                },
                jumpHostID = server.jumpHostId?.toUuidOrStable("jump:${server.jumpHostId}"),
                usePersistentTmux = server.persistentSessionEnabled,
                startupCommand = server.startupCommand,
                preferredKeyRecordIDs = listOfNotNull(server.keyId?.let(keyIdsByPath::get)),
                preferredCertificateIDs = listOfNotNull(server.certificatePath?.let(certIdsByPath::get)),
                passwordData = server.passwordKeychainID
                    ?.let(securePasswordStore::getPassword)
                    ?.toByteArray(StandardCharsets.UTF_8)
            )
        }

        val archivedKeys = keys.map { key ->
            TermexArchiveKeyV2(
                id = keyIdsByPath.getValue(key.path),
                name = key.name,
                keyType = key.type,
                publicKeyData = key.publicKey.takeIf { it.isNotBlank() }?.toByteArray(StandardCharsets.UTF_8),
                fingerprint = key.fingerprint.takeIf { it.isNotBlank() },
                comment = key.name,
                privateKeyData = runCatching {
                    File(key.path).takeIf(File::exists)
                        ?.readText()
                        ?.toByteArray(StandardCharsets.UTF_8)
                }.getOrNull()
            )
        }

        val archivedCertificates = certificates.map { certificate ->
            TermexArchiveCertificateV2(
                id = certIdsByPath.getValue(certificate.path),
                name = certificate.name,
                certType = certificate.certificateType.name.lowercase(),
                publicKeyData = File(certificate.path)
                    .takeIf(File::exists)
                    ?.readText()
                    ?.toByteArray(StandardCharsets.UTF_8)
                    ?: ByteArray(0),
                fingerprint = certificate.caFingerprint.takeIf { it.isNotBlank() },
                comment = certificate.name,
                principals = certificate.principals,
                validAfter = certificate.validAfter,
                validBefore = certificate.validBefore,
                signerKeyID = certificate.keyId.takeIf { it.isNotBlank() }
            )
        }

        val archivedKnownHosts = knownHosts
            .groupBy { "${it.hostname}:${it.port}" }
            .values
            .mapNotNull { records -> records.maxByOrNull { it.addedAt.time } }
            .sortedWith(compareBy({ it.hostname.lowercase() }, { it.port }, { it.id }))
            .map { host ->
                TermexArchiveKnownHostV2(
                    id = host.id.toUuidOrStable("host:${host.hostname}:${host.port}"),
                    hostname = host.hostname,
                    port = host.port,
                    keyFingerprint = host.fingerprint,
                    keyType = host.keyType,
                    createdAt = host.addedAt
                )
            }

        val serverIdsByWorkplace = servers.groupBy { it.workplaceId }
        val archivedWorkplaces = workplaces.map { workplace ->
            TermexArchiveWorkplaceV2(
                id = workplace.id.toUuidOrStable("workplace:${workplace.name}:${workplace.id}"),
                name = workplace.name,
                serverIDs = serverIdsByWorkplace[workplace.id].orEmpty()
                    .map { it.id.toUuidOrStable("server:${it.id}") }
                    .sortedBy(UUID::toString)
            )
        }

        return TermexArchivePayloadV2(
            version = PAYLOAD_VERSION,
            createdAt = Date(),
            appVersion = "1.0.0",
            servers = archivedServers,
            keys = archivedKeys,
            certificates = archivedCertificates,
            knownHosts = archivedKnownHosts,
            workplaces = archivedWorkplaces
        )
    }

    private fun makeManifest(payload: TermexArchivePayloadV2, plaintext: ByteArray): TermexArchiveManifestV2 {
        return TermexArchiveManifestV2(
            appVersion = payload.appVersion,
            archiveVersion = ARCHIVE_VERSION,
            createdAt = Date(),
            counts = TermexArchiveCountsV2(
                servers = payload.servers.size,
                keys = payload.keys.size,
                certificates = payload.certificates.size,
                knownHosts = payload.knownHosts.size,
                workplaces = payload.workplaces.size
            ),
            checksum = TermexArchiveChecksumV2(
                algorithm = "sha256",
                payloadDigest = MessageDigest.getInstance("SHA-256").digest(plaintext)
            )
        )
    }

    private fun decodePayload(bytes: ByteArray, password: String): TermexArchivePayloadV2 {
        val text = bytes.toString(StandardCharsets.UTF_8)
        val document = runCatching { json.decodeFromString<TermexArchiveDocumentV2>(text) }.getOrNull()
        if (document != null) {
            if (document.manifest.archiveVersion > ARCHIVE_VERSION) {
                throw TermexArchiveTransferException.FutureArchiveVersion(document.manifest.archiveVersion)
            }
            val plaintext = decrypt(document.envelope, password)
            val payload = json.decodeFromString<TermexArchivePayloadV2>(plaintext.toString(StandardCharsets.UTF_8))
            validateManifest(document.manifest, payload, plaintext)
            return payload
        }

        val legacyEnvelope = runCatching { json.decodeFromString<TermexArchiveEnvelopeV2>(text) }.getOrNull()
            ?: throw TermexArchiveTransferException.InvalidArchive
        if (legacyEnvelope.version > ARCHIVE_VERSION) {
            throw TermexArchiveTransferException.FutureArchiveVersion(legacyEnvelope.version)
        }
        val plaintext = decrypt(legacyEnvelope, password)
        val payload = json.decodeFromString<TermexArchivePayloadV2>(plaintext.toString(StandardCharsets.UTF_8))
        if (payload.version > PAYLOAD_VERSION) {
            throw TermexArchiveTransferException.FutureArchiveVersion(payload.version)
        }
        return payload
    }

    private fun validateManifest(
        manifest: TermexArchiveManifestV2,
        payload: TermexArchivePayloadV2,
        plaintext: ByteArray
    ) {
        if (manifest.checksum.algorithm.lowercase() != "sha256") {
            throw TermexArchiveTransferException.InvalidArchive
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(plaintext)
        if (!digest.contentEquals(manifest.checksum.payloadDigest)) {
            throw TermexArchiveTransferException.InvalidArchive
        }
        val counts = TermexArchiveCountsV2(
            servers = payload.servers.size,
            keys = payload.keys.size,
            certificates = payload.certificates.size,
            knownHosts = payload.knownHosts.size,
            workplaces = payload.workplaces.size
        )
        if (counts != manifest.counts) {
            throw TermexArchiveTransferException.InvalidArchive
        }
        if (payload.version > PAYLOAD_VERSION) {
            throw TermexArchiveTransferException.FutureArchiveVersion(payload.version)
        }
    }

    private suspend fun applyPayload(payload: TermexArchivePayloadV2): TermexArchiveImportSummary {
        val summary = TermexArchiveImportSummary()

        val existingServers = serverRepository.getAllServers().first().associateBy { it.id }.toMutableMap()
        val existingKeys = keyRepository.getAllKeys().first().associateBy { it.name }.toMutableMap()
        val existingCertificates = certificateRepository.getAllCertificates().first().associateBy { it.name }.toMutableMap()
        val existingKnownHostsById = knownHostRepository.getAllKnownHosts().first().associateBy { it.id }.toMutableMap()
        val existingKnownHostsByEndpoint = existingKnownHostsById.values
            .associateBy { "${it.hostname}:${it.port}" }
            .toMutableMap()
        val existingWorkplaces = workplaceRepository.getAllWorkplaces().first().associateBy { it.id }.toMutableMap()

        val keyPathByArchiveId = mutableMapOf<UUID, String>()
        payload.keys.forEach { archivedKey ->
            val fileName = sanitizeFileName(archivedKey.name.ifBlank { archivedKey.id.toString() })
            archivedKey.privateKeyData?.let { privateKeyBytes ->
                keyRepository.importKey(
                    name = fileName,
                    privateKeyContent = privateKeyBytes.toString(StandardCharsets.UTF_8),
                    publicKeyContent = archivedKey.publicKeyData?.toString(StandardCharsets.UTF_8)
                )
            }
            val path = keyRepository.getKeyPath(fileName)
            keyPathByArchiveId[archivedKey.id] = path
            if (existingKeys.containsKey(fileName)) {
                summary.updatedKeys += 1
            } else {
                summary.insertedKeys += 1
            }
        }

        val certificatePathByArchiveId = mutableMapOf<UUID, String>()
        payload.certificates.forEach { archivedCertificate ->
            val fileName = sanitizeFileName(archivedCertificate.name.ifBlank { archivedCertificate.id.toString() })
            if (archivedCertificate.publicKeyData.isNotEmpty()) {
                certificateRepository.importCertificate(
                    name = fileName,
                    content = archivedCertificate.publicKeyData.toString(StandardCharsets.UTF_8)
                )
            }
            val path = File(certsDir, fileName).absolutePath
            certificatePathByArchiveId[archivedCertificate.id] = path
            if (existingCertificates.containsKey(fileName)) {
                summary.updatedCertificates += 1
            } else {
                summary.insertedCertificates += 1
            }
        }

        payload.workplaces.forEach { archivedWorkplace ->
            val workplace = Workplace(
                id = archivedWorkplace.id.toString(),
                name = archivedWorkplace.name
            )
            if (existingWorkplaces.containsKey(workplace.id)) {
                workplaceRepository.updateWorkplace(workplace)
                summary.updatedWorkplaces += 1
            } else {
                workplaceRepository.addWorkplace(workplace)
                summary.insertedWorkplaces += 1
            }
            existingWorkplaces[workplace.id] = workplace
        }

        val workplaceIdByServerId = payload.workplaces
            .flatMap { workplace -> workplace.serverIDs.map { serverId -> serverId.toString() to workplace.id.toString() } }
            .toMap()

        payload.servers.forEach { archivedServer ->
            val passwordKeychainId = archivedServer.passwordData
                ?.toString(StandardCharsets.UTF_8)
                ?.takeIf { it.isNotBlank() }
                ?.let { password -> securePasswordStore.savePasswordForServer(archivedServer.id.toString(), password) }
            val server = Server(
                id = archivedServer.id.toString(),
                name = archivedServer.name,
                hostname = archivedServer.hostname,
                port = archivedServer.port,
                username = archivedServer.username,
                authMode = archivedServer.authModeRaw.toAuthMode(),
                passwordKeychainID = passwordKeychainId,
                keyId = archivedServer.preferredKeyRecordIDs.firstNotNullOfOrNull(keyPathByArchiveId::get),
                certificatePath = archivedServer.preferredCertificateIDs.firstNotNullOfOrNull(certificatePathByArchiveId::get),
                workplaceId = workplaceIdByServerId[archivedServer.id.toString()],
                portForwards = archivedServer.portForwards.map { forward ->
                    com.termex.app.domain.PortForward(
                        id = forward.id.toString(),
                        type = forward.type.toArchivePortForwardType(),
                        localPort = forward.bindPort,
                        remoteHost = forward.targetAddress,
                        remotePort = forward.targetPort,
                        bindAddress = forward.bindAddress
                    )
                },
                jumpHostId = archivedServer.jumpHostID?.toString(),
                identitiesOnly = archivedServer.identitiesOnly,
                persistentSessionEnabled = archivedServer.usePersistentTmux,
                startupCommand = archivedServer.startupCommand
            )
            if (existingServers.containsKey(server.id)) {
                serverRepository.updateServer(server)
                summary.updatedServers += 1
            } else {
                serverRepository.addServer(server)
                summary.insertedServers += 1
            }
            existingServers[server.id] = server
        }

        payload.knownHosts
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
            .forEach { archivedKnownHost ->
                val endpointKey = "${archivedKnownHost.hostname}:${archivedKnownHost.port}"
                val existingById = existingKnownHostsById[archivedKnownHost.id.toString()]
                val existingByEndpoint = existingKnownHostsByEndpoint[endpointKey]
                val target = when {
                    existingById != null -> existingById
                    existingByEndpoint == null -> null
                    existingByEndpoint.fingerprint == archivedKnownHost.keyFingerprint -> existingByEndpoint
                    else -> return@forEach
                }

                val knownHost = KnownHost(
                    id = target?.id ?: archivedKnownHost.id.toString(),
                    hostname = archivedKnownHost.hostname,
                    port = archivedKnownHost.port,
                    keyType = archivedKnownHost.keyType,
                    fingerprint = archivedKnownHost.keyFingerprint,
                    publicKey = target?.publicKey ?: "",
                    addedAt = archivedKnownHost.createdAt,
                    lastSeenAt = archivedKnownHost.createdAt
                )
                if (target == null) {
                    knownHostRepository.addKnownHost(knownHost)
                    summary.insertedKnownHosts += 1
                } else {
                    knownHostRepository.updateKnownHost(knownHost)
                    summary.updatedKnownHosts += 1
                }
                existingKnownHostsById[knownHost.id] = knownHost
                existingKnownHostsByEndpoint[endpointKey] = knownHost
            }

        return summary
    }

    private fun encrypt(plaintext: ByteArray, password: String): TermexArchiveEnvelopeV2 {
        val salt = ByteArray(16).also(random::nextBytes)
        val key = deriveKey(password, salt, PBKDF2_ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val combined = cipher.iv + cipher.doFinal(plaintext)
        return TermexArchiveEnvelopeV2(
            version = PAYLOAD_VERSION,
            createdAt = Date(),
            salt = salt,
            iterations = PBKDF2_ITERATIONS,
            sealedBox = combined
        )
    }

    private fun decrypt(envelope: TermexArchiveEnvelopeV2, password: String): ByteArray {
        if (envelope.iterations !in MIN_PBKDF2_ITERATIONS..MAX_PBKDF2_ITERATIONS) {
            throw TermexArchiveTransferException.UnsupportedSecurityParameters
        }
        if (envelope.sealedBox.size <= 12) {
            throw TermexArchiveTransferException.InvalidArchive
        }
        val iv = envelope.sealedBox.copyOfRange(0, 12)
        val ciphertext = envelope.sealedBox.copyOfRange(12, envelope.sealedBox.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        return try {
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(deriveKey(password, envelope.salt, envelope.iterations), "AES"),
                GCMParameterSpec(128, iv)
            )
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            throw TermexArchiveTransferException.InvalidPassword
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun sanitizeFileName(name: String): String {
        return File(name).name.replace("..", "").ifBlank { stableUuid("file:$name").toString() }
    }

    companion object {
        private const val ARCHIVE_VERSION = 2
        private const val PAYLOAD_VERSION = 2
        private const val PBKDF2_ITERATIONS = 200_000
        private const val MIN_PBKDF2_ITERATIONS = 50_000
        private const val MAX_PBKDF2_ITERATIONS = 500_000
        private const val MAX_ARCHIVE_BYTES = 8 * 1024 * 1024
        private val random = SecureRandom()
        private val archiveJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        fun exportArchive(
            payload: TermexArchivePayloadV2,
            password: String,
            appVersion: String = payload.appVersion,
            createdAt: Date = Date(),
            secureRandom: SecureRandom = random
        ): TermexArchiveExportResult {
            if (password.isBlank()) {
                throw TermexArchiveTransferException.EmptyPassword
            }

            val normalizedPayload = payload.copy(
                version = PAYLOAD_VERSION,
                createdAt = createdAt,
                appVersion = appVersion
            )
            val plaintext = archiveJson.encodeToString(normalizedPayload).toByteArray(StandardCharsets.UTF_8)
            if (plaintext.size > MAX_ARCHIVE_BYTES) {
                throw TermexArchiveTransferException.ArchiveTooLarge
            }

            val envelope = encryptDocument(plaintext, password, createdAt, secureRandom)
            val manifest = TermexArchiveManifestV2(
                appVersion = appVersion,
                archiveVersion = ARCHIVE_VERSION,
                createdAt = createdAt,
                counts = countsFor(normalizedPayload),
                checksum = TermexArchiveChecksumV2(
                    algorithm = "sha256",
                    payloadDigest = MessageDigest.getInstance("SHA-256").digest(plaintext)
                )
            )
            val document = TermexArchiveDocumentV2(manifest = manifest, envelope = envelope)
            val bytes = encodeDocument(document)
            return TermexArchiveExportResult(
                document = document,
                bytes = bytes,
                summary = TermexArchiveExportSummary(
                    archiveVersion = ARCHIVE_VERSION,
                    payloadVersion = PAYLOAD_VERSION,
                    counts = manifest.counts,
                    archiveBytes = bytes.size
                )
            )
        }

        fun importArchive(bytes: ByteArray, password: String): TermexArchiveImportResult {
            if (password.isBlank()) {
                throw TermexArchiveTransferException.EmptyPassword
            }
            if (bytes.size > MAX_ARCHIVE_BYTES) {
                throw TermexArchiveTransferException.ArchiveTooLarge
            }

            val payload = decodePayloadDocument(bytes, password)
            return TermexArchiveImportResult(
                payload = payload,
                summary = TermexArchiveImportSummary(
                    insertedServers = payload.servers.size,
                    insertedKeys = payload.keys.size,
                    insertedCertificates = payload.certificates.size,
                    insertedKnownHosts = payload.knownHosts.size,
                    insertedWorkplaces = payload.workplaces.size
                )
            )
        }

        fun encodeDocument(document: TermexArchiveDocumentV2): ByteArray {
            return archiveJson.encodeToString(document).toByteArray(StandardCharsets.UTF_8)
        }

        fun encodeEnvelope(envelope: TermexArchiveEnvelopeV2): ByteArray {
            return archiveJson.encodeToString(envelope).toByteArray(StandardCharsets.UTF_8)
        }

        private fun decodePayloadDocument(bytes: ByteArray, password: String): TermexArchivePayloadV2 {
            val text = bytes.toString(StandardCharsets.UTF_8)
            val document = runCatching {
                archiveJson.decodeFromString<TermexArchiveDocumentV2>(text)
            }.getOrNull()
            if (document != null) {
                if (document.manifest.archiveVersion > ARCHIVE_VERSION) {
                    throw TermexArchiveTransferException.FutureArchiveVersion(document.manifest.archiveVersion)
                }
                val plaintext = decryptDocument(document.envelope, password)
                val payload = archiveJson.decodeFromString<TermexArchivePayloadV2>(plaintext.toString(StandardCharsets.UTF_8))
                validateManifest(document.manifest, payload, plaintext)
                return payload
            }

            val envelope = runCatching {
                archiveJson.decodeFromString<TermexArchiveEnvelopeV2>(text)
            }.getOrNull() ?: throw TermexArchiveTransferException.InvalidArchive
            if (envelope.version > ARCHIVE_VERSION) {
                throw TermexArchiveTransferException.FutureArchiveVersion(envelope.version)
            }
            val plaintext = decryptDocument(envelope, password)
            val payload = archiveJson.decodeFromString<TermexArchivePayloadV2>(plaintext.toString(StandardCharsets.UTF_8))
            if (payload.version > PAYLOAD_VERSION) {
                throw TermexArchiveTransferException.FutureArchiveVersion(payload.version)
            }
            return payload
        }

        private fun countsFor(payload: TermexArchivePayloadV2): TermexArchiveCountsV2 {
            return TermexArchiveCountsV2(
                servers = payload.servers.size,
                keys = payload.keys.size,
                certificates = payload.certificates.size,
                knownHosts = payload.knownHosts.size,
                workplaces = payload.workplaces.size
            )
        }

        private fun validateManifest(
            manifest: TermexArchiveManifestV2,
            payload: TermexArchivePayloadV2,
            plaintext: ByteArray
        ) {
            if (manifest.checksum.algorithm.lowercase() != "sha256") {
                throw TermexArchiveTransferException.InvalidArchive
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(plaintext)
            if (!digest.contentEquals(manifest.checksum.payloadDigest)) {
                throw TermexArchiveTransferException.InvalidArchive
            }
            if (manifest.counts != countsFor(payload)) {
                throw TermexArchiveTransferException.InvalidArchive
            }
            if (payload.version > PAYLOAD_VERSION) {
                throw TermexArchiveTransferException.FutureArchiveVersion(payload.version)
            }
        }

        private fun encryptDocument(
            plaintext: ByteArray,
            password: String,
            createdAt: Date,
            secureRandom: SecureRandom
        ): TermexArchiveEnvelopeV2 {
            val salt = ByteArray(16).also(secureRandom::nextBytes)
            val key = deriveArchiveKey(password, salt, PBKDF2_ITERATIONS)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12).also(secureRandom::nextBytes)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            val combined = iv + cipher.doFinal(plaintext)
            return TermexArchiveEnvelopeV2(
                version = PAYLOAD_VERSION,
                createdAt = createdAt,
                salt = salt,
                iterations = PBKDF2_ITERATIONS,
                sealedBox = combined
            )
        }

        private fun decryptDocument(envelope: TermexArchiveEnvelopeV2, password: String): ByteArray {
            if (envelope.iterations !in MIN_PBKDF2_ITERATIONS..MAX_PBKDF2_ITERATIONS) {
                throw TermexArchiveTransferException.UnsupportedSecurityParameters
            }
            if (envelope.sealedBox.size <= 12) {
                throw TermexArchiveTransferException.InvalidArchive
            }
            val iv = envelope.sealedBox.copyOfRange(0, 12)
            val ciphertext = envelope.sealedBox.copyOfRange(12, envelope.sealedBox.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            return try {
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(deriveArchiveKey(password, envelope.salt, envelope.iterations), "AES"),
                    GCMParameterSpec(128, iv)
                )
                cipher.doFinal(ciphertext)
            } catch (_: Exception) {
                throw TermexArchiveTransferException.InvalidPassword
            }
        }

        private fun deriveArchiveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        }
    }
}

private fun String.toUuidOrStable(seed: String): UUID {
    return runCatching { UUID.fromString(this) }.getOrElse {
        stableUuid(seed)
    }
}

private fun stableUuid(seed: String): UUID {
    return UUID.nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8))
}

private fun String.toAuthMode(): AuthMode {
    return when (lowercase()) {
        "password" -> AuthMode.PASSWORD
        "key" -> AuthMode.KEY
        else -> AuthMode.AUTO
    }
}

private fun String.toArchivePortForwardType(): com.termex.app.domain.PortForwardType {
    return when (lowercase()) {
        "remote" -> com.termex.app.domain.PortForwardType.REMOTE
        "dynamic" -> com.termex.app.domain.PortForwardType.DYNAMIC
        else -> com.termex.app.domain.PortForwardType.LOCAL
    }
}
