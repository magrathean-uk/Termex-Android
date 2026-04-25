package com.termex.app.domain

import java.util.Date
import java.util.UUID

data class KnownHost(
    val id: String = UUID.randomUUID().toString(),
    val hostname: String,
    val port: Int = 22,
    val keyType: String,
    val fingerprint: String,
    val publicKey: String,
    val addedAt: Date = Date(),
    val lastSeenAt: Date = Date()
) {
    val hostKey: String
        get() = "$hostname:$port"
}
