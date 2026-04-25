package com.termex.app.domain

import java.util.UUID
import java.util.Date

data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val command: String,
    val createdAt: Date = Date()
)
