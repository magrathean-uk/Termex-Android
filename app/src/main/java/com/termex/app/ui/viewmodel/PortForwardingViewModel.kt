package com.termex.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.ssh.HostKeyVerificationCallback
import com.termex.app.core.ssh.HostKeyVerificationResult
import com.termex.app.core.ssh.PortForwardManager
import com.termex.app.core.ssh.SSHClient
import com.termex.app.core.ssh.SSHConnectionConfig
import com.termex.app.core.ssh.SSHConnectionState
import com.termex.app.core.ssh.SshConfigBuilder
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class PortForwardFormState(
    val type: PortForwardType = PortForwardType.LOCAL,
    val localPort: String = "",
    val remoteHost: String = "localhost",
    val remotePort: String = "",
    val bindAddress: String = "127.0.0.1",
    val isEditing: Boolean = false,
    val editingId: String? = null
)

@HiltViewModel
class PortForwardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val portForwardManager: PortForwardManager,
    private val sshClient: SSHClient,
    private val sshConfigBuilder: SshConfigBuilder
) : ViewModel() {

    private val serverId: String = savedStateHandle.get<String>("serverId") ?: ""

    private val _server = MutableStateFlow<Server?>(null)
    val server: StateFlow<Server?> = _server.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _formState = MutableStateFlow(PortForwardFormState())
    val formState: StateFlow<PortForwardFormState> = _formState.asStateFlow()

    val activeForwards = portForwardManager.activeForwards

    private val _connectionState = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Disconnected)
    val connectionState: StateFlow<SSHConnectionState> = _connectionState.asStateFlow()

    private val _needsPassword = MutableStateFlow(false)
    val needsPassword: StateFlow<Boolean> = _needsPassword.asStateFlow()

    private val _hostKeyVerification = MutableStateFlow<HostKeyVerificationResult?>(null)
    val hostKeyVerification: StateFlow<HostKeyVerificationResult?> = _hostKeyVerification.asStateFlow()

    private val connectionMutex = Mutex()
    private var pendingServerId: String? = null
    private var pendingForward: PortForward? = null
    private var hostKeyVerificationDeferred: CompletableDeferred<Boolean>? = null
    private var pendingReconnectConfig: SSHConnectionConfig? = null

    init {
        if (serverId.isBlank()) {
            _connectionState.value = SSHConnectionState.Error("Server not found")
        } else {
            loadServer()
        }
        sshClient.setHostKeyVerificationCallback(object : HostKeyVerificationCallback {
            override fun onVerificationRequiredAsync(result: HostKeyVerificationResult) {
                _hostKeyVerification.value = result
                _connectionState.value = SSHConnectionState.VerifyingHostKey(result)
                if (hostKeyVerificationDeferred?.isActive != true) {
                    hostKeyVerificationDeferred = CompletableDeferred()
                }
            }
        })
    }

    private fun loadServer() {
        viewModelScope.launch {
            val s = serverRepository.getServer(serverId)
            _server.value = s
            if (s == null) {
                _connectionState.value = SSHConnectionState.Error("Server not found")
                return@launch
            }
            s?.let { portForwardManager.initializeForwards(serverId, it.portForwards) }
        }
    }

    fun showAddDialog() {
        _formState.value = PortForwardFormState()
        _showDialog.value = true
    }

    fun showEditDialog(portForward: PortForward) {
        _formState.value = PortForwardFormState(
            type = portForward.type,
            localPort = portForward.localPort.toString(),
            remoteHost = portForward.remoteHost,
            remotePort = portForward.remotePort.toString(),
            bindAddress = portForward.bindAddress,
            isEditing = true,
            editingId = portForward.id
        )
        _showDialog.value = true
    }

    fun dismissDialog() {
        _showDialog.value = false
        _formState.value = PortForwardFormState()
    }

    fun updateType(type: PortForwardType) {
        _formState.value = _formState.value.copy(type = type)
    }

    fun updateLocalPort(port: String) {
        _formState.value = _formState.value.copy(localPort = port.filter { it.isDigit() })
    }

    fun updateRemoteHost(host: String) {
        _formState.value = _formState.value.copy(remoteHost = host)
    }

    fun updateRemotePort(port: String) {
        _formState.value = _formState.value.copy(remotePort = port.filter { it.isDigit() })
    }

    fun updateBindAddress(address: String) {
        _formState.value = _formState.value.copy(bindAddress = address)
    }

    fun savePortForward() {
        val form = _formState.value
        val localPort = form.localPort.toIntOrNull() ?: return
        val remotePort = form.remotePort.toIntOrNull() ?: 0

        viewModelScope.launch {
            val currentServer = _server.value ?: return@launch
            val currentForwards = currentServer.portForwards.toMutableList()

            if (form.isEditing && form.editingId != null) {
                val index = currentForwards.indexOfFirst { it.id == form.editingId }
                if (index >= 0) {
                    currentForwards[index] = currentForwards[index].copy(
                        type = form.type,
                        localPort = localPort,
                        remoteHost = form.remoteHost,
                        remotePort = remotePort,
                        bindAddress = form.bindAddress
                    )
                }
            } else {
                currentForwards.add(
                    PortForward(
                        type = form.type,
                        localPort = localPort,
                        remoteHost = form.remoteHost,
                        remotePort = remotePort,
                        bindAddress = form.bindAddress
                    )
                )
            }

            val updatedServer = currentServer.copy(portForwards = currentForwards)
            serverRepository.updateServer(updatedServer)
            _server.value = updatedServer
            portForwardManager.initializeForwards(serverId, currentForwards)
            dismissDialog()
        }
    }

    fun deletePortForward(portForward: PortForward) {
        viewModelScope.launch {
            val currentServer = _server.value ?: return@launch
            val updatedForwards = currentServer.portForwards.filter { it.id != portForward.id }
            val updatedServer = currentServer.copy(portForwards = updatedForwards)
            serverRepository.updateServer(updatedServer)
            _server.value = updatedServer
            portForwardManager.stopForward(serverId, portForward.id)
            portForwardManager.initializeForwards(serverId, updatedForwards)
        }
    }

    fun togglePortForward(portForward: PortForward) {
        val activeForward = activeForwards.value.find {
            it.sessionKey == serverId && it.config.id == portForward.id
        }
        if (activeForward?.isActive == true) {
            portForwardManager.stopForward(serverId, portForward.id)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                pendingForward = portForward
                if (ensureConnected()) {
                    pendingForward = null
                    portForwardManager.startForward(serverId, portForward)
                } else if (!_needsPassword.value && _hostKeyVerification.value !is HostKeyVerificationResult.Changed) {
                    pendingForward = null
                }
            }
        }
    }

    fun providePassword(password: String) {
        _needsPassword.value = false
        val serverId = pendingServerId
        if (serverId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val server = serverRepository.getServer(serverId) ?: return@launch
                val config = sshConfigBuilder.buildConfig(server, passwordOverride = password) ?: return@launch
                pendingReconnectConfig = config
                val result = sshClient.connect(config)
                if (result.isSuccess) {
                    _connectionState.value = SSHConnectionState.Connected
                    portForwardManager.setClient(serverId, sshClient)
                    pendingForward?.let { portForwardManager.startForward(serverId, it) }
                    pendingForward = null
                } else if (_hostKeyVerification.value !is HostKeyVerificationResult.Changed) {
                    _connectionState.value = SSHConnectionState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to connect"
                    )
                }
            }
        }
        pendingServerId = null
    }

    fun cancelPasswordPrompt() {
        _needsPassword.value = false
        pendingServerId = null
        pendingForward = null
    }

    fun acceptHostKey() {
        viewModelScope.launch {
            val verification = _hostKeyVerification.value ?: return@launch
            sshClient.trustHostKey(verification)
            _hostKeyVerification.value = null
            hostKeyVerificationDeferred?.complete(true)
            hostKeyVerificationDeferred = null
            if (verification is HostKeyVerificationResult.Changed) {
                val config = pendingReconnectConfig ?: return@launch
                val result = sshClient.connect(config)
                if (result.isSuccess) {
                    _connectionState.value = SSHConnectionState.Connected
                    portForwardManager.setClient(serverId, sshClient)
                    pendingForward?.let { portForwardManager.startForward(serverId, it) }
                    pendingForward = null
                }
            }
        }
    }

    fun rejectHostKey() {
        _hostKeyVerification.value = null
        hostKeyVerificationDeferred?.complete(false)
        hostKeyVerificationDeferred = null
        pendingForward = null
        _connectionState.value = SSHConnectionState.Disconnected
        viewModelScope.launch(Dispatchers.IO) {
            pendingReconnectConfig = null
            portForwardManager.setClient(serverId, null)
            sshClient.disconnect()
        }
    }

    private suspend fun ensureConnected(): Boolean {
        return connectionMutex.withLock {
            if (sshClient.isConnected()) return@withLock true
            val server = _server.value ?: return@withLock false
            val config = sshConfigBuilder.buildConfig(server)
            if (config == null) {
                _connectionState.value = SSHConnectionState.Error("Invalid connection config")
                return@withLock false
            }
            if (config.password == null && config.privateKey == null) {
                pendingServerId = server.id
                _needsPassword.value = true
                _connectionState.value = SSHConnectionState.Error("Password required")
                return@withLock false
            }
            _connectionState.value = SSHConnectionState.Connecting
            pendingReconnectConfig = config
            val result = sshClient.connect(config)
            if (result.isSuccess) {
                _connectionState.value = SSHConnectionState.Connected
                portForwardManager.setClient(serverId, sshClient)
                return@withLock true
            }
            if (_hostKeyVerification.value !is HostKeyVerificationResult.Changed) {
                _connectionState.value = SSHConnectionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to connect"
                )
            }
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        hostKeyVerificationDeferred?.complete(false)
        hostKeyVerificationDeferred = null
        pendingReconnectConfig = null
        sshClient.setHostKeyVerificationCallback(null)
        portForwardManager.setClient(serverId, null)
        sshClient.disconnect()
    }
}
