package com.termex.app.domain

import java.util.UUID

data class Server(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authMode: AuthMode = AuthMode.PASSWORD,
    val passwordKeychainID: String? = null,
    val keyId: String? = null,
    val workplaceId: String? = null
) {
    val displayName: String
        get() = name.ifEmpty { hostname }
}

enum class AuthMode {
    PASSWORD, KEY, AUTO
}
