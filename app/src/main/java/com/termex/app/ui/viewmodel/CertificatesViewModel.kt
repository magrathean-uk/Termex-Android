package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.AuthMode
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.SSHCertificate
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
class CertificatesViewModel @Inject constructor(
    private val certificateRepository: CertificateRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    val certificates: StateFlow<List<SSHCertificate>> = certificateRepository.getAllCertificates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    fun showImportDialog() { _showImportDialog.value = true }
    fun hideImportDialog() { _showImportDialog.value = false }

    fun importCertificate(name: String, content: String) {
        viewModelScope.launch {
            certificateRepository.importCertificate(name.trim(), content.trim())
            hideImportDialog()
        }
    }

    fun deleteCertificate(certificate: SSHCertificate) {
        viewModelScope.launch {
            certificateRepository.deleteCertificate(certificate)
        }
    }

    fun repairServerWithSelectedCertificate(
        serverId: String,
        certificate: SSHCertificate,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId) ?: return@launch
            serverRepository.updateServer(
                server.copy(
                    authMode = AuthMode.KEY,
                    certificatePath = certificate.path
                )
            )
            onComplete()
        }
    }

    fun importCertificateForServer(
        serverId: String,
        name: String,
        content: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val sanitizedName = name.trim()
            certificateRepository.importCertificate(sanitizedName, content.trim())
            val server = serverRepository.getServer(serverId) ?: return@launch
            serverRepository.updateServer(
                server.copy(
                    authMode = AuthMode.KEY,
                    certificatePath = certificateRepository.getCertificatePath(sanitizedName)
                )
            )
            hideImportDialog()
            onComplete()
        }
    }
}
