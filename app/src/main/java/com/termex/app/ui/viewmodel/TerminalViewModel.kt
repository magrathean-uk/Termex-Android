package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.demo.DemoTerminal
import com.termex.app.core.ssh.HostKeyVerificationCallback
import com.termex.app.core.ssh.HostKeyVerificationResult
import com.termex.app.core.ssh.SSHClient
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.SshConfigBuilder
import com.termex.app.core.ssh.TerminalBuffer
import com.termex.app.data.prefs.TerminalSettings
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.Snippet
import com.termex.app.domain.SnippetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sshClient: SSHClient,
    private val serverRepository: ServerRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sshConfigBuilder: SshConfigBuilder,
    private val sessionRepository: com.termex.app.data.repository.SessionRepository,
    private val snippetRepository: SnippetRepository
) : ViewModel() {

    private val _connectionState = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Disconnected)
    val connectionState: StateFlow<SSHConnectionState> = _connectionState.asStateFlow()

    // Expose buffer directly - its internal flows handle reactivity
    private val terminalBuffer = TerminalBuffer()
    val terminalLines: StateFlow<List<TerminalBuffer.TerminalLine>> = terminalBuffer.contentFlow
    val cursorPosition: StateFlow<Pair<Int, Int>> = terminalBuffer.cursorPosition

    private val _currentServer = MutableStateFlow<Server?>(null)
    val currentServer: StateFlow<Server?> = _currentServer.asStateFlow()

    private var readJob: Job? = null

    // Demo mode support
    private var demoTerminal: DemoTerminal? = null
    private var isInDemoMode = false
    private var globalDemoModeEnabled = false

    // For credential prompting
    private val _needsPassword = MutableStateFlow(false)
    val needsPassword: StateFlow<Boolean> = _needsPassword.asStateFlow()

    // For host key verification
    private val _hostKeyVerification = MutableStateFlow<HostKeyVerificationResult?>(null)
    val hostKeyVerification: StateFlow<HostKeyVerificationResult?> = _hostKeyVerification.asStateFlow()

    private var pendingServerId: String? = null
    private var hostKeyVerificationDeferred: CompletableDeferred<Boolean>? = null

    // Snippets
    val snippets: StateFlow<List<Snippet>> = snippetRepository.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Terminal settings (font, colors)
    val terminalSettings: StateFlow<TerminalSettings> = userPreferencesRepository.terminalSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TerminalSettings())

    private val _showSnippetPicker = MutableStateFlow(false)
    val showSnippetPicker: StateFlow<Boolean> = _showSnippetPicker.asStateFlow()

    fun showSnippetPicker() { _showSnippetPicker.value = true }
    fun hideSnippetPicker() { _showSnippetPicker.value = false }

    fun insertSnippet(snippet: Snippet) {
        sendInput(snippet.command)
        _showSnippetPicker.value = false
    }

    init {
        // Track global demo mode setting
        viewModelScope.launch {
            userPreferencesRepository.demoModeEnabledFlow.collect { enabled ->
                globalDemoModeEnabled = enabled
            }
        }

        // Sync connection state from SSH client
        viewModelScope.launch {
            sshClient.connectionState.collect { state ->
                if (!isInDemoMode && _hostKeyVerification.value == null) {
                    _connectionState.value = state
                }
            }
        }

        sshClient.setHostKeyVerificationCallback(object : HostKeyVerificationCallback {
            override suspend fun onVerificationRequired(result: HostKeyVerificationResult): Boolean {
                _hostKeyVerification.value = result
                _connectionState.value = SSHConnectionState.VerifyingHostKey(result)
                hostKeyVerificationDeferred = CompletableDeferred()
                return hostKeyVerificationDeferred!!.await()
            }
        })
    }

    fun acceptHostKey() {
        viewModelScope.launch {
            val verification = _hostKeyVerification.value
            if (verification != null) {
                sshClient.trustHostKey(verification)
                _hostKeyVerification.value = null
                hostKeyVerificationDeferred?.complete(true)
                hostKeyVerificationDeferred = null
            }
        }
    }

    fun rejectHostKey() {
        _hostKeyVerification.value = null
        hostKeyVerificationDeferred?.complete(false)
        hostKeyVerificationDeferred = null
    }

    fun connect(serverId: String, password: String? = null) {
        viewModelScope.launch {
            _needsPassword.value = false
            _hostKeyVerification.value = null
            hostKeyVerificationDeferred?.complete(false)
            hostKeyVerificationDeferred = null
            // Handle demo server specially
            if (serverId == Server.DEMO_SERVER_ID) {
                connectToDemo()
                return@launch
            }

            // DEMO MODE: Block ALL real connections - only demo server works
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

            // Check if this is a demo server
            if (server.isDemo) {
                connectToDemo()
                return@launch
            }

            val config = sshConfigBuilder.buildConfig(server, password)
            if (config == null) {
                _connectionState.value = SSHConnectionState.Error("Invalid connection config")
                return@launch
            }
            val shouldPromptPassword = config.password == null && config.privateKey == null
            if (shouldPromptPassword) {
                pendingServerId = server.id
                _needsPassword.value = true
                return@launch
            }
            performConnect(config)
        }
    }

    private fun connectToDemo() {
        isInDemoMode = true
        _currentServer.value = Server.createDemoServer()
        _connectionState.value = SSHConnectionState.Connecting

        viewModelScope.launch {
            // Small delay to simulate connection
            kotlinx.coroutines.delay(500)

            demoTerminal = DemoTerminal()
            demoTerminal?.reset()

            // Write welcome message to terminal buffer
            terminalBuffer.write(demoTerminal?.getWelcomeMessage() ?: "")

            _connectionState.value = SSHConnectionState.Connected

            // Start collecting demo output
            readJob = viewModelScope.launch {
                demoTerminal?.output?.collect { output ->
                    if (output.isNotEmpty()) {
                        // Clear and rewrite - demo terminal manages its own state
                        terminalBuffer.clear()
                        terminalBuffer.write(output)
                    }
                }
            }
        }
    }
    
    fun providePassword(password: String) {
        _needsPassword.value = false
        val serverId = pendingServerId
        pendingServerId = null
        if (serverId != null) {
            viewModelScope.launch {
                val server = serverRepository.getServer(serverId) ?: return@launch
                val config = sshConfigBuilder.buildConfig(server, passwordOverride = password) ?: return@launch
                performConnect(config)
            }
        }
    }
    
    private suspend fun performConnect(config: SSHConnectionConfig) {
        val result = sshClient.connect(config)
        
        if (result.isSuccess) {
            // Try to restore previous session state
            _currentServer.value?.let { server ->
                val restored = restoreSessionState(server.id)
                if (restored != null) {
                    // Session restored - terminal buffer already populated
                } else {
                    // No saved session - fresh connection
                }
            }
            startReading()
        }
    }
    
    private fun startReading() {
        readJob?.cancel()
        readJob = viewModelScope.launch(Dispatchers.IO) {
            val inputStream = sshClient.inputStream ?: return@launch
            val reader = java.io.InputStreamReader(inputStream, Charsets.UTF_8)
            val charBuffer = CharArray(4096)
            
            try {
                while (isActive && sshClient.isConnected()) {
                    val charsRead = reader.read(charBuffer)
                    if (charsRead > 0) {
                        val data = String(charBuffer, 0, charsRead)
                        terminalBuffer.write(data)
                    } else if (charsRead == -1) {
                        sshClient.disconnect()
                        break
                    }
                }
            } catch (e: Exception) {
                // Connection closed or error
            }
        }
    }
    
    fun sendInput(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isInDemoMode) {
                demoTerminal?.processInput(data)
            } else {
                sshClient.sendData(data)
            }
            // Auto-scroll to bottom on input
            terminalBuffer.scrollToBottom()
        }
    }
    
    fun sendKey(char: Char) {
        sendInput(char.toString())
    }
    
    fun resizeTerminal(cols: Int, rows: Int) {
        terminalBuffer.resize(cols, rows)
        sshClient.resizeTerminal(cols, rows)
    }

    fun resizeTerminal(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        terminalBuffer.resize(cols, rows)
        sshClient.resizeTerminal(cols, rows, widthPx, heightPx)
    }
    
    fun scrollTerminal(deltaLines: Int) {
        if (deltaLines > 0) {
            terminalBuffer.scrollUp(deltaLines)
        } else if (deltaLines < 0) {
            terminalBuffer.scrollDown(-deltaLines)
        }
    }
    
    fun disconnect() {
        readJob?.cancel()
        readJob = null
        _hostKeyVerification.value = null
        hostKeyVerificationDeferred?.complete(false)
        hostKeyVerificationDeferred = null
        _needsPassword.value = false

        if (isInDemoMode) {
            isInDemoMode = false
            demoTerminal = null
            _connectionState.value = SSHConnectionState.Disconnected
        } else {
            sshClient.disconnect()
        }

        terminalBuffer.clear()
        _currentServer.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        sshClient.setHostKeyVerificationCallback(null)
        
        // Capture buffer content BEFORE disconnect clears it
        val server = _currentServer.value
        val bufferSnapshot = if (server != null) {
            try {
                terminalBuffer.contentFlow.value
                    .takeLast(500)
                    .joinToString("\n") { line ->
                        line.cells.joinToString("") { it.char.toString() }
                    }
            } catch (_: Exception) { null }
        } else null
        
        disconnect()
        
        // Save session with NonCancellable since viewModelScope is cancelled
        if (server != null && bufferSnapshot != null) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO + NonCancellable).launch {
                try {
                    val sessionState = com.termex.app.domain.SessionState(
                        id = java.util.UUID.randomUUID().toString(),
                        serverId = server.id,
                        terminalBuffer = bufferSnapshot,
                        workingDirectory = null,
                        connectedAt = System.currentTimeMillis(),
                        lastActiveAt = System.currentTimeMillis()
                    )
                    sessionRepository.saveSession(sessionState)
                } catch (_: Exception) {}
            }
        }
    }
    
    suspend fun restoreSessionState(serverId: String): com.termex.app.domain.SessionState? {
        return try {
            val session = sessionRepository.getLatestSessionForServer(serverId)
            session?.let {
                // Restore terminal buffer
                terminalBuffer.write(it.terminalBuffer)
            }
            session
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun clearAllSessions() {
        sessionRepository.deleteAllSessions()
    }
}
