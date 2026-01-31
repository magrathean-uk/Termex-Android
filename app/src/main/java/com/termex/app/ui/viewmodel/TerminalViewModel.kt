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
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sshClient: SSHClient,
    private val serverRepository: ServerRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sshConfigBuilder: SshConfigBuilder
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
            startReading()
        }
    }
    
    private fun startReading() {
        readJob?.cancel()
        readJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(8192)
            val inputStream = sshClient.inputStream ?: return@launch
            
            try {
                while (isActive && sshClient.isConnected()) {
                    // Blocking read is efficient and correct for network streams
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        terminalBuffer.write(data)
                    } else if (bytesRead == -1) {
                        // End of stream
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
        viewModelScope.launch {
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
        disconnect()
    }
}
