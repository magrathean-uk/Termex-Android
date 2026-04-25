package com.termex.app.data.repository

import android.content.Context
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.CertificateType
import com.termex.app.domain.SSHCertificate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val certificateMetadataStore: CertificateMetadataStore
) : CertificateRepository {

    private val certsDir: File by lazy {
        File(context.filesDir, "ssh_certs").apply { mkdirs() }
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also {
        it.tryEmit(Unit)
    }

    override fun getAllCertificates(): Flow<List<SSHCertificate>> = callbackFlow {
        val job = launch {
            merge(refreshTrigger, certificateMetadataStore.changesFlow()).collect {
                val files = certsDir.listFiles() ?: emptyArray()
                val actualCertificates = files.map { file ->
                    parseCertificate(file)
                }
                val metadataOnlyCertificates = certificateMetadataStore.getAll().filter { metadataCert ->
                    actualCertificates.none { actual -> actual.path == metadataCert.path }
                }
                val certs = (actualCertificates + metadataOnlyCertificates).sortedBy { it.name }
                trySend(certs)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun importCertificate(name: String, content: String) = withContext(Dispatchers.IO) {
        val sanitizedName = File(name).name.replace("..", "")
        require(sanitizedName.isNotBlank()) { "Invalid certificate name" }
        val certFile = File(certsDir, sanitizedName)
        require(certFile.canonicalPath.startsWith(certsDir.canonicalPath)) { "Invalid certificate path" }
        certFile.writeText(content.trim())
        certificateMetadataStore.deleteByPath(certFile.absolutePath)
        refreshTrigger.emit(Unit)
    }

    override suspend fun deleteCertificate(certificate: SSHCertificate) = withContext(Dispatchers.IO) {
        File(certsDir, certificate.name).delete()
        certificateMetadataStore.deleteByPath(certificate.path)
        refreshTrigger.emit(Unit)
    }

    override fun getCertificatePath(name: String): String {
        return File(certsDir, name).absolutePath
    }

    private fun parseCertificate(file: File): SSHCertificate {
        val content = try { file.readText().trim() } catch (_: Exception) { "" }

        // OpenSSH certificate format: "<type>-cert-v01@openssh.com <base64> [comment]"
        val certType = when {
            content.startsWith("ssh-rsa-cert") -> CertificateType.USER
            content.startsWith("ssh-ed25519-cert") -> CertificateType.USER
            content.startsWith("ecdsa-sha2-nistp256-cert") -> CertificateType.USER
            content.startsWith("ssh-rsa-cert-v01@openssh.com") -> CertificateType.USER
            content.startsWith("ssh-ed25519-cert-v01@openssh.com") -> CertificateType.USER
            else -> CertificateType.USER
        }

        val keyTypeString = content.substringBefore(" ").ifEmpty { "Unknown" }

        return SSHCertificate(
            id = file.name,
            name = file.name,
            path = file.absolutePath,
            certificateType = certType,
            keyId = keyTypeString,
            caFingerprint = ""
        )
    }
}
