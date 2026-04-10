package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.demo.DemoTerminal
import com.termex.app.core.ssh.ConnectionManager
import com.termex.app.core.ssh.HostKeyVerificationCallback
import com.termex.app.core.ssh.HostKeyVerificationResult
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.SshConfigBuilder
import com.termex.app.core.ssh.TerminalBuffer
import com.termex.app.data.diagnostics.DiagnosticEvent
import com.termex.app.data.diagnostics.DiagnosticSeverity
import com.termex.app.data.diagnostics.DiagnosticsRepository
import com.termex.app.data.prefs.TerminalSettings
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.Snippet
import com.termex.app.domain.SnippetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val serverRepository: ServerRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sshConfigBuilder: SshConfigBuilder,
    private val sessionRepository: com.termex.app.data.repository.SessionRepository,
    private val snippetRepository: SnippetRepository,
    private val diagnosticsRepository: DiagnosticsRepository
) : ViewModel() {

    // Session key — set when connect() is called, used to address ConnectionManager
    @Volatile private var sessionKey: String? = null

    // Local state flows — these mirror or derive from ConnectionManager session state
    private val _connectionState = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Disconnected)
    val connectionState: StateFlow<SSHConnectionState> = _connectionState.asStateFlow()

    // Terminal buffer and cursor come from ConnectionManager session (live as long as connection does)
    private val _terminalLines = MutableStateFlow<List<TerminalBuffer.TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalBuffer.TerminalLine>> = _terminalLines.asStateFlow()
    private val _cursorPosition = MutableStateFlow(0 to 0)
    val cursorPosition: StateFlow<Pair<Int, Int>> = _cursorPosition.asStateFlow()

    private val _currentServer = MutableStateFlow<Server?>(null)
    val currentServer: StateFlow<Server?> = _currentServer.asStateFlow()

    // Demo mode
    private var demoTerminal: DemoTerminal? = null
    private var isInDemoMode = false
    private var globalDemoModeEnabled = false

    // Credential prompting
    private val _needsPassword = MutableStateFlow(false)
    val needsPassword: StateFlow<Boolean> = _needsPassword.asStateFlow()

    // Host key verification
    private val _hostKeyVerification = MutableStateFlow<HostKeyVerificationResult?>(null)
    val hostKeyVerification: StateFlow<HostKeyVerificationResult?> = _hostKeyVerification.asStateFlow()

    private var pendingServerId: String? = null
    private var pendingReconnectConfig: SSHConnectionConfig? = null

    // Snippets
    val snippets: StateFlow<List<Snippet>> = snippetRepository.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Terminal settings
    val terminalSettings: StateFlow<TerminalSettings> = userPreferencesRepository.terminalSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TerminalSettings())

    val recentDiagnosticEvents: StateFlow<List<DiagnosticEvent>> = combine(
        diagnosticsRepository.events,
        _currentServer
    ) { events, server ->
        val serverId = server?.id ?: return@combine emptyList()
        events.filter { it.serverId == serverId }.take(20)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _showSnippetPicker = MutableStateFlow(false)
    val showSnippetPicker: StateFlow<Boolean> = _showSnippetPicker.asStateFlow()

    fun showSnippetPicker() { _showSnippetPicker.value = true }
    fun hideSnippetPicker() { _showSnippetPicker.value = false }

    fun insertSnippet(snippet: Snippet) {
        sendInput(snippet.command)
        _showSnippetPicker.value = false
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.demoModeEnabledFlow.collect { enabled ->
                globalDemoModeEnabled = enabled
            }
        }
    }

    fun connect(serverId: String, password: String? = null) {
        viewModelScope.launch {
            _needsPassword.value = false
            _hostKeyVerification.value = null

            if (serverId == Server.DEMO_SERVER_ID) {
                connectToDemo()
                return@launch
            }
            if (globalDemoModeEnabled) {
                _connectionState.value = SSHConnectionState.Error("Demo mode: Real connections disabled")
                return@launch
            }

            val server = serverRepository.getServer(serverId)
            if (server == null) {
                _connectionState.value = SSHConnectionState.Error("Server not found")
                return@launch
            }
            _currentServer.value = server

            if (server.isDemo) {
                connectToDemo()
                return@launch
            }

            // Re-attach to an already-live connection — no need to reconnect
            if (connectionManager.isConnected(serverId)) {
                sessionKey = serverId
                _connectionState.value = SSHConnectionState.Connected
                observeSession(serverId)
                return@launch
            }

            val config = sshConfigBuilder.buildConfig(server, password)
            if (config == null) {
                _connectionState.value = SSHConnectionState.Error("Invalid connection config")
                return@launch
            }
            if (config.password == null && config.privateKey == null) {
                pendingServerId = server.id
                _needsPassword.value = true
                return@launch
            }
            performConnect(serverId, config)
        }
    }

    private fun connectToDemo() {
        isInDemoMode = true
        _currentServer.value = Server.createDemoServer()
        _connectionState.value = SSHConnectionState.Connecting
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            demoTerminal = DemoTerminal()
            demoTerminal?.reset()
            val demoBuffer = TerminalBuffer()
            demoBuffer.write(demoTerminal?.getWelcomeMessage() ?: "")
            _terminalLines.value = demoBuffer.contentFlow.value
            _connectionState.value = SSHConnectionState.Connected
            diagnosticsRepository.record(
                category = "connection",
                title = "Demo session started",
                detail = "Demo mode terminal",
                serverId = Server.DEMO_SERVER_ID
            )
            launch {
                demoTerminal?.output?.collect { output ->
                    if (output.isNotEmpty()) {
                        demoBuffer.clear()
                        demoBuffer.write(output)
                        _terminalLines.value = demoBuffer.contentFlow.value
                        _cursorPosition.value = demoBuffer.cursorPosition.value
                    }
                }
            }
        }
    }

    fun providePassword(password: String) {
        _needsPassword.value = false
        val serverId = pendingServerId ?: return
        pendingServerId = null
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId) ?: return@launch
            val config = sshConfigBuilder.buildConfig(server, passwordOverride = password) ?: return@launch
            performConnect(serverId, config)
        }
    }

    private suspend fun performConnect(serverId: String, config: SSHConnectionConfig) {
        sessionKey = serverId
        pendingReconnectConfig = config

        // Fire-and-forget callback — never blocks the MINA NIO2 thread
        val hostKeyCallback = object : HostKeyVerificationCallback {
            override fun onVerificationRequiredAsync(result: HostKeyVerificationResult) {
                viewModelScope.launch {
                    diagnosticsRepository.record(
                        category = "host_key",
                        title = when (result) {
                            is HostKeyVerificationResult.Unknown -> "Unknown host key"
                            is HostKeyVerificationResult.Changed -> "Host key changed"
                            HostKeyVerificationResult.Trusted -> "Trusted host key"
                        },
                        detail = hostKeySummary(result),
                        serverId = serverId,
                        severity = if (result is HostKeyVerificationResult.Changed) DiagnosticSeverity.WARNING else DiagnosticSeverity.INFO
                    )
                    _hostKeyVerification.value = result
                    _connectionState.value = SSHConnectionState.VerifyingHostKey(result)
                }
            }
        }

        _connectionState.value = SSHConnectionState.Connecting
        val result = connectionManager.connect(serverId, config, hostKeyCallback)
        if (result.isSuccess) {
            observeSession(serverId)
        } else if (_hostKeyVerification.value !is HostKeyVerificationResult.Changed) {
            _connectionState.value = SSHConnectionState.Error(
                result.exceptionOrNull()?.message ?: "Connection failed"
            )
        }
    }

    private fun observeSession(serverId: String) {
        // Mirror ConnectionManager state → ViewModel state
        viewModelScope.launch {
            connectionManager.getState(serverId)?.collect { state ->
                if (!isInDemoMode && _hostKeyVerification.value == null) {
                    _connectionState.value = state
                }
            }
        }
        // Mirror buffer → ViewModel lines
        viewModelScope.launch {
            connectionManager.getBuffer(serverId)?.let { buffer ->
                buffer.contentFlow.collect { lines ->
                    _terminalLines.value = lines
                    _cursorPosition.value = buffer.cursorPosition.value
                }
            }
        }
        viewModelScope.launch {
            connectionManager.getBuffer(serverId)?.cursorPosition?.collect { pos ->
                _cursorPosition.value = pos
            }
        }
    }

    fun acceptHostKey() {
        viewModelScope.launch {
            val verification = _hostKeyVerification.value ?: return@launch
            val key = sessionKey ?: return@launch
            diagnosticsRepository.record(
                category = "host_key",
                title = "Host key accepted",
                detail = hostKeySummary(verification),
                serverId = key,
                severity = if (verification is HostKeyVerificationResult.Changed) DiagnosticSeverity.WARNING else DiagnosticSeverity.INFO
            )
            // Save key to DB, then clear the dialog and update state
            connectionManager.trustHostKey(verification)
            _hostKeyVerification.value = null
            if (verification is HostKeyVerificationResult.Changed) {
                val reconnectConfig = pendingReconnectConfig ?: return@launch
                performConnect(key, reconnectConfig)
            } else {
                _connectionState.value = connectionManager.getState(key)?.value
                    ?: SSHConnectionState.Connected
            }
        }
    }

    fun rejectHostKey() {
        val key = sessionKey
        val verification = _hostKeyVerification.value
        _hostKeyVerification.value = null
        _connectionState.value = SSHConnectionState.Disconnected
        if (key != null) {
            viewModelScope.launch {
                diagnosticsRepository.record(
                    category = "host_key",
                    title = "Host key rejected",
                    detail = verification?.let(::hostKeySummary),
                    serverId = key,
                    severity = DiagnosticSeverity.WARNING
                )
                connectionManager.disconnect(key)
            }
        }
    }

    fun sendInput(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val key = sessionKey
            if (isInDemoMode) {
                demoTerminal?.processInput(data)
            } else if (key != null) {
                connectionManager.sendData(key, data)
            }
            connectionManager.getBuffer(key ?: return@launch)?.scrollToBottom()
        }
    }

    fun sendKey(char: Char) = sendInput(char.toString())

    fun resizeTerminal(cols: Int, rows: Int) {
        sessionKey?.let { connectionManager.resizeTerminal(it, cols, rows, 0, 0) }
    }

    fun resizeTerminal(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        sessionKey?.let { connectionManager.resizeTerminal(it, cols, rows, widthPx, heightPx) }
    }

    fun scrollTerminal(deltaLines: Int) {
        sessionKey?.let {
            val buffer = connectionManager.getBuffer(it) ?: return@let
            if (deltaLines > 0) buffer.scrollUp(deltaLines) else buffer.scrollDown(-deltaLines)
        }
    }

    fun disconnect() {
        val key = sessionKey ?: return
        sessionKey = null
        pendingReconnectConfig = null
        viewModelScope.launch(Dispatchers.IO) {
            connectionManager.disconnect(key)
        }
        _connectionState.value = SSHConnectionState.Disconnected
        _currentServer.value = null
    }

    override fun onCleared() {
        super.onCleared()
        val key = sessionKey
        connectionManager.clearHostKeyCallback(key ?: return)
        _hostKeyVerification.value = null
        pendingReconnectConfig = null

        // Save session state in background — connection stays alive in ConnectionManager
        val server = _currentServer.value
        if (server != null && key != null) {
            val bufferSnapshot = try {
                connectionManager.getBuffer(key)?.contentFlow?.value
                    ?.takeLast(500)
                    ?.joinToString("\n") { line -> line.cells.joinToString("") { it.char.toString() } }
            } catch (_: Exception) {
                null
            }
            if (bufferSnapshot != null) {
                @Suppress("OPT_IN_USAGE")
                GlobalScope.launch(NonCancellable + Dispatchers.IO) {
                    try {
                        withTimeout(5_000) {
                            val sessionState = com.termex.app.domain.SessionState(
                                id = java.util.UUID.randomUUID().toString(),
                                serverId = server.id,
                                terminalBuffer = bufferSnapshot,
                                workingDirectory = null,
                                connectedAt = System.currentTimeMillis(),
                                lastActiveAt = System.currentTimeMillis()
                            )
                            sessionRepository.saveSession(sessionState)
                        }
                    } catch (_: TimeoutCancellationException) {
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun hostKeySummary(result: HostKeyVerificationResult): String = when (result) {
        is HostKeyVerificationResult.Unknown -> "${result.hostname}:${result.port} ${result.fingerprint}"
        is HostKeyVerificationResult.Changed -> "${result.hostname}:${result.port} ${result.oldFingerprint} → ${result.newFingerprint}"
        HostKeyVerificationResult.Trusted -> "Trusted host"
    }
}
