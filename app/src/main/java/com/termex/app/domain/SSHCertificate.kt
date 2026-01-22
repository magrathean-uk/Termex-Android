package com.termex.app.domain

import java.util.Date
import java.util.UUID

data class SSHCertificate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val principals: List<String> = emptyList(),
    val validAfter: Date? = null,
    val validBefore: Date? = null,
    val keyId: String = "",
    val caFingerprint: String = "",
    val certificateType: CertificateType = CertificateType.USER
) {
    val isValid: Boolean
        get() {
            val now = Date()
            val afterValid = validAfter?.let { now.after(it) || now == it } ?: true
            val beforeValid = validBefore?.let { now.before(it) } ?: true
            return afterValid && beforeValid
        }

    val validityStatus: String
        get() = when {
            validBefore != null && Date().after(validBefore) -> "Expired"
            validAfter != null && Date().before(validAfter) -> "Not yet valid"
            else -> "Valid"
        }
}

enum class CertificateType {
    USER,
    HOST
}
