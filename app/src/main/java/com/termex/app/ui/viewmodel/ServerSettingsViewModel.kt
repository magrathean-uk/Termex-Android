package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.AuthMode
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.SSHKey
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerFormState(
    val id: String? = null,
    val name: String = "",
    val hostname: String = "",
    val port: String = "22",
    val username: String = "",
    val authMode: AuthMode = AuthMode.PASSWORD,
    val password: String = "",
    val hasStoredPassword: Boolean = false,
    val keyId: String? = null,
    val selectedKeyName: String? = null,
    val jumpHostId: String? = null,
    val jumpHostName: String? = null,
    val forwardAgent: Boolean = false
)

@HiltViewModel
class ServerSettingsViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(ServerFormState())
    val formState: StateFlow<ServerFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val keys: StateFlow<List<SSHKey>> = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val servers: StateFlow<List<Server>> = serverRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadServer(serverId: String) {
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId) ?: return@launch
            val jumpHost = server.jumpHostId?.let { serverRepository.getServer(it) }
            val key = server.keyId?.let { keyId ->
                keys.value.find { it.path == keyId }
            }

            _formState.value = ServerFormState(
                id = server.id,
                name = server.name,
                hostname = server.hostname,
                port = server.port.toString(),
                username = server.username,
                authMode = server.authMode,
                password = "", // Don't load password for security
                hasStoredPassword = !server.passwordKeychainID.isNullOrEmpty(),
                keyId = server.keyId,
                selectedKeyName = key?.name,
                jumpHostId = server.jumpHostId,
                jumpHostName = jumpHost?.displayName,
                forwardAgent = server.forwardAgent
            )
        }
    }

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun updateHostname(hostname: String) {
        _formState.value = _formState.value.copy(hostname = hostname)
    }

    fun updatePort(port: String) {
        _formState.value = _formState.value.copy(port = port)
    }

    fun updateUsername(username: String) {
        _formState.value = _formState.value.copy(username = username)
    }

    fun updateAuthMode(authMode: AuthMode) {
        _formState.value = _formState.value.copy(authMode = authMode)
    }

    fun updatePassword(password: String) {
        _formState.value = _formState.value.copy(password = password)
    }

    fun clearPassword() {
        _formState.value = _formState.value.copy(
            password = "",
            hasStoredPassword = false
        )
    }

    fun updateSelectedKey(keyId: String?, keyName: String?) {
        _formState.value = _formState.value.copy(keyId = keyId, selectedKeyName = keyName)
    }

    fun updateJumpHost(jumpHostId: String?, jumpHostName: String?) {
        _formState.value = _formState.value.copy(jumpHostId = jumpHostId, jumpHostName = jumpHostName)
    }

    fun updateForwardAgent(forwardAgent: Boolean) {
        _formState.value = _formState.value.copy(forwardAgent = forwardAgent)
    }

    fun triggerSave() {
        _isSaving.value = true
    }

    fun saveServer() {
        val form = _formState.value
        val port = form.port.toIntOrNull() ?: 22

        viewModelScope.launch {
            // Determine the password to save:
            // - If new password entered, use it
            // - If hasStoredPassword and no new password, preserve existing
            // - If !hasStoredPassword and no new password, set to null (cleared)
            val existingServer = form.id?.let { serverRepository.getServer(it) }
            val passwordToSave = when {
                form.password.isNotEmpty() -> form.password
                form.hasStoredPassword -> existingServer?.passwordKeychainID
                else -> null
            }

            val server = Server(
                id = form.id ?: java.util.UUID.randomUUID().toString(),
                name = form.name.ifBlank { form.hostname },
                hostname = form.hostname,
                port = port,
                username = form.username,
                authMode = form.authMode,
                passwordKeychainID = passwordToSave,
                keyId = form.keyId,
                jumpHostId = form.jumpHostId,
                forwardAgent = form.forwardAgent
            )

            if (form.id != null) {
                serverRepository.updateServer(server)
            } else {
                serverRepository.addServer(server)
            }

            _isSaving.value = false
        }
    }
}
