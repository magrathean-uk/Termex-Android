package com.termex.app.domain

import java.util.Date

data class SSHKey(
    val name: String,
    val path: String,
    val publicKey: String = "",
    val type: String = "RSA",
    val fingerprint: String = "",
    val lastModified: Date = Date()
)
