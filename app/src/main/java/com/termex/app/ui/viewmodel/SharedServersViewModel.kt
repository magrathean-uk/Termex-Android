package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.discovery.SharedServerDiscoveryItem
import com.termex.app.discovery.SharedServerImportTarget
import com.termex.app.discovery.SharedServersDiscoveryService
import com.termex.app.discovery.SharedServersDiscoveryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SharedServersEvent {
    data class ImportServer(val target: SharedServerImportTarget) : SharedServersEvent
}

@HiltViewModel
class SharedServersViewModel @Inject constructor(
    private val discoveryService: SharedServersDiscoveryService
) : ViewModel() {

    val state = discoveryService.state

    private val _events = MutableSharedFlow<SharedServersEvent>()
    val events: SharedFlow<SharedServersEvent> = _events.asSharedFlow()

    init {
        discoveryService.startDiscovery()
    }

    fun refresh() {
        discoveryService.stopDiscovery()
        discoveryService.startDiscovery()
    }

    fun onServiceSelected(service: SharedServerDiscoveryItem) {
        viewModelScope.launch {
            discoveryService.resolve(service.id)?.let { target ->
                _events.emit(SharedServersEvent.ImportServer(target))
            }
        }
    }

    override fun onCleared() {
        discoveryService.stopDiscovery()
        super.onCleared()
    }
}

