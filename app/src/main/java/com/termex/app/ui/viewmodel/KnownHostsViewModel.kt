package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KnownHostsViewModel @Inject constructor(
    private val knownHostRepository: KnownHostRepository
) : ViewModel() {

    val knownHosts: StateFlow<List<KnownHost>> = knownHostRepository.getAllKnownHosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteKnownHost(host: KnownHost) {
        viewModelScope.launch {
            knownHostRepository.deleteKnownHost(host)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            knownHostRepository.deleteAll()
        }
    }
}
