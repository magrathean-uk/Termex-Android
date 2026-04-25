package com.termex.app.core.transfer

import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object TermexArchiveSerializers {
    object UuidAsString : KSerializer<UUID> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("TermexArchiveUUID", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: UUID) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): UUID {
            return UUID.fromString(decoder.decodeString())
        }
    }

    object UuidListAsString : KSerializer<List<UUID>> {
        override val descriptor: SerialDescriptor =
            ListSerializer(UuidAsString).descriptor

        override fun serialize(encoder: Encoder, value: List<UUID>) {
            ListSerializer(UuidAsString).serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): List<UUID> {
            return ListSerializer(UuidAsString).deserialize(decoder)
        }
    }

    object IsoDate : KSerializer<Date> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("TermexArchiveDate", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Date) {
            encoder.encodeString(TermexArchiveDateCodec.format(value))
        }

        override fun deserialize(decoder: Decoder): Date {
            return TermexArchiveDateCodec.parse(decoder.decodeString())
        }
    }

    object Base64Bytes : KSerializer<ByteArray> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("TermexArchiveBytes", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ByteArray) {
            encoder.encodeString(Base64.getEncoder().encodeToString(value))
        }

        override fun deserialize(decoder: Decoder): ByteArray {
            return Base64.getDecoder().decode(decoder.decodeString())
        }
    }
}

internal object TermexArchiveDateCodec {
    private val formatter = java.time.format.DateTimeFormatter.ISO_INSTANT

    fun format(date: Date): String {
        return formatter.format(date.toInstant())
    }

    fun parse(value: String): Date {
        return Date.from(java.time.Instant.parse(value))
    }
}

@Serializable
data class TermexArchiveCountsV2(
    val servers: Int,
    val keys: Int,
    val certificates: Int,
    val knownHosts: Int,
    val workplaces: Int
)

@Serializable
data class TermexArchiveChecksumV2(
    val algorithm: String,
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val payloadDigest: ByteArray
)

@Serializable
data class TermexArchiveManifestV2(
    val appVersion: String,
    val archiveVersion: Int,
    @Serializable(with = TermexArchiveSerializers.IsoDate::class)
    val createdAt: Date,
    val counts: TermexArchiveCountsV2,
    val checksum: TermexArchiveChecksumV2
)

@Serializable
data class TermexArchiveEnvelopeV2(
    val version: Int,
    @Serializable(with = TermexArchiveSerializers.IsoDate::class)
    val createdAt: Date,
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val salt: ByteArray,
    val iterations: Int,
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val sealedBox: ByteArray
)

@Serializable
data class TermexArchiveDocumentV2(
    val manifest: TermexArchiveManifestV2,
    val envelope: TermexArchiveEnvelopeV2
)

@Serializable
data class TermexArchivePortForwardV2(
    @Serializable(with = TermexArchiveSerializers.UuidAsString::class)
    val id: UUID,
    val type: String,
    val bindAddress: String,
    val bindPort: Int,
    val targetAddress: String,
    val targetPort: Int
)

@Serializable
data class TermexArchiveServerV2(
    @Serializable(with = TermexArchiveSerializers.UuidAsString::class)
    val id: UUID,
    val name: String,
    val hostname: String,
    val port: Int,
    val username: String,
    val authModeRaw: String,
    val identitiesOnly: Boolean,
    val portForwards: List<TermexArchivePortForwardV2> = emptyList(),
    @Serializable(with = TermexArchiveSerializers.UuidAsString::class)
    val jumpHostID: UUID? = null,
    val usePersistentTmux: Boolean,
    val tmuxWorkingDirectory: String? = null,
    val startupCommand: String? = null,
    @Serializable(with = TermexArchiveSerializers.UuidListAsString::class)
    val preferredKeyRecordIDs: List<UUID> = emptyList(),
    @Serializable(with = TermexArchiveSerializers.UuidListAsString::class)
    val preferredCertificateIDs: List<UUID> = emptyList(),
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val passwordData: ByteArray? = null
)

@Serializable
data class TermexArchiveKeyV2(
    @Serializable(with = TermexArchiveSerializers.UuidAsString::class)
    val id: UUID,
    val name: String,
    val keyType: String,
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val publicKeyData: ByteArray? = null,
    val fingerprint: String? = null,
    val comment: String? = null,
    @Serializable(with = TermexArchiveSerializers.UuidListAsString::class)
    val certificateIDs: List<UUID> = emptyList(),
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val privateKeyData: ByteArray? = null,
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val passphraseData: ByteArray? = null
)

@Serializable
data class TermexArchiveCertificateV2(
    @Serializable(with = TermexArchiveSerializers.UuidAsString::class)
    val id: UUID,
    val name: String,
    val certType: String,
    @Serializable(with = TermexArchiveSerializers.Base64Bytes::class)
    val publicKeyData: ByteArray,
    val fingerprint: String? = null,
    val comment: String? = null,
    val principals: List<String> = emptyList(),
    @Serializable(with = TermexArchiveSerializers.IsoDate::class)
    val validAfter: Date? = null,
    @Serializable(with = TermexArchiveSerializers.IsoDate::class)
    val validBefore: Date? = null,
    val signerKeyID: String? = null
)

@Serializable
data class TermexArchiveKnownHostV2(
    @Serializable(with = TermexArchiveSerializers.UuidAsString::class)
    val id: UUID,
    val hostname: String,
    val port: Int,
    val keyFingerprint: String,
    val keyType: String,
    @Serializable(with = TermexArchiveSerializers.IsoDate::class)
    val createdAt: Date
)

@Serializable
data class TermexArchiveWorkplaceV2(
    @Serializable(with = TermexArchiveSerializers.UuidAsString::class)
    val id: UUID,
    val name: String,
    @Serializable(with = TermexArchiveSerializers.UuidListAsString::class)
    val serverIDs: List<UUID> = emptyList()
)

@Serializable
data class TermexArchivePayloadV2(
    val version: Int,
    @Serializable(with = TermexArchiveSerializers.IsoDate::class)
    val createdAt: Date,
    val appVersion: String,
    val servers: List<TermexArchiveServerV2> = emptyList(),
    val keys: List<TermexArchiveKeyV2> = emptyList(),
    val certificates: List<TermexArchiveCertificateV2> = emptyList(),
    val knownHosts: List<TermexArchiveKnownHostV2> = emptyList(),
    val workplaces: List<TermexArchiveWorkplaceV2> = emptyList()
)

data class TermexArchiveExportSummary(
    val archiveVersion: Int,
    val payloadVersion: Int,
    val counts: TermexArchiveCountsV2,
    val archiveBytes: Int
)

data class TermexArchiveImportSummary(
    var insertedServers: Int = 0,
    var updatedServers: Int = 0,
    var insertedKeys: Int = 0,
    var updatedKeys: Int = 0,
    var insertedCertificates: Int = 0,
    var updatedCertificates: Int = 0,
    var insertedKnownHosts: Int = 0,
    var updatedKnownHosts: Int = 0,
    var insertedWorkplaces: Int = 0,
    var updatedWorkplaces: Int = 0
) {
    val summaryText: String
        get() {
            val parts = listOf(
                countText(insertedServers + updatedServers, "server"),
                countText(insertedKeys + updatedKeys, "key"),
                countText(insertedCertificates + updatedCertificates, "certificate"),
                countText(insertedKnownHosts + updatedKnownHosts, "known host"),
                countText(insertedWorkplaces + updatedWorkplaces, "workplace")
            ).filterNot { it.startsWith("0 ") }

            return if (parts.isEmpty()) {
                "Archive imported."
            } else {
                "Imported ${parts.joinToString(", ")}."
            }
        }

    private fun countText(count: Int, noun: String): String {
        return "$count $noun${if (count == 1) "" else "s"}"
    }
}

data class TermexArchiveExportResult(
    val document: TermexArchiveDocumentV2,
    val bytes: ByteArray,
    val summary: TermexArchiveExportSummary
)

data class TermexArchiveImportResult(
    val payload: TermexArchivePayloadV2,
    val summary: TermexArchiveImportSummary
)

sealed class TermexArchiveTransferException(message: String) : IllegalArgumentException(message) {
    data object EmptyPassword : TermexArchiveTransferException("Enter an archive password first.")
    data object PasswordMismatch : TermexArchiveTransferException("The archive passwords do not match.")
    data object InvalidArchive : TermexArchiveTransferException("This file is not a valid Termex archive.")
    data object InvalidPassword : TermexArchiveTransferException(
        "The archive password is incorrect, or the file is corrupted."
    )
    data class FutureArchiveVersion(val version: Int) : TermexArchiveTransferException(
        "This archive was made by Termex archive format $version, which is newer than this app can read. Update Termex and try again."
    )
    data class InaccessibleSecret(val label: String) : TermexArchiveTransferException(
        "Couldn't read $label from secure storage. Unlock the device and try again."
    )
    data object ArchiveTooLarge : TermexArchiveTransferException(
        "This archive is too large to import on-device."
    )
    data object UnsupportedSecurityParameters : TermexArchiveTransferException(
        "This archive uses unsupported security parameters."
    )
}
