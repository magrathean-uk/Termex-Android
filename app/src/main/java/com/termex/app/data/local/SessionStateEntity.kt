package com.termex.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_states")
data class SessionStateEntity(
    @PrimaryKey
    val id: String,
    val serverId: String,
    val terminalBuffer: String,
    val workingDirectory: String?,
    val connectedAt: Long,
    val lastActiveAt: Long
)
