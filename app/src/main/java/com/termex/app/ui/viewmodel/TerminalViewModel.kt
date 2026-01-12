package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.ssh.SSHClient
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.TerminalBuffer
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val serverRepository: ServerRepository
) : ViewModel() {
    
    val connectionState: StateFlow<SSHConnectionState> = sshClient.connectionState
    
    private val _terminalBuffer = MutableStateFlow(TerminalBuffer())
    val terminalBuffer: StateFlow<TerminalBuffer> = _terminalBuffer.asStateFlow()
    
    private val _currentServer = MutableStateFlow<Server?>(null)
    val currentServer: StateFlow<Server?> = _currentServer.asStateFlow()
    
    private var readJob: Job? = null
    
    // For credential prompting
    private val _needsPassword = MutableStateFlow(false)
    val needsPassword: StateFlow<Boolean> = _needsPassword.asStateFlow()
    
    private var pendingConfig: SSHConnectionConfig? = null
    
    fun connect(serverId: String, password: String? = null) {
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId) ?: return@launch
            _currentServer.value = server
            
            // Build connection config
            val config = SSHConnectionConfig(
                hostname = server.hostname,
                port = server.port,
                username = server.username,
                password = password
                // TODO: Add key-based auth when implemented
            )
            
            if (password == null && server.passwordKeychainID == null && server.keyId == null) {
                // Need to prompt for password
                pendingConfig = config
                _needsPassword.value = true
                return@launch
            }
            
            performConnect(config)
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
                    val available = inputStream.available()
                    if (available > 0) {
                        val bytesRead = inputStream.read(buffer, 0, minOf(available, buffer.size))
                        if (bytesRead > 0) {
                            val data = String(buffer, 0, bytesRead, Charsets.UTF_8)
                            _terminalBuffer.value.write(data)
                        }
                    } else {
                        // Small delay to prevent busy waiting
                        kotlinx.coroutines.delay(10)
                    }
                }
            } catch (e: Exception) {
                // Connection closed or error
            }
        }
    }
    
    fun sendInput(data: String) {
        viewModelScope.launch {
            sshClient.sendData(data)
            // Auto-scroll to bottom on input
            _terminalBuffer.value.scrollToBottom()
        }
    }
    
    fun sendKey(char: Char) {
        sendInput(char.toString())
    }
    
    fun resizeTerminal(cols: Int, rows: Int) {
        sshClient.resizeTerminal(cols, rows)
        _terminalBuffer.value = _terminalBuffer.value.resize(cols, rows)
    }
    
    fun disconnect() {
        readJob?.cancel()
        readJob = null
        sshClient.disconnect()
        _terminalBuffer.value.clear()
        _currentServer.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
