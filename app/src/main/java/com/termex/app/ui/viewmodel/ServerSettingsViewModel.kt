package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.CertificateRepository
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.domain.AuthMode
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.PortForward
import com.termex.app.domain.SSHCertificate
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
import java.io.File
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
    val certificatePath: String? = null,
    val selectedCertificateName: String? = null,
    val jumpHostId: String? = null,
    val jumpHostName: String? = null,
    val forwardAgent: Boolean = false,
    val identitiesOnly: Boolean = false,
    val persistentSessionEnabled: Boolean = false,
    val startupCommand: String = "",
    val portForwards: List<PortForward> = emptyList()
)

@HiltViewModel
class ServerSettingsViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val keyRepository: KeyRepository,
    private val certificateRepository: CertificateRepository,
    private val passwordStore: SecurePasswordStore
) : ViewModel() {

    private val _formState = MutableStateFlow(ServerFormState())
    val formState: StateFlow<ServerFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val keys: StateFlow<List<SSHKey>> = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val certificates: StateFlow<List<SSHCertificate>> = certificateRepository.getAllCertificates()
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
            val certificate = server.certificatePath?.let { certificatePath ->
                certificates.value.find { it.path == certificatePath }
            }
            val resolvedPassword = passwordStore.resolvePassword(server.id, server.passwordKeychainID)
            if (resolvedPassword.keyId != null && resolvedPassword.keyId != server.passwordKeychainID) {
                serverRepository.updateServer(server.copy(passwordKeychainID = resolvedPassword.keyId))
            }

            _formState.value = ServerFormState(
                id = server.id,
                name = server.name,
                hostname = server.hostname,
                port = server.port.toString(),
                username = server.username,
                authMode = server.authMode,
                password = "", // Don't load password for security
                hasStoredPassword = resolvedPassword.password != null || !server.passwordKeychainID.isNullOrEmpty(),
                keyId = server.keyId,
                selectedKeyName = key?.name ?: server.keyId?.let { File(it).name },
                certificatePath = server.certificatePath,
                selectedCertificateName = certificate?.name ?: server.certificatePath?.let { File(it).name },
                jumpHostId = server.jumpHostId,
                jumpHostName = jumpHost?.displayName,
                forwardAgent = server.forwardAgent,
                identitiesOnly = server.identitiesOnly,
                persistentSessionEnabled = server.persistentSessionEnabled,
                startupCommand = server.startupCommand.orEmpty(),
                portForwards = server.portForwards
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

    fun updateSelectedCertificate(certificatePath: String?, certificateName: String?) {
        _formState.value = _formState.value.copy(
            certificatePath = certificatePath,
            selectedCertificateName = certificateName
        )
    }

    fun updateJumpHost(jumpHostId: String?, jumpHostName: String?) {
        _formState.value = _formState.value.copy(jumpHostId = jumpHostId, jumpHostName = jumpHostName)
    }

    fun updateForwardAgent(forwardAgent: Boolean) {
        _formState.value = _formState.value.copy(forwardAgent = forwardAgent)
    }

    fun updateIdentitiesOnly(identitiesOnly: Boolean) {
        _formState.value = _formState.value.copy(identitiesOnly = identitiesOnly)
    }

    fun updatePersistentSessionEnabled(enabled: Boolean) {
        _formState.value = _formState.value.copy(persistentSessionEnabled = enabled)
    }

    fun updateStartupCommand(startupCommand: String) {
        _formState.value = _formState.value.copy(startupCommand = startupCommand)
    }

    fun updatePortForwards(portForwards: List<PortForward>) {
        _formState.value = _formState.value.copy(portForwards = portForwards)
    }

    fun triggerSave() {
        _isSaving.value = true
    }

    fun saveServer() {
        val form = _formState.value
        val port = form.port.toIntOrNull() ?: 22

        viewModelScope.launch {
            val serverId = form.id ?: java.util.UUID.randomUUID().toString()
            // Determine the password to save:
            // - If new password entered, use it
            // - If hasStoredPassword and no new password, preserve existing
            // - If !hasStoredPassword and no new password, set to null (cleared)
            val existingServer = form.id?.let { serverRepository.getServer(it) }
            val passwordToSave = when {
                form.password.isNotEmpty() -> passwordStore.savePasswordForServer(serverId, form.password)
                form.hasStoredPassword -> existingServer?.passwordKeychainID
                else -> null
            }

            val server = Server(
                id = serverId,
                name = form.name.ifBlank { form.hostname },
                hostname = form.hostname,
                port = port,
                username = form.username,
                authMode = form.authMode,
                passwordKeychainID = passwordToSave,
                keyId = form.keyId,
                certificatePath = form.certificatePath,
                jumpHostId = form.jumpHostId,
                forwardAgent = form.forwardAgent,
                identitiesOnly = form.identitiesOnly,
                persistentSessionEnabled = form.persistentSessionEnabled,
                startupCommand = form.startupCommand.trim().ifBlank { null },
                portForwards = form.portForwards
            )

            if (form.id != null) {
                serverRepository.updateServer(server)
            } else {
                serverRepository.addServer(server)
            }

            if (form.password.isEmpty() && !form.hasStoredPassword) {
                passwordStore.deletePassword(existingServer?.passwordKeychainID)
            }

            _isSaving.value = false
        }
    }
}
