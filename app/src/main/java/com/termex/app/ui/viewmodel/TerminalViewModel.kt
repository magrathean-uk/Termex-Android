package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.demo.DemoTerminal
import com.termex.app.core.ssh.HostKeyVerificationCallback
import com.termex.app.core.ssh.HostKeyVerificationResult
import com.termex.app.core.ssh.SSHClient
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.TermexHostKeyVerifier
import com.termex.app.core.ssh.TerminalBuffer
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.AuthMode
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sshClient: SSHClient,
    private val serverRepository: ServerRepository,
    private val hostKeyVerifier: TermexHostKeyVerifier,
    private val userPreferencesRepository: UserPreferencesRepository
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

    private var pendingConfig: SSHConnectionConfig? = null
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
                if (!isInDemoMode) {
                    _connectionState.value = state
                }
            }
        }

        sshClient.setHostKeyVerificationCallback(object : HostKeyVerificationCallback {
            override suspend fun onVerificationRequired(result: HostKeyVerificationResult): Boolean {
                _hostKeyVerification.value = result
                hostKeyVerificationDeferred = CompletableDeferred()
                return hostKeyVerificationDeferred!!.await()
            }
        })
    }

    fun acceptHostKey() {
        viewModelScope.launch {
            val verification = _hostKeyVerification.value
            if (verification != null) {
                when (verification) {
                    is HostKeyVerificationResult.Unknown -> {
                        hostKeyVerifier.acceptHostKey(
                            hostname = verification.hostname,
                            port = verification.port,
                            keyType = verification.keyType,
                            fingerprint = verification.fingerprint,
                            publicKey = verification.publicKey
                        )
                    }
                    is HostKeyVerificationResult.Changed -> {
                        hostKeyVerifier.replaceHostKey(
                            hostname = verification.hostname,
                            port = verification.port,
                            keyType = verification.keyType,
                            fingerprint = verification.newFingerprint,
                            publicKey = verification.publicKey
                        )
                    }
                    is HostKeyVerificationResult.Trusted -> {}
                }
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

            val server = serverRepository.getServer(serverId) ?: return@launch
            _currentServer.value = server

            // Check if this is a demo server
            if (server.isDemo) {
                connectToDemo()
                return@launch
            }

            val savedPassword = server.passwordKeychainID?.takeIf { it.isNotBlank() }
            val effectivePassword = password?.takeIf { it.isNotBlank() } ?: savedPassword

            val keyPath = server.keyId?.takeIf { it.isNotBlank() }
            val privateKeyBytes = keyPath?.let { path ->
                val keyFile = File(path)
                if (keyFile.exists()) keyFile.readBytes() else null
            }

            val shouldPromptPassword = effectivePassword == null && privateKeyBytes == null
            if (shouldPromptPassword) {
                // Need to prompt for password
                pendingConfig = SSHConnectionConfig(
                    hostname = server.hostname,
                    port = server.port,
                    username = server.username
                )
                _needsPassword.value = true
                return@launch
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
        pendingConfig?.let { config ->
            viewModelScope.launch {
                performConnect(config.copy(password = password))
            }
        }
        pendingConfig = null
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
        sshClient.resizeTerminal(cols, rows)
    }
    
    fun disconnect() {
        readJob?.cancel()
        readJob = null

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
        disconnect()
    }
}
