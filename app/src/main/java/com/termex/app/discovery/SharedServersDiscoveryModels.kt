package com.termex.app.discovery

data class SharedServerDiscoveryItem(
    val id: String,
    val serviceName: String,
    val host: String? = null,
    val port: Int? = null
) {
    val isResolved: Boolean
        get() = !host.isNullOrBlank() && port != null && port > 0
}

data class SharedServerImportTarget(
    val host: String,
    val port: Int
)

data class SharedServersDiscoveryState(
    val isDiscovering: Boolean = false,
    val services: List<SharedServerDiscoveryItem> = emptyList(),
    val errorMessage: String? = null
)

