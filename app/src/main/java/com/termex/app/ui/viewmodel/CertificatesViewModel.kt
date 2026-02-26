package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.SSHCertificate
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
    private val certificateRepository: CertificateRepository
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
}
