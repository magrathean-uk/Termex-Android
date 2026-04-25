package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.AuthMode
import com.termex.app.domain.SSHKey
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeysViewModel @Inject constructor(
    private val keyRepository: KeyRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {
    
    val keys: StateFlow<List<SSHKey>> = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _showGenerateDialog = MutableStateFlow(false)
    val showGenerateDialog: StateFlow<Boolean> = _showGenerateDialog.asStateFlow()
    
    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()
    
    fun showGenerateDialog() {
        _showGenerateDialog.value = true
    }
    
    fun hideGenerateDialog() {
        _showGenerateDialog.value = false
    }
    
    fun showImportDialog() {
        _showImportDialog.value = true
    }
    
    fun hideImportDialog() {
        _showImportDialog.value = false
    }
    
    fun generateKey(name: String) {
        viewModelScope.launch {
            keyRepository.generateKey(name)
            hideGenerateDialog()
        }
    }
    
    fun importKey(name: String, privateContent: String, publicContent: String?) {
        viewModelScope.launch {
            keyRepository.importKey(name, privateContent, publicContent)
            hideImportDialog()
        }
    }
    
    fun deleteKey(key: SSHKey) {
        viewModelScope.launch {
            keyRepository.deleteKey(key)
        }
    }

    fun repairServerWithSelectedKey(serverId: String, key: SSHKey, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId) ?: return@launch
            serverRepository.updateServer(
                server.copy(
                    authMode = AuthMode.KEY,
                    keyId = key.path
                )
            )
            onComplete()
        }
    }

    fun importKeyForServer(
        serverId: String,
        name: String,
        privateContent: String,
        publicContent: String?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            keyRepository.importKey(name, privateContent, publicContent)
            val server = serverRepository.getServer(serverId) ?: return@launch
            serverRepository.updateServer(
                server.copy(
                    authMode = AuthMode.KEY,
                    keyId = keyRepository.getKeyPath(name)
                )
            )
            hideImportDialog()
            onComplete()
        }
    }
}
