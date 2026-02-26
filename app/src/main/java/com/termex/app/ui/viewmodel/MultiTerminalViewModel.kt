package com.termex.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.ssh.HostKeyVerificationCallback
import com.termex.app.core.ssh.HostKeyVerificationResult
import com.termex.app.core.ssh.SSHClient
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.SshConfigBuilder
import com.termex.app.core.ssh.TerminalBuffer
import com.termex.app.domain.Server
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

data class TerminalPaneState(
    val server: Server,
    val connectionState: SSHConnectionState = SSHConnectionState.Disconnected,
    val lines: List<TerminalBuffer.TerminalLine> = emptyList(),
    val cursorPosition: Pair<Int, Int> = 0 to 0
)

data class HostKeyPrompt(
    val serverId: String,
    val result: HostKeyVerificationResult
)

@HiltViewModel
class MultiTerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workplaceRepository: WorkplaceRepository,
    private val sshClientProvider: Provider<SSHClient>,
    private val sshConfigBuilder: SshConfigBuilder
) : ViewModel() {

    private val workplaceId: String = savedStateHandle.get<String>("workplaceId") ?: ""

    val servers: StateFlow<List<Server>> = workplaceRepository.getServersForWorkplace(workplaceId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _paneStates = MutableStateFlow<Map<String, TerminalPaneState>>(emptyMap())
    val paneStates: StateFlow<Map<String, TerminalPaneState>> = _paneStates.asStateFlow()

    private val clients = java.util.concurrent.ConcurrentHashMap<String, SSHClient>()
    private val buffers = java.util.concurrent.ConcurrentHashMap<String, TerminalBuffer>()
    private val readJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val hostKeyDeferred = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val pendingPrompts = ArrayDeque<HostKeyPrompt>()

    private val _selectedPane = MutableStateFlow<String?>(null)
    val selectedPane: StateFlow<String?> = _selectedPane.asStateFlow()

    private val _hostKeyPrompt = MutableStateFlow<HostKeyPrompt?>(null)
    val hostKeyPrompt: StateFlow<HostKeyPrompt?> = _hostKeyPrompt.asStateFlow()

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

            client.setHostKeyVerificationCallback(object : HostKeyVerificationCallback {
                override suspend fun onVerificationRequired(result: HostKeyVerificationResult): Boolean {
                    updatePaneState(server.id) {
                        it.copy(connectionState = SSHConnectionState.VerifyingHostKey(result))
                    }
                    val deferred = CompletableDeferred<Boolean>()
                    hostKeyDeferred[server.id] = deferred
                    enqueuePrompt(HostKeyPrompt(server.id, result))
                    return deferred.await()
                }
            })

            val config = sshConfigBuilder.buildConfig(server, password)
            if (config == null || (config.password == null && config.privateKey == null)) {
                updatePaneState(server.id) {
                    it.copy(connectionState = SSHConnectionState.Error("Missing credentials"))
                }
                return@launch
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
            val inputStream = client.inputStream ?: return@launch
            val reader = java.io.InputStreamReader(inputStream, Charsets.UTF_8)
            val charBuffer = CharArray(4096)

            try {
                while (isActive && client.isConnected()) {
                    val charsRead = reader.read(charBuffer)
                    if (charsRead > 0) {
                        val data = String(charBuffer, 0, charsRead)
                        buffer.write(data)
                        updatePaneState(serverId) {
                            it.copy(
                                lines = buffer.contentFlow.value,
                                cursorPosition = buffer.cursorPosition.value
                            )
                        }
                    } else if (charsRead == -1) {
                        updatePaneState(serverId) {
                            it.copy(connectionState = SSHConnectionState.Disconnected)
                        }
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
        hostKeyDeferred.remove(serverId)?.complete(false)
        clearPromptForServer(serverId)
        clients[serverId]?.setHostKeyVerificationCallback(null)
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

    fun acceptHostKey() {
        val prompt = _hostKeyPrompt.value ?: return
        viewModelScope.launch {
            clients[prompt.serverId]?.trustHostKey(prompt.result)
            hostKeyDeferred.remove(prompt.serverId)?.complete(true)
            advancePrompt()
        }
    }

    fun rejectHostKey() {
        val prompt = _hostKeyPrompt.value ?: return
        hostKeyDeferred.remove(prompt.serverId)?.complete(false)
        advancePrompt()
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

    private fun enqueuePrompt(prompt: HostKeyPrompt) {
        if (_hostKeyPrompt.value == null) {
            _hostKeyPrompt.value = prompt
        } else {
            pendingPrompts.addLast(prompt)
        }
    }

    private fun advancePrompt() {
        _hostKeyPrompt.value = if (pendingPrompts.isEmpty()) null else pendingPrompts.removeFirst()
    }

    private fun clearPromptForServer(serverId: String) {
        val current = _hostKeyPrompt.value
        if (current?.serverId == serverId) {
            advancePrompt()
        }
        if (pendingPrompts.isNotEmpty()) {
            val iterator = pendingPrompts.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().serverId == serverId) {
                    iterator.remove()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectAll()
    }
}
