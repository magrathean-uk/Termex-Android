package com.termex.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.ssh.ConnectionManager
import com.termex.app.core.ssh.HostKeyVerificationCallback
import com.termex.app.core.ssh.HostKeyVerificationResult
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.SshConfigBuilder
import com.termex.app.core.ssh.TerminalBuffer
import com.termex.app.domain.Server
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject

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
    private val connectionManager: ConnectionManager,
    private val sshConfigBuilder: SshConfigBuilder
) : ViewModel() {

    private val workplaceId: String = savedStateHandle.get<String>("workplaceId") ?: ""

    val servers: StateFlow<List<Server>> = workplaceRepository.getServersForWorkplace(workplaceId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _paneStates = MutableStateFlow<Map<String, TerminalPaneState>>(emptyMap())
    val paneStates: StateFlow<Map<String, TerminalPaneState>> = _paneStates.asStateFlow()

    private val pendingPrompts = ConcurrentLinkedDeque<HostKeyPrompt>()
    private val pendingConfigs = ConcurrentHashMap<String, SSHConnectionConfig>()
    // Guard against duplicate coroutine collectors per pane
    private val observedPanes = ConcurrentHashMap.newKeySet<String>()

    private val _selectedPane = MutableStateFlow<String?>(null)
    val selectedPane: StateFlow<String?> = _selectedPane.asStateFlow()

    private val _hostKeyPrompt = MutableStateFlow<HostKeyPrompt?>(null)
    val hostKeyPrompt: StateFlow<HostKeyPrompt?> = _hostKeyPrompt.asStateFlow()

    fun selectPane(serverId: String) {
        _selectedPane.value = serverId
    }

    fun connectServer(server: Server, password: String? = null) {
        // Re-attach to already-live session without reconnecting
        if (connectionManager.isConnected(server.id)) {
            updatePaneState(server.id) { it.copy(connectionState = SSHConnectionState.Connected) }
            observePane(server.id)
            return
        }

        viewModelScope.launch {
            updatePaneState(server.id) { it.copy(connectionState = SSHConnectionState.Connecting) }

            val hostKeyCallback = object : HostKeyVerificationCallback {
                override fun onVerificationRequiredAsync(result: HostKeyVerificationResult) {
                    viewModelScope.launch {
                        updatePaneState(server.id) {
                            it.copy(connectionState = SSHConnectionState.VerifyingHostKey(result))
                        }
                        enqueuePrompt(HostKeyPrompt(server.id, result))
                    }
                }
            }

            val config = sshConfigBuilder.buildConfig(server, password)
            if (config == null || (config.password == null && config.privateKey == null)) {
                updatePaneState(server.id) {
                    it.copy(connectionState = SSHConnectionState.Error("Missing credentials"))
                }
                return@launch
            }

            pendingConfigs[server.id] = config
            val result = connectionManager.connect(server.id, config, hostKeyCallback)
            if (result.isSuccess) {
                updatePaneState(server.id) { it.copy(connectionState = SSHConnectionState.Connected) }
                observePane(server.id)
            } else if (_hostKeyPrompt.value?.serverId != server.id ||
                _hostKeyPrompt.value?.result !is HostKeyVerificationResult.Changed
            ) {
                updatePaneState(server.id) {
                    it.copy(connectionState = SSHConnectionState.Error(
                        result.exceptionOrNull()?.message ?: "Failed"
                    ))
                }
            }
        }
    }

    private fun observePane(serverId: String) {
        // Prevent duplicate collectors if already observing this pane
        if (!observedPanes.add(serverId)) return

        // Mirror ConnectionManager session state → pane state
        viewModelScope.launch {
            connectionManager.getState(serverId)?.collect { state ->
                updatePaneState(serverId) { it.copy(connectionState = state) }
            }
        }
        // Mirror buffer content → pane lines
        viewModelScope.launch {
            connectionManager.getBuffer(serverId)?.let { buffer ->
                buffer.contentFlow.collect { lines ->
                    updatePaneState(serverId) {
                        it.copy(lines = lines, cursorPosition = buffer.cursorPosition.value)
                    }
                }
            }
        }
        viewModelScope.launch {
            connectionManager.getBuffer(serverId)?.cursorPosition?.collect { pos ->
                updatePaneState(serverId) { it.copy(cursorPosition = pos) }
            }
        }
    }

    fun sendInput(serverId: String, data: String) {
        connectionManager.sendData(serverId, data)
    }

    fun resizeTerminal(serverId: String, cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        connectionManager.resizeTerminal(serverId, cols, rows, widthPx, heightPx)
    }

    fun disconnectServer(serverId: String) {
        clearPromptForServer(serverId)
        pendingConfigs.remove(serverId)
        connectionManager.clearHostKeyCallback(serverId)
        connectionManager.disconnect(serverId)
        _paneStates.value = _paneStates.value.toMutableMap().apply { remove(serverId) }
    }

    fun disconnectAll() {
        servers.value.forEach { disconnectServer(it.id) }
    }

    fun acceptHostKey() {
        val prompt = _hostKeyPrompt.value ?: return
        viewModelScope.launch {
            connectionManager.trustHostKey(prompt.result)
            if (prompt.result is HostKeyVerificationResult.Changed) {
                val server = servers.value.firstOrNull { it.id == prompt.serverId }
                val config = pendingConfigs[prompt.serverId]
                advancePrompt()
                if (server != null && config != null) {
                    connectServer(server, config.password)
                }
                return@launch
            }
            updatePaneState(prompt.serverId) {
                it.copy(connectionState = connectionManager.getState(prompt.serverId)?.value
                    ?: SSHConnectionState.Connected)
            }
            advancePrompt()
        }
    }

    fun rejectHostKey() {
        val prompt = _hostKeyPrompt.value ?: return
        viewModelScope.launch {
            connectionManager.disconnect(prompt.serverId)
            updatePaneState(prompt.serverId) {
                it.copy(connectionState = SSHConnectionState.Disconnected)
            }
            advancePrompt()
        }
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
        _paneStates.value = servers.associate { server ->
            server.id to TerminalPaneState(server = server)
        }
    }

    private fun enqueuePrompt(prompt: HostKeyPrompt) {
        if (_hostKeyPrompt.value == null) {
            _hostKeyPrompt.value = prompt
        } else {
            pendingPrompts.addLast(prompt)
        }
    }

    private fun advancePrompt() {
        _hostKeyPrompt.value = if (pendingPrompts.isEmpty()) null else pendingPrompts.pollFirst()
    }

    private fun clearPromptForServer(serverId: String) {
        if (_hostKeyPrompt.value?.serverId == serverId) advancePrompt()
        pendingPrompts.removeIf { it.serverId == serverId }
    }

    override fun onCleared() {
        super.onCleared()
        observedPanes.clear()
        pendingConfigs.clear()
        // Only clear callbacks — connections stay alive in ConnectionManager
        servers.value.forEach { server ->
            connectionManager.clearHostKeyCallback(server.id)
        }
    }
}
