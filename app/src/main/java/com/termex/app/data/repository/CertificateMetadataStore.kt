package com.termex.app.data.repository

import android.content.Context
import com.termex.app.domain.CertificateType
import com.termex.app.domain.SSHCertificate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class StoredCertificateMetadata(
    val id: String,
    val name: String,
    val path: String,
    val principals: List<String>,
    val validAfterMillis: Long? = null,
    val validBeforeMillis: Long? = null,
    val keyId: String,
    val caFingerprint: String,
    val certificateType: String
)

@Singleton
class CertificateMetadataStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val metadataDir = File(context.filesDir, "ssh_cert_metadata").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }
    private val changes = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun changesFlow(): Flow<Unit> = changes

    fun getAll(): List<SSHCertificate> {
        val files = metadataDir.listFiles()?.sortedBy { it.name } ?: emptyList()
        return files.mapNotNull { file ->
            runCatching {
                json.decodeFromString<StoredCertificateMetadata>(file.readText())
            }.getOrNull()?.toDomain()
        }
    }

    fun upsert(certificate: SSHCertificate) {
        fileForPath(certificate.path).writeText(
            json.encodeToString(
                StoredCertificateMetadata(
                    id = certificate.id,
                    name = certificate.name,
                    path = certificate.path,
                    principals = certificate.principals,
                    validAfterMillis = certificate.validAfter?.time,
                    validBeforeMillis = certificate.validBefore?.time,
                    keyId = certificate.keyId,
                    caFingerprint = certificate.caFingerprint,
                    certificateType = certificate.certificateType.name
                )
            )
        )
        changes.tryEmit(Unit)
    }

    fun replaceAll(certificates: List<SSHCertificate>) {
        val targetFiles = certificates.associateBy { fileForPath(it.path).name }
        metadataDir.listFiles()?.forEach { file ->
            if (file.name !in targetFiles) {
                file.delete()
            }
        }
        certificates.forEach(::upsert)
        changes.tryEmit(Unit)
    }

    fun deleteByPath(path: String?) {
        if (path.isNullOrBlank()) return
        fileForPath(path).delete()
        changes.tryEmit(Unit)
    }

    private fun fileForPath(path: String): File {
        return File(metadataDir, "${path.sha256Hex()}.json")
    }

    private fun StoredCertificateMetadata.toDomain(): SSHCertificate {
        return SSHCertificate(
            id = id,
            name = name,
            path = path,
            principals = principals,
            validAfter = validAfterMillis?.let(::Date),
            validBefore = validBeforeMillis?.let(::Date),
            keyId = keyId,
            caFingerprint = caFingerprint,
            certificateType = runCatching { CertificateType.valueOf(certificateType) }
                .getOrDefault(CertificateType.USER)
        )
    }
}
