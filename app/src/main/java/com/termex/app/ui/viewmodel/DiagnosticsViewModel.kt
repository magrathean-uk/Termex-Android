package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.BuildConfig
import com.termex.app.core.ssh.ConnectionManager
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.data.diagnostics.DiagnosticEvent
import com.termex.app.data.diagnostics.DiagnosticsRepository
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.KnownHostRepository
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DiagnosticsSummary(
    val serverCount: Int = 0,
    val workplaceCount: Int = 0,
    val keyCount: Int = 0,
    val certificateCount: Int = 0,
    val knownHostCount: Int = 0,
    val savedSessionCount: Int = 0,
    val activeConnectionCount: Int = 0,
    val eventCount: Int = 0,
    val credentialIssueCount: Int = 0
)

private data class DiagnosticsReferenceCounts(
    val workplaceCount: Int,
    val keyCount: Int,
    val certificateCount: Int,
    val knownHostCount: Int,
    val savedSessionCount: Int
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    serverRepository: ServerRepository,
    workplaceRepository: WorkplaceRepository,
    keyRepository: KeyRepository,
    certificateRepository: CertificateRepository,
    knownHostRepository: KnownHostRepository,
    private val sessionRepository: com.termex.app.data.repository.SessionRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val passwordStore: SecurePasswordStore,
    connectionManager: ConnectionManager
) : ViewModel() {

    val events: StateFlow<List<DiagnosticEvent>> = diagnosticsRepository.events

    private val referenceCounts = combine(
        workplaceRepository.getAllWorkplaces(),
        keyRepository.getAllKeys(),
        certificateRepository.getAllCertificates(),
        knownHostRepository.getAllKnownHosts(),
        sessionRepository.getAllSessions()
    ) { workplaces, keys, certificates, knownHosts, sessions ->
        DiagnosticsReferenceCounts(
            workplaceCount = workplaces.size,
            keyCount = keys.size,
            certificateCount = certificates.size,
            knownHostCount = knownHosts.size,
            savedSessionCount = sessions.size
        )
    }

    val summary: StateFlow<DiagnosticsSummary> = combine(
        serverRepository.getAllServers(),
        referenceCounts,
        diagnosticsRepository.events,
        connectionManager.activeSessionKeys
    ) { servers, counts, events, activeConnections ->
        DiagnosticsSummary(
            serverCount = servers.size,
            workplaceCount = counts.workplaceCount,
            keyCount = counts.keyCount,
            certificateCount = counts.certificateCount,
            knownHostCount = counts.knownHostCount,
            savedSessionCount = counts.savedSessionCount,
            activeConnectionCount = activeConnections.size,
            eventCount = events.size,
            credentialIssueCount = countCredentialIssues(servers)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiagnosticsSummary())

    fun clearDiagnostics() {
        viewModelScope.launch {
            diagnosticsRepository.clear()
        }
    }

    fun clearSavedSessions() {
        viewModelScope.launch {
            sessionRepository.deleteAllSessions()
        }
    }

    fun buildExportText(): String {
        val currentSummary = summary.value
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val now = formatter.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()))
        val recentEvents = events.value.take(50)

        return buildString {
            appendLine("Termex Android Diagnostics")
            appendLine("Generated: $now")
            appendLine("Version: ${BuildConfig.VERSION_NAME}")
            appendLine()
            appendLine("Counts")
            appendLine("- Servers: ${currentSummary.serverCount}")
            appendLine("- Workplaces: ${currentSummary.workplaceCount}")
            appendLine("- Keys: ${currentSummary.keyCount}")
            appendLine("- Certificates: ${currentSummary.certificateCount}")
            appendLine("- Known Hosts: ${currentSummary.knownHostCount}")
            appendLine("- Saved Sessions: ${currentSummary.savedSessionCount}")
            appendLine("- Active Connections: ${currentSummary.activeConnectionCount}")
            appendLine("- Diagnostic Events: ${currentSummary.eventCount}")
            appendLine("- Credential Issues: ${currentSummary.credentialIssueCount}")
            appendLine()
            appendLine("Recent Events")
            if (recentEvents.isEmpty()) {
                appendLine("- none")
            } else {
                recentEvents.forEach { event ->
                    val timestamp = formatter.format(Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()))
                    val serverLabel = event.serverId?.let { " [server=$it]" }.orEmpty()
                    val detail = event.detail?.let { " :: $it" }.orEmpty()
                    appendLine("- $timestamp ${event.severity.name}/${event.category}$serverLabel :: ${event.title}$detail")
                }
            }
        }
    }

    private fun countCredentialIssues(servers: List<Server>): Int =
        countCredentialIssues(servers, passwordStore::getPassword)
}

internal fun countCredentialIssues(
    servers: List<Server>,
    passwordLookup: (String?) -> String?
): Int {
    if (servers.isEmpty()) return 0
    val serverIds = servers.map { it.id }.toSet()
    return servers.count { server ->
        val keyMissing = server.keyId?.let { path -> !File(path).exists() } ?: false
        val passwordMissing = server.passwordKeychainID?.let { passwordLookup(it) == null } ?: false
        val jumpHostMissing = server.jumpHostId?.let { it !in serverIds } ?: false
        keyMissing || passwordMissing || jumpHostMissing
    }
}
