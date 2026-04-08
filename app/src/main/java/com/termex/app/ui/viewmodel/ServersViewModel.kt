package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.ssh.ConnectionManager
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecentSessionSummary(
    val sessionId: String,
    val server: Server,
    val preview: String,
    val lastActiveAt: Long
)

data class WorkplaceSummary(
    val id: String,
    val name: String,
    val serverCount: Int
)

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val workplaceRepository: WorkplaceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sessionRepository: com.termex.app.data.repository.SessionRepository,
    private val connectionManager: ConnectionManager,
    private val passwordStore: SecurePasswordStore
) : ViewModel() {

    val demoModeEnabled: StateFlow<Boolean> = userPreferencesRepository.demoModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val servers: StateFlow<List<Server>> = combine(
        serverRepository.getAllServers(),
        userPreferencesRepository.demoModeEnabledFlow
    ) { servers, demoEnabled ->
        if (demoEnabled) {
            listOf(Server.createDemoServer())
        } else {
            servers.sortedBy { it.displayName.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeServers: StateFlow<List<Server>> = combine(
        connectionManager.activeSessionKeys,
        servers
    ) { activeKeys, servers ->
        servers.filter { it.id in activeKeys }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentSessions: StateFlow<List<RecentSessionSummary>> = combine(
        sessionRepository.getAllSessions(),
        serverRepository.getAllServers(),
        userPreferencesRepository.demoModeEnabledFlow
    ) { sessions, servers, demoEnabled ->
        if (demoEnabled) {
            emptyList()
        } else {
            val serverMap = servers.associateBy { it.id }
            sessions
                .sortedByDescending { it.lastActiveAt }
                .mapNotNull { session ->
                    val server = serverMap[session.serverId] ?: return@mapNotNull null
                    RecentSessionSummary(
                        sessionId = session.id,
                        server = server,
                        preview = session.terminalBuffer
                            .lineSequence()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .lastOrNull()
                            ?.take(120)
                            ?: server.displayName,
                        lastActiveAt = session.lastActiveAt
                    )
                }
                .distinctBy { it.server.id }
                .take(6)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val workplaceSummaries: StateFlow<List<WorkplaceSummary>> = combine(
        workplaceRepository.getAllWorkplaces(),
        serverRepository.getAllServers(),
        userPreferencesRepository.demoModeEnabledFlow
    ) { workplaces, servers, demoEnabled ->
        if (demoEnabled) {
            emptyList()
        } else {
            workplaces
                .map { workplace ->
                    WorkplaceSummary(
                        id = workplace.id,
                        name = workplace.name,
                        serverCount = servers.count { it.workplaceId == workplace.id }
                    )
                }
                .filter { it.serverCount > 0 }
                .sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val serverIssues: StateFlow<Map<String, String>> = combine(
        serverRepository.getAllServers(),
        userPreferencesRepository.demoModeEnabledFlow
    ) { servers, demoEnabled ->
        if (demoEnabled) {
            emptyMap()
        } else {
            val serverIds = servers.map { it.id }.toSet()
            servers.mapNotNull { server ->
                issueFor(server, serverIds)?.let { server.id to it }
            }.toMap()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            serverRepository.deleteServer(server)
        }
    }

    private fun issueFor(server: Server, serverIds: Set<String>): String? {
        if (server.isDemo) return null
        if (server.keyId?.let { !File(it).exists() } == true) {
            return "Configured key file is missing"
        }
        if (server.passwordKeychainID?.let { passwordStore.getPassword(it) == null } == true) {
            return "Saved password is unavailable"
        }
        if (server.jumpHostId?.let { it !in serverIds } == true) {
            return "Jump host reference is missing"
        }
        return null
    }
}
