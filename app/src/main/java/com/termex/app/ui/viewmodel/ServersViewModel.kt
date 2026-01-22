package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val demoModeEnabled: StateFlow<Boolean> = userPreferencesRepository.demoModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val servers: StateFlow<List<Server>> = combine(
        serverRepository.getAllServers(),
        userPreferencesRepository.demoModeEnabledFlow
    ) { servers, demoEnabled ->
        if (demoEnabled) {
            // Demo mode: ONLY show demo server, hide all real servers
            listOf(Server.createDemoServer())
        } else {
            servers
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            serverRepository.deleteServer(server)
        }
    }
}
