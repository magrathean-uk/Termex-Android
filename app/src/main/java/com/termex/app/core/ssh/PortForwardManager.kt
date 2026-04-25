package com.termex.app.core.ssh

import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.apache.sshd.common.util.net.SshdSocketAddress
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ActivePortForward(
    val sessionKey: String,
    val config: PortForward,
    val isActive: Boolean = false,
    val error: String? = null,
    val boundAddress: SshdSocketAddress? = null
)

@Singleton
class PortForwardManager @Inject constructor() {

    private val _activeForwards = MutableStateFlow<List<ActivePortForward>>(emptyList())
    val activeForwards: StateFlow<List<ActivePortForward>> = _activeForwards.asStateFlow()

    private data class ForwardHandle(
        val sessionKey: String,
        val forward: PortForward,
        val boundAddress: SshdSocketAddress,
        val type: PortForwardType
    )

    private val forwardHandles = ConcurrentHashMap<String, ForwardHandle>()
    private val clients = ConcurrentHashMap<String, SSHClient>()

    fun activeForwards(sessionKey: String) = activeForwards.map { forwards ->
        forwards.filter { it.sessionKey == sessionKey }
    }

    suspend fun runShellCommand(
        sessionKey: String,
        command: String,
        expectedFragment: String? = null,
        timeoutMillis: Long = 5_000L
    ): String {
        val client = clients[sessionKey] ?: error("Not connected")
        return client.runShellCommand(command, expectedFragment, timeoutMillis)
    }

    fun setClient(sessionKey: String, client: SSHClient?) {
        if (client == null) {
            stopAllForwards(sessionKey)
            clients.remove(sessionKey)
            return
        }
        clients[sessionKey] = client
    }

    fun startForward(sessionKey: String, forward: PortForward): Result<Unit> {
        val client = clients[sessionKey] ?: return Result.failure(Exception("Not connected"))
        if (forwardHandles.containsKey(handleKey(sessionKey, forward.id))) {
            return Result.success(Unit)
        }

        return try {
            when (forward.type) {
                PortForwardType.LOCAL -> startLocalForward(sessionKey, client, forward)
                PortForwardType.REMOTE -> startRemoteForward(sessionKey, client, forward)
                PortForwardType.DYNAMIC -> startDynamicForward(sessionKey, client, forward)
            }

            updateForwardState(sessionKey, forward.id, isActive = true)
            Result.success(Unit)
        } catch (e: Exception) {
            val detail = buildString {
                append(e::class.java.simpleName)
                e.message?.takeIf { it.isNotBlank() }?.let {
                    append(": ")
                    append(it)
                }
            }
            updateForwardState(sessionKey, forward.id, isActive = false, error = detail)
            Result.failure(IllegalStateException(detail, e))
        }
    }

    private fun startLocalForward(sessionKey: String, client: SSHClient, forward: PortForward) {
        val local = SshdSocketAddress(forward.bindAddress, forward.localPort)
        val remote = SshdSocketAddress(forward.remoteHost, forward.remotePort)
        val bound = client.startLocalPortForwarding(local, remote)
        forwardHandles[handleKey(sessionKey, forward.id)] =
            ForwardHandle(sessionKey, forward, bound, PortForwardType.LOCAL)
    }

    private fun startRemoteForward(sessionKey: String, client: SSHClient, forward: PortForward) {
        val remoteBind = SshdSocketAddress(normalizeRemoteBindAddress(forward.bindAddress), forward.remotePort)
        val localTarget = SshdSocketAddress(forward.remoteHost, forward.localPort)
        val bound = client.startRemotePortForwarding(remoteBind, localTarget)
        forwardHandles[handleKey(sessionKey, forward.id)] =
            ForwardHandle(sessionKey, forward, bound, PortForwardType.REMOTE)
    }

    private fun startDynamicForward(sessionKey: String, client: SSHClient, forward: PortForward) {
        val local = SshdSocketAddress(forward.bindAddress, forward.localPort)
        val bound = client.startDynamicPortForwarding(local)
        forwardHandles[handleKey(sessionKey, forward.id)] =
            ForwardHandle(sessionKey, forward, bound, PortForwardType.DYNAMIC)
    }

    fun stopForward(sessionKey: String, forwardId: String) {
        val handle = forwardHandles.remove(handleKey(sessionKey, forwardId))
        handle?.let { h ->
            try {
                val client = clients[h.sessionKey]
                when (h.type) {
                    PortForwardType.LOCAL -> client?.stopLocalPortForwarding(h.boundAddress)
                    PortForwardType.REMOTE -> client?.stopRemotePortForwarding(h.boundAddress)
                    PortForwardType.DYNAMIC -> client?.stopDynamicPortForwarding(h.boundAddress)
                }
            } catch (_: Exception) {
            }
        }

        updateForwardState(sessionKey, forwardId, isActive = false)
    }

    fun stopAllForwards(sessionKey: String) {
        forwardHandles.values
            .filter { it.sessionKey == sessionKey }
            .map { it.forward.id }
            .forEach { stopForward(sessionKey, it) }
        _activeForwards.value = _activeForwards.value.filterNot { it.sessionKey == sessionKey }
    }

    private fun updateForwardState(sessionKey: String, id: String, isActive: Boolean, error: String? = null) {
        val current = _activeForwards.value.toMutableList()
        val index = current.indexOfFirst { it.sessionKey == sessionKey && it.config.id == id }
        val handle = forwardHandles[handleKey(sessionKey, id)]
        if (index >= 0) {
            current[index] = current[index].copy(
                isActive = isActive,
                error = error,
                boundAddress = handle?.boundAddress ?: current[index].boundAddress
            )
            _activeForwards.value = current
        } else {
            val config = handle?.forward ?: return
            current.add(
                ActivePortForward(
                    sessionKey = sessionKey,
                    config = config,
                    isActive = isActive,
                    error = error,
                    boundAddress = handle?.boundAddress
                )
            )
            _activeForwards.value = current
        }
    }

    fun initializeForwards(sessionKey: String, forwards: List<PortForward>) {
        val otherForwards = _activeForwards.value.filterNot { it.sessionKey == sessionKey }
        _activeForwards.value = otherForwards + forwards.map {
            ActivePortForward(sessionKey, it, isActive = false)
        }
    }

    private fun normalizeRemoteBindAddress(address: String): String {
        return normalizeLoopbackHost(address)
    }

    private fun normalizeLoopbackHost(address: String): String {
        return when (address.trim()) {
            "127.0.0.1", "::1" -> "localhost"
            else -> address
        }
    }

    private fun handleKey(sessionKey: String, forwardId: String): String = "$sessionKey:$forwardId"
}
