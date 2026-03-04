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
    val workplaceId: String? = null,
    val portForwards: List<PortForward> = emptyList(),
    val jumpHostId: String? = null,
    val forwardAgent: Boolean = false,
    val isDemo: Boolean = false,
    val identitiesOnly: Boolean = false
) {
    val displayName: String
        get() = name.ifEmpty { hostname }

    companion object {
        const val DEMO_SERVER_ID = "demo-server"

        fun createDemoServer() = Server(
            id = DEMO_SERVER_ID,
            name = "Demo Server",
            hostname = "demo.termex.app",
            port = 22,
            username = "demo",
            authMode = AuthMode.PASSWORD,
            isDemo = true
        )
    }
}

enum class AuthMode {
    PASSWORD, KEY, AUTO
}
