package com.termex.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.ssh.SSHClient
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.TerminalBuffer
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

data class TerminalPaneState(
    val server: Server,
    val connectionState: SSHConnectionState = SSHConnectionState.Disconnected,
    val lines: List<TerminalBuffer.TerminalLine> = emptyList(),
    val cursorPosition: Pair<Int, Int> = 0 to 0
)

@HiltViewModel
class MultiTerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workplaceRepository: WorkplaceRepository,
    private val sshClientProvider: Provider<SSHClient>
) : ViewModel() {

    private val workplaceId: String = savedStateHandle.get<String>("workplaceId") ?: ""

    val servers: StateFlow<List<Server>> = workplaceRepository.getServersForWorkplace(workplaceId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _paneStates = MutableStateFlow<Map<String, TerminalPaneState>>(emptyMap())
    val paneStates: StateFlow<Map<String, TerminalPaneState>> = _paneStates.asStateFlow()

    private val clients = mutableMapOf<String, SSHClient>()
    private val buffers = mutableMapOf<String, TerminalBuffer>()
    private val readJobs = mutableMapOf<String, Job>()

    private val _selectedPane = MutableStateFlow<String?>(null)
    val selectedPane: StateFlow<String?> = _selectedPane.asStateFlow()

    fun selectPane(serverId: String) {
        _selectedPane.value = serverId
    }

    fun connectServer(server: Server, password: String? = null) {
        if (clients.containsKey(server.id)) return

        viewModelScope.launch {
            val client = sshClientProvider.get()
            clients[server.id] = client

            val buffer = TerminalBuffer()
            buffers[server.id] = buffer

            updatePaneState(server.id) { it.copy(connectionState = SSHConnectionState.Connecting) }

            val savedPassword = server.passwordKeychainID?.takeIf { it.isNotBlank() }
            val effectivePassword = password?.takeIf { it.isNotBlank() } ?: savedPassword

            val keyPath = server.keyId?.takeIf { it.isNotBlank() }
            val privateKeyBytes = keyPath?.let { path ->
                val keyFile = File(path)
                if (keyFile.exists()) keyFile.readBytes() else null
            }

            val config = when (server.authMode) {
                AuthMode.KEY -> {
                    if (privateKeyBytes != null) {
                        SSHConnectionConfig(
                            hostname = server.hostname,
                            port = server.port,
                            username = server.username,
                            privateKey = privateKeyBytes
                        )
                    } else {
                        SSHConnectionConfig(
                            hostname = server.hostname,
                            port = server.port,
                            username = server.username,
                            password = effectivePassword
                        )
                    }
                }
                AuthMode.PASSWORD -> {
                    if (effectivePassword != null) {
                        SSHConnectionConfig(
                            hostname = server.hostname,
                            port = server.port,
                            username = server.username,
                            password = effectivePassword
                        )
                    } else {
                        SSHConnectionConfig(
                            hostname = server.hostname,
                            port = server.port,
                            username = server.username,
                            privateKey = privateKeyBytes
                        )
                    }
                }
                AuthMode.AUTO -> SSHConnectionConfig(
                    hostname = server.hostname,
                    port = server.port,
                    username = server.username,
                    privateKey = privateKeyBytes,
                    password = effectivePassword
                )
            }

            val result = client.connect(config)

            if (result.isSuccess) {
                updatePaneState(server.id) { it.copy(connectionState = SSHConnectionState.Connected) }
                startReading(server.id, client, buffer)
            } else {
                updatePaneState(server.id) {
                    it.copy(connectionState = SSHConnectionState.Error(result.exceptionOrNull()?.message ?: "Failed"))
                }
            }
        }
    }

    private fun startReading(serverId: String, client: SSHClient, buffer: TerminalBuffer) {
        readJobs[serverId]?.cancel()
        readJobs[serverId] = viewModelScope.launch(Dispatchers.IO) {
            val byteBuffer = ByteArray(8192)
            val inputStream = client.inputStream ?: return@launch

            try {
                while (isActive && client.isConnected()) {
                    val bytesRead = inputStream.read(byteBuffer)
                    if (bytesRead > 0) {
                        val data = String(byteBuffer, 0, bytesRead, Charsets.UTF_8)
                        buffer.write(data)
                        updatePaneState(serverId) {
                            it.copy(
                                lines = buffer.contentFlow.value,
                                cursorPosition = buffer.cursorPosition.value
                            )
                        }
                    } else if (bytesRead == -1) {
                        break
                    }
                }
            } catch (e: Exception) {
                // Connection closed
            }
        }
    }

    fun sendInput(serverId: String, data: String) {
        viewModelScope.launch {
            clients[serverId]?.sendData(data)
        }
    }

    fun disconnectServer(serverId: String) {
        readJobs[serverId]?.cancel()
        readJobs.remove(serverId)
        clients[serverId]?.disconnect()
        clients.remove(serverId)
        buffers.remove(serverId)

        _paneStates.value = _paneStates.value.toMutableMap().apply {
            remove(serverId)
        }
    }

    fun disconnectAll() {
        clients.keys.toList().forEach { disconnectServer(it) }
    }

    private fun updatePaneState(serverId: String, update: (TerminalPaneState) -> TerminalPaneState) {
        val current = _paneStates.value.toMutableMap()
        val existing = current[serverId]
        if (existing != null) {
            current[serverId] = update(existing)
            _paneStates.value = current
        }
    }

    fun initializePanes(servers: List<Server>) {
        val states = servers.associate { server ->
            server.id to TerminalPaneState(server = server)
        }
        _paneStates.value = states
    }

    override fun onCleared() {
        super.onCleared()
        disconnectAll()
    }
}
