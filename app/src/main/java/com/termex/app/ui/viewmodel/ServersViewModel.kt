package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.AuthMode
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
    val name: String = "",
    val hostname: String = "",
    val port: String = "22",
    val username: String = "",
    val authMode: AuthMode = AuthMode.PASSWORD,
    val isEditing: Boolean = false,
    val editingServerId: String? = null
)

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {
    
    val servers: StateFlow<List<Server>> = serverRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _formState = MutableStateFlow(ServerFormState())
    val formState: StateFlow<ServerFormState> = _formState.asStateFlow()
    
    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()
    
    private val _selectedServerForConnection = MutableStateFlow<Server?>(null)
    val selectedServerForConnection: StateFlow<Server?> = _selectedServerForConnection.asStateFlow()
    
    fun showAddDialog() {
        _formState.value = ServerFormState()
        _showAddEditDialog.value = true
    }
    
    fun showEditDialog(server: Server) {
        _formState.value = ServerFormState(
            name = server.name,
            hostname = server.hostname,
            port = server.port.toString(),
            username = server.username,
            authMode = server.authMode,
            isEditing = true,
            editingServerId = server.id
        )
        _showAddEditDialog.value = true
    }
    
    fun dismissDialog() {
        _showAddEditDialog.value = false
        _formState.value = ServerFormState()
    }
    
    fun updateFormName(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }
    
    fun updateFormHostname(hostname: String) {
        _formState.value = _formState.value.copy(hostname = hostname)
    }
    
    fun updateFormPort(port: String) {
        _formState.value = _formState.value.copy(port = port)
    }
    
    fun updateFormUsername(username: String) {
        _formState.value = _formState.value.copy(username = username)
    }
    
    fun updateFormAuthMode(authMode: AuthMode) {
        _formState.value = _formState.value.copy(authMode = authMode)
    }
    
    fun saveServer() {
        val form = _formState.value
        val port = form.port.toIntOrNull() ?: 22
        
        viewModelScope.launch {
            val server = Server(
                id = form.editingServerId ?: java.util.UUID.randomUUID().toString(),
                name = form.name.ifBlank { form.hostname },
                hostname = form.hostname,
                port = port,
                username = form.username,
                authMode = form.authMode
            )
            
            if (form.isEditing) {
                serverRepository.updateServer(server)
            } else {
                serverRepository.addServer(server)
            }
            
            dismissDialog()
        }
    }
    
    fun deleteServer(server: Server) {
        viewModelScope.launch {
            serverRepository.deleteServer(server)
        }
    }
    
    fun selectServerForConnection(server: Server) {
        _selectedServerForConnection.value = server
    }
    
    fun clearSelectedServer() {
        _selectedServerForConnection.value = null
    }
}
