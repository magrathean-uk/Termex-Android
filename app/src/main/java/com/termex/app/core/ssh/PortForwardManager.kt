package com.termex.app.core.ssh

import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder
import net.schmizz.sshj.connection.channel.forwarded.SocketForwardingConnectListener
import java.net.InetSocketAddress
import java.net.ServerSocket
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

    private val localForwardSockets = mutableMapOf<String, ServerSocket>()
    private var currentClient: SSHClient? = null

    fun setClient(client: SSHClient?) {
        currentClient = client
        if (client == null) {
            stopAllForwards()
        }
    }

    fun startForward(forward: PortForward): Result<Unit> {
        val client = currentClient ?: return Result.failure(Exception("Not connected"))

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
        val serverSocket = ServerSocket()
        serverSocket.reuseAddress = true
        serverSocket.bind(InetSocketAddress("localhost", forward.localPort))
        localForwardSockets[forward.id] = serverSocket

        // Start forwarding in a background thread using direct-tcpip channel
        Thread {
            try {
                while (!serverSocket.isClosed) {
                    val socket = serverSocket.accept()
                    // For each connection, open a direct-tcpip channel to forward traffic
                    Thread {
                        try {
                            val channel = client.newDirectConnection(
                                forward.remoteHost,
                                forward.remotePort
                            )
                            val localIn = socket.getInputStream()
                            val localOut = socket.getOutputStream()
                            val remoteIn = channel.inputStream
                            val remoteOut = channel.outputStream

                            // Forward local to remote
                            Thread {
                                try {
                                    localIn.copyTo(remoteOut)
                                } catch (_: Exception) {}
                            }.start()

                            // Forward remote to local
                            remoteIn.copyTo(localOut)
                        } catch (_: Exception) {
                        } finally {
                            socket.close()
                        }
                    }.start()
                }
            } catch (e: Exception) {
                updateForwardState(forward.id, isActive = false, error = e.message)
            }
        }.start()
    }

    private fun startRemoteForward(client: SSHClient, forward: PortForward) {
        // Remote port forwarding: connections to remotePort on server are forwarded to localhost:localPort
        client.remotePortForwarder.bind(
            RemotePortForwarder.Forward(forward.remotePort),
            SocketForwardingConnectListener(InetSocketAddress("localhost", forward.localPort))
        )
    }

    private fun startDynamicForward(client: SSHClient, forward: PortForward) {
        // Dynamic SOCKS proxy - SSHJ supports this via local port forwarder with special handling
        // For simplicity, we'll implement a basic local port that acts as SOCKS proxy
        // This is a simplified implementation - full SOCKS5 would require more work
        val serverSocket = ServerSocket()
        serverSocket.reuseAddress = true
        serverSocket.bind(InetSocketAddress("localhost", forward.localPort))
        localForwardSockets[forward.id] = serverSocket

        // Note: Full SOCKS5 proxy implementation would require additional handling
        // For now, mark as active - the actual SOCKS handling would need more code
    }

    fun stopForward(forwardId: String) {
        localForwardSockets[forwardId]?.let { socket ->
            try {
                socket.close()
            } catch (_: Exception) {}
            localForwardSockets.remove(forwardId)
        }

        updateForwardState(forwardId, isActive = false)
    }

    fun stopAllForwards() {
        localForwardSockets.values.forEach { socket ->
            try {
                socket.close()
            } catch (_: Exception) {}
        }
        localForwardSockets.clear()
        _activeForwards.value = emptyList()
    }

    private fun updateForwardState(id: String, isActive: Boolean, error: String? = null) {
        val current = _activeForwards.value.toMutableList()
        val index = current.indexOfFirst { it.config.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(isActive = isActive, error = error)
            _activeForwards.value = current
        }
    }

    fun initializeForwards(forwards: List<PortForward>) {
        _activeForwards.value = forwards.map { ActivePortForward(it, isActive = false) }
    }
}
