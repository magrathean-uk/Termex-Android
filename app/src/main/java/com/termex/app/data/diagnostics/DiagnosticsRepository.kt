package com.termex.app.data.diagnostics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR
}

data class DiagnosticEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val title: String,
    val detail: String? = null,
    val serverId: String? = null,
    val severity: DiagnosticSeverity = DiagnosticSeverity.INFO
)

@Singleton
class DiagnosticsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val DIAGNOSTICS_DIR = "diagnostics"
        private const val EVENTS_FILE = "events.json"
    }

    private val diagnosticsDir = File(context.filesDir, DIAGNOSTICS_DIR).apply { mkdirs() }
    private val eventsFile = File(diagnosticsDir, EVENTS_FILE)
    private val mutex = Mutex()

    private val _events = MutableStateFlow(loadEventsFromDisk())
    val events: StateFlow<List<DiagnosticEvent>> = _events.asStateFlow()

    fun getEventsForServer(serverId: String?, limit: Int = 25) = events.map { allEvents ->
        if (serverId.isNullOrBlank()) {
            emptyList()
        } else {
            allEvents.filter { it.serverId == serverId }.take(limit)
        }
    }

    suspend fun record(
        category: String,
        title: String,
        detail: String? = null,
        serverId: String? = null,
        severity: DiagnosticSeverity = DiagnosticSeverity.INFO
    ) {
        val event = DiagnosticEvent(
            category = category.trim().ifBlank { "general" },
            title = DiagnosticsRedactor.redact(title)?.trim().orEmpty().ifBlank { "Event" },
            detail = DiagnosticsRedactor.redact(detail)?.trim()?.takeIf { it.isNotEmpty() }
                ?.take(DiagnosticsRetentionPolicy.MAX_DETAIL_LENGTH),
            serverId = serverId?.takeIf { it.isNotBlank() },
            severity = severity
        )

        mutateEvents { current ->
            listOf(event) + current.take(DiagnosticsRetentionPolicy.MAX_EVENTS - 1)
        }
    }

    suspend fun clear() {
        mutateEvents { emptyList() }
    }

    private suspend fun mutateEvents(transform: (List<DiagnosticEvent>) -> List<DiagnosticEvent>) {
        mutex.withLock {
            val updated = transform(_events.value).take(DiagnosticsRetentionPolicy.MAX_EVENTS)
            _events.value = updated
            withContext(Dispatchers.IO) {
                persist(updated)
            }
        }
    }

    private fun loadEventsFromDisk(): List<DiagnosticEvent> {
        return try {
            if (!eventsFile.exists()) return emptyList()
            if (eventsFile.length() > DiagnosticsRetentionPolicy.MAX_EVENTS_FILE_BYTES) return emptyList()
            val raw = eventsFile.readText()
            if (raw.isBlank()) return emptyList()
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(
                        DiagnosticEvent(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            category = obj.optString("category", "general"),
                            title = DiagnosticsRedactor.redact(obj.optString("title", "Event")) ?: "Event",
                            detail = DiagnosticsRedactor.redact(obj.optString("detail")).takeIf { !it.isNullOrBlank() },
                            serverId = obj.optString("serverId").takeIf { it.isNotBlank() },
                            severity = runCatching {
                                DiagnosticSeverity.valueOf(obj.optString("severity", DiagnosticSeverity.INFO.name))
                            }.getOrDefault(DiagnosticSeverity.INFO)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(events: List<DiagnosticEvent>) {
        val json = JSONArray().apply {
            events.forEach { event ->
                put(
                    JSONObject().apply {
                        put("id", event.id)
                        put("timestamp", event.timestamp)
                        put("category", event.category)
                        put("title", event.title)
                        put("detail", event.detail)
                        put("serverId", event.serverId)
                        put("severity", event.severity.name)
                    }
                )
            }
        }
        eventsFile.writeText(json.toString())
    }
}
