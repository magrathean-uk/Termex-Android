package com.termex.app.discovery

import kotlinx.coroutines.flow.StateFlow

interface SharedServersDiscoveryService {
    val state: StateFlow<SharedServersDiscoveryState>

    fun startDiscovery()

    fun stopDiscovery()

    suspend fun resolve(serviceId: String): SharedServerImportTarget?
}

