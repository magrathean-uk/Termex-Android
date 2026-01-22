package com.termex.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.ssh.PortForwardManager
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortForwardFormState(
    val type: PortForwardType = PortForwardType.LOCAL,
    val localPort: String = "",
    val remoteHost: String = "localhost",
    val remotePort: String = "",
    val isEditing: Boolean = false,
    val editingId: String? = null
)

@HiltViewModel
class PortForwardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val portForwardManager: PortForwardManager
) : ViewModel() {

    private val serverId: String = savedStateHandle.get<String>("serverId") ?: ""

    private val _server = MutableStateFlow<Server?>(null)
    val server: StateFlow<Server?> = _server.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _formState = MutableStateFlow(PortForwardFormState())
    val formState: StateFlow<PortForwardFormState> = _formState.asStateFlow()

    val activeForwards = portForwardManager.activeForwards

    init {
        loadServer()
    }

    private fun loadServer() {
        viewModelScope.launch {
            val s = serverRepository.getServer(serverId)
            _server.value = s
            s?.let { portForwardManager.initializeForwards(it.portForwards) }
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
                        remotePort = remotePort
                    )
                }
            } else {
                currentForwards.add(
                    PortForward(
                        type = form.type,
                        localPort = localPort,
                        remoteHost = form.remoteHost,
                        remotePort = remotePort
                    )
                )
            }

            val updatedServer = currentServer.copy(portForwards = currentForwards)
            serverRepository.updateServer(updatedServer)
            _server.value = updatedServer
            portForwardManager.initializeForwards(currentForwards)
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
            portForwardManager.stopForward(portForward.id)
            portForwardManager.initializeForwards(updatedForwards)
        }
    }

    fun togglePortForward(portForward: PortForward) {
        val activeForward = activeForwards.value.find { it.config.id == portForward.id }
        if (activeForward?.isActive == true) {
            portForwardManager.stopForward(portForward.id)
        } else {
            portForwardManager.startForward(portForward)
        }
    }
}
