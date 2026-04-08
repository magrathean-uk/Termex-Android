package com.termex.app.core.ssh

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.termex.app.data.diagnostics.DiagnosticSeverity
import com.termex.app.data.diagnostics.DiagnosticsRepository
import com.termex.app.service.TermexConnectionService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Application-scoped manager that owns all SSH connections.
 * Connections survive ViewModel destruction (navigation, back press) and app backgrounding.
 * A foreground service is started whenever any session is active, preventing Android from
 * killing the process.
 */
@Singleton
class ConnectionManager @Inject constructor(
    private val sshClientProvider: Provider<SSHClient>,
    private val diagnosticsRepository: DiagnosticsRepository,
    @ApplicationContext private val context: Context
) {
    data class Session(
        val client: SSHClient,
        val buffer: TerminalBuffer,
        val state: MutableStateFlow<SSHConnectionState>,
        var readJob: Job? = null,
        var disconnectRequested: Boolean = false
    )

    private val sessions = ConcurrentHashMap<String, Session>()
    // Per-key mutexes prevent concurrent connect() calls for the same server
    // but allow different servers to connect in parallel (critical for multi-terminal)
    private val connectMutexes = ConcurrentHashMap<String, Mutex>()

    // Application-level scope — never cancelled while app process is alive
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _activeSessionKeys = MutableStateFlow<Set<String>>(emptySet())
    val activeSessionKeys: StateFlow<Set<String>> = _activeSessionKeys.asStateFlow()

    // --- Public API ---

    fun getState(key: String): StateFlow<SSHConnectionState>? = sessions[key]?.state?.asStateFlow()
    fun getBuffer(key: String): TerminalBuffer? = sessions[key]?.buffer
    fun isConnected(key: String) = sessions[key]?.state?.value is SSHConnectionState.Connected
    fun hasSession(key: String) = sessions.containsKey(key)
    fun activeCount() = sessions.size

    fun registerHostKeyCallback(key: String, callback: HostKeyVerificationCallback) {
        sessions[key]?.client?.setHostKeyVerificationCallback(callback)
    }

    fun clearHostKeyCallback(key: String) {
        sessions[key]?.client?.setHostKeyVerificationCallback(null)
    }

    suspend fun connect(
        key: String,
        config: SSHConnectionConfig,
        hostKeyCallback: HostKeyVerificationCallback? = null
    ): Result<Unit> {
        val mutex = connectMutexes.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            // If already have a live (connecting or connected) session, re-use it
            val existing = sessions[key]
            if (
                existing != null &&
                existing.state.value !is SSHConnectionState.Error &&
                existing.state.value !is SSHConnectionState.Disconnected
            ) {
                return@withLock Result.success(Unit)
            }
            // Remove any stale errored/disconnected session
            sessions.remove(key)
            publishActiveSessionKeys()

            diagnosticsRepository.record(
                category = "connection",
                title = "Connection started",
                detail = "${config.username}@${config.hostname}:${config.port}",
                serverId = key
            )

            val client = sshClientProvider.get()
            val stateFlow = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Connecting)
            val buffer = TerminalBuffer()
            val session = Session(client, buffer, stateFlow)
            sessions[key] = session
            publishActiveSessionKeys()

            // Register the host key callback now that the session/client exists
            if (hostKeyCallback != null) {
                client.setHostKeyVerificationCallback(hostKeyCallback)
            }

            // Mirror client connection state into our flow.
            // Only clean up AFTER a successful connection (wasEverConnected guard prevents
            // the fresh-client initial Disconnected value from immediately removing the session).
            scope.launch {
                var wasEverConnected = false
                client.connectionState.collect { clientState ->
                    stateFlow.value = clientState
                    when (clientState) {
                        is SSHConnectionState.Connected -> {
                            wasEverConnected = true
                            diagnosticsRepository.record(
                                category = "connection",
                                title = "Connection established",
                                detail = "${config.username}@${config.hostname}:${config.port}",
                                serverId = key
                            )
                        }

                        is SSHConnectionState.Error -> {
                            diagnosticsRepository.record(
                                category = "connection",
                                title = "Connection error",
                                detail = clientState.message,
                                serverId = key,
                                severity = DiagnosticSeverity.ERROR
                            )
                            cleanupSession(key)
                        }

                        is SSHConnectionState.Disconnected -> {
                            if (wasEverConnected) {
                                diagnosticsRepository.record(
                                    category = "connection",
                                    title = if (session.disconnectRequested) "Disconnected by user" else "Connection closed",
                                    detail = "${config.username}@${config.hostname}:${config.port}",
                                    serverId = key
                                )
                                cleanupSession(key)
                            }
                        }

                        else -> Unit
                    }
                }
            }

            val result = client.connect(config)
            if (result.isSuccess) {
                startService() // Only start service after confirmed connection
                startReading(key, session)
            } else {
                diagnosticsRepository.record(
                    category = "connection",
                    title = "Connection failed",
                    detail = result.exceptionOrNull()?.message ?: "Unknown connection failure",
                    serverId = key,
                    severity = DiagnosticSeverity.ERROR
                )
                cleanupSession(key)
            }
            updateServiceNotification()
            result
        }
    }

    fun sendData(key: String, data: String) {
        scope.launch { sessions[key]?.client?.sendData(data) }
    }

    fun resizeTerminal(key: String, cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        sessions[key]?.buffer?.resize(cols, rows)
        scope.launch { sessions[key]?.client?.resizeTerminal(cols, rows, widthPx, heightPx) }
    }

    fun trustHostKey(key: String, result: HostKeyVerificationResult) {
        scope.launch { sessions[key]?.client?.trustHostKey(result) }
    }

    fun disconnect(key: String) {
        val session = sessions[key] ?: return
        session.disconnectRequested = true
        sessions.remove(key)
        publishActiveSessionKeys()
        scope.launch(NonCancellable) {
            session.readJob?.cancel()
            session.client.disconnect()
            updateServiceNotification()
            stopServiceIfIdle()
        }
    }

    fun disconnectAll() {
        sessions.keys.toList().forEach { disconnect(it) }
    }

    // --- Private helpers ---

    private fun startReading(key: String, session: Session) {
        session.readJob?.cancel()
        session.readJob = scope.launch {
            val inputStream = session.client.inputStream ?: return@launch
            val reader = java.io.InputStreamReader(inputStream, Charsets.UTF_8)
            val buf = CharArray(4096)
            try {
                while (isActive && session.client.isConnected()) {
                    val n = reader.read(buf)
                    when {
                        n > 0 -> session.buffer.write(String(buf, 0, n))
                        n == -1 -> {
                            session.client.disconnect()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    session.state.value = SSHConnectionState.Error(
                        "Connection lost: ${e.message ?: "unknown"}"
                    )
                    cleanupSession(key)
                }
            }
        }
    }

    private fun cleanupSession(key: String) {
        sessions.remove(key)?.readJob?.cancel()
        publishActiveSessionKeys()
        connectMutexes.remove(key)
        scope.launch {
            updateServiceNotification()
            stopServiceIfIdle()
        }
    }

    private fun publishActiveSessionKeys() {
        _activeSessionKeys.value = sessions.keys.toSet()
    }

    private fun startService() {
        try {
            val intent = Intent(context, TermexConnectionService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
        }
    }

    private fun updateServiceNotification() {
        try {
            val intent = Intent(TermexConnectionService.ACTION_UPDATE_NOTIFICATION)
            intent.setPackage(context.packageName)
            intent.putExtra(TermexConnectionService.EXTRA_CONNECTION_COUNT, sessions.size)
            context.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }

    private fun stopServiceIfIdle() {
        if (sessions.isEmpty()) {
            try {
                context.stopService(Intent(context, TermexConnectionService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
