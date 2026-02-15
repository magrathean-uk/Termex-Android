package com.termex.app.domain

/**
 * Represents a saved terminal session state
 */
data class SessionState(
    val id: String,
    val serverId: String,
    val terminalBuffer: String, // Last N lines of terminal output
    val workingDirectory: String? = null,
    val connectedAt: Long,
    val lastActiveAt: Long
)
