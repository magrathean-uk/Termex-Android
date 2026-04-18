package com.termex.app.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidSharedServersDiscoveryService @Inject constructor(
    @ApplicationContext context: Context
) : SharedServersDiscoveryService {

    private val nsdManager: NsdManager = checkNotNull(context.getSystemService()) {
        "NsdManager not available"
    }
    private val discoveredServices = linkedMapOf<String, NsdServiceInfo>()
    private val discoveryActive = AtomicBoolean(false)

    private val _state = MutableStateFlow(SharedServersDiscoveryState())
    override val state: StateFlow<SharedServersDiscoveryState> = _state.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    override fun startDiscovery() {
        if (!discoveryActive.compareAndSet(false, true)) return

        discoveredServices.clear()
        _state.value = SharedServersDiscoveryState(isDiscovering = true)

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                fail("Discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                fail("Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                _state.update { it.copy(isDiscovering = true, errorMessage = null) }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                discoveryActive.set(false)
                _state.update { it.copy(isDiscovering = false) }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val id = serviceId(serviceInfo)
                discoveredServices[id] = serviceInfo
                publishServices()
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                discoveredServices.remove(serviceId(serviceInfo))
                publishServices()
            }
        }

        discoveryListener = listener

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (t: Throwable) {
            fail(t.message ?: "Discovery failed")
        }
    }

    override fun stopDiscovery() {
        val listener = discoveryListener ?: run {
            discoveryActive.set(false)
            _state.update { it.copy(isDiscovering = false) }
            return
        }
        discoveryListener = null
        discoveryActive.set(false)
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (_: Throwable) {
            // Ignore. Discovery state already cleared.
        } finally {
            _state.update { it.copy(isDiscovering = false) }
        }
    }

    override suspend fun resolve(serviceId: String): SharedServerImportTarget? {
        val serviceInfo = discoveredServices[serviceId] ?: return null

        serviceInfo.host?.hostAddress?.takeIf { serviceInfo.port > 0 }?.let { host ->
            return SharedServerImportTarget(host = host, port = serviceInfo.port)
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: serviceInfo.host?.hostName
                        val port = serviceInfo.port
                        if (host.isNullOrBlank() || port <= 0) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                            return
                        }
                        discoveredServices[serviceId(serviceInfo)] = serviceInfo
                        publishServices()
                        if (continuation.isActive) {
                            continuation.resume(SharedServerImportTarget(host = host, port = port))
                        }
                    }
                })
            } catch (_: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    private fun publishServices() {
        _state.update { current ->
            current.copy(
                services = discoveredServices.values
                    .map { info ->
                        val host = info.host?.hostAddress ?: info.host?.hostName
                        SharedServerDiscoveryItem(
                            id = serviceId(info),
                            serviceName = info.serviceName,
                            host = host,
                            port = info.port.takeIf { it > 0 }
                        )
                    }
                    .sortedBy { it.serviceName.lowercase() }
            )
        }
    }

    private fun fail(message: String) {
        discoveryActive.set(false)
        discoveryListener = null
        _state.value = SharedServersDiscoveryState(
            isDiscovering = false,
            services = _state.value.services,
            errorMessage = message
        )
    }

    private fun serviceId(serviceInfo: NsdServiceInfo): String {
        return "${serviceInfo.serviceType}:${serviceInfo.serviceName}"
    }

    companion object {
        private const val SERVICE_TYPE = "_ssh._tcp"
    }
}
