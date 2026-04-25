package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.PersistedRootRoute
import com.termex.app.core.RootRouteRestoreState
import com.termex.app.core.resolveRootRoute
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.Snippet
import com.termex.app.domain.SnippetRepository
import com.termex.app.domain.Workplace
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val serverRepository: ServerRepository,
    private val workplaceRepository: WorkplaceRepository,
    private val snippetRepository: SnippetRepository
) : ViewModel() {

    val hasCompletedOnboarding: StateFlow<Boolean> = userPreferencesRepository.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val demoModeEnabled: StateFlow<Boolean> = userPreferencesRepository.demoModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val startupRouteState: StateFlow<RootRouteRestoreState> = combine(
        userPreferencesRepository.persistedRootRouteFlow,
        userPreferencesRepository.persistentSessionResumeServerIdFlow,
        serverRepository.getAllServers(),
        workplaceRepository.getAllWorkplaces()
    ) { persistedRoute, resumeServerId, servers, workplaces ->
        val eligibleResumeServerIds = servers
            .filter { it.persistentSessionEnabled }
            .map { it.id }
            .toSet()
        resolveRootRoute(
            persistedRoute = persistedRoute,
            validServerIds = servers.map { it.id }.toSet(),
            validWorkplaceIds = workplaces.map { it.id }.toSet(),
            eligibleResumeServerIds = eligibleResumeServerIds,
            resumeServerId = resumeServerId
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootRouteRestoreState())
    
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.completeOnboarding()
        }
    }

    fun enableDemoMode() {
        viewModelScope.launch {
            userPreferencesRepository.setDemoModeEnabled(true)
            seedDemoDataIfNeeded()
        }
    }

    /** Seeds a demo server, workplace, and snippet the first time demo mode is activated. */
    private suspend fun seedDemoDataIfNeeded() {
        val existing = serverRepository.getServer(Server.DEMO_SERVER_ID)
        if (existing != null) return

        val demoServer = Server.createDemoServer()
        serverRepository.addServer(demoServer)

        val demoWorkplace = Workplace(name = "Demo Workspace")
        workplaceRepository.addWorkplace(demoWorkplace)

        val demoSnippet = Snippet(name = "Status", command = "status")
        snippetRepository.addSnippet(demoSnippet)
    }

    fun saveRootRoute(route: PersistedRootRoute) {
        viewModelScope.launch {
            userPreferencesRepository.setPersistedRootRoute(route)
        }
    }
}
