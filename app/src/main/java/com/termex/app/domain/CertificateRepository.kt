package com.termex.app.domain

import kotlinx.coroutines.flow.Flow

interface CertificateRepository {
    fun getAllCertificates(): Flow<List<SSHCertificate>>
    suspend fun importCertificate(name: String, content: String)
    suspend fun deleteCertificate(certificate: SSHCertificate)
    fun getCertificatePath(name: String): String
}
