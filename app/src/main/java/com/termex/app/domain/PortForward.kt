package com.termex.app.domain

import java.util.UUID

enum class PortForwardType {
    LOCAL,    // -L: Local port forwarding (local:remoteHost:remotePort)
    REMOTE,   // -R: Remote port forwarding (remote:localHost:localPort)
    DYNAMIC   // -D: Dynamic SOCKS proxy
}

data class PortForward(
    val id: String = UUID.randomUUID().toString(),
    val type: PortForwardType = PortForwardType.LOCAL,
    val localPort: Int,
    val remoteHost: String = "localhost",
    val remotePort: Int,
    val enabled: Boolean = true,
    val bindAddress: String = "127.0.0.1"
) {
    val displayString: String
        get() = when (type) {
            PortForwardType.LOCAL -> "L:$localPort -> $remoteHost:$remotePort"
            PortForwardType.REMOTE -> "R:$bindAddress:$remotePort -> localhost:$localPort"
            PortForwardType.DYNAMIC -> "D:$localPort (SOCKS)"
        }
}
