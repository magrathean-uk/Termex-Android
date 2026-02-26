package com.termex.app.core.ssh

import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.sshd.common.util.net.SshdSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

data class ActivePortForward(
    val config: PortForward,
    val isActive: Boolean = false,
    val error: String? = null
)

@Singleton
class PortForwardManager @Inject constructor() {

    private val _activeForwards = MutableStateFlow<List<ActivePortForward>>(emptyList())
    val activeForwards: StateFlow<List<ActivePortForward>> = _activeForwards.asStateFlow()

    private data class ForwardHandle(
        val forward: PortForward,
        val boundAddress: SshdSocketAddress,
        val type: PortForwardType
    )

    private val forwardHandles = mutableMapOf<String, ForwardHandle>()
    private var currentClient: SSHClient? = null

    fun setClient(client: SSHClient?) {
        if (client == null) {
            stopAllForwards()
        }
        currentClient = client
    }

    fun startForward(forward: PortForward): Result<Unit> {
        val client = currentClient ?: return Result.failure(Exception("Not connected"))
        if (forwardHandles.containsKey(forward.id)) {
            return Result.success(Unit)
        }

        return try {
            when (forward.type) {
                PortForwardType.LOCAL -> startLocalForward(client, forward)
                PortForwardType.REMOTE -> startRemoteForward(client, forward)
                PortForwardType.DYNAMIC -> startDynamicForward(client, forward)
            }

            updateForwardState(forward.id, isActive = true)
            Result.success(Unit)
        } catch (e: Exception) {
            updateForwardState(forward.id, isActive = false, error = e.message)
            Result.failure(e)
        }
    }

    private fun startLocalForward(client: SSHClient, forward: PortForward) {
        // Local port forwarding: connections to localPort are forwarded to remoteHost:remotePort
        val local = SshdSocketAddress("127.0.0.1", forward.localPort)
        val remote = SshdSocketAddress(forward.remoteHost, forward.remotePort)
        val bound = client.startLocalPortForwarding(local, remote)
        forwardHandles[forward.id] = ForwardHandle(forward, bound, PortForwardType.LOCAL)
    }

    private fun startRemoteForward(client: SSHClient, forward: PortForward) {
        // Remote port forwarding: connections to remotePort on server are forwarded to localhost:localPort
        val remoteBind = SshdSocketAddress(forward.remoteHost, forward.remotePort)
        val localTarget = SshdSocketAddress("127.0.0.1", forward.localPort)
        val bound = client.startRemotePortForwarding(remoteBind, localTarget)
        forwardHandles[forward.id] = ForwardHandle(forward, bound, PortForwardType.REMOTE)
    }

    private fun startDynamicForward(client: SSHClient, forward: PortForward) {
        val local = SshdSocketAddress("127.0.0.1", forward.localPort)
        val bound = client.startDynamicPortForwarding(local)
        forwardHandles[forward.id] = ForwardHandle(forward, bound, PortForwardType.DYNAMIC)
    }

    fun stopForward(forwardId: String) {
        val handle = forwardHandles.remove(forwardId)
        handle?.let { h ->
            try {
                when (h.type) {
                    PortForwardType.LOCAL -> currentClient?.stopLocalPortForwarding(h.boundAddress)
                    PortForwardType.REMOTE -> currentClient?.stopRemotePortForwarding(h.boundAddress)
                    PortForwardType.DYNAMIC -> currentClient?.stopDynamicPortForwarding(h.boundAddress)
                }
            } catch (_: Exception) {
            }
        }

        updateForwardState(forwardId, isActive = false)
    }

    fun stopAllForwards() {
        forwardHandles.keys.toList().forEach { stopForward(it) }
        _activeForwards.value = emptyList()
    }

    private fun updateForwardState(id: String, isActive: Boolean, error: String? = null) {
        val current = _activeForwards.value.toMutableList()
        val index = current.indexOfFirst { it.config.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(isActive = isActive, error = error)
            _activeForwards.value = current
        } else {
            val config = forwardHandles[id]?.forward ?: return
            current.add(ActivePortForward(config, isActive = isActive, error = error))
            _activeForwards.value = current
        }
    }

    fun initializeForwards(forwards: List<PortForward>) {
        _activeForwards.value = forwards.map { ActivePortForward(it, isActive = false) }
    }
}
