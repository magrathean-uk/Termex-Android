package com.termex.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.termex.app.domain.Snippet
import java.util.Date

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val command: String,
    val createdAt: Long
)

fun SnippetEntity.toDomain() = Snippet(id = id, name = name, command = command, createdAt = Date(createdAt))
fun Snippet.toEntity() = SnippetEntity(id = id, name = name, command = command, createdAt = createdAt.time)
