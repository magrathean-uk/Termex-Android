package com.termex.app.domain

import kotlinx.coroutines.flow.Flow

interface SnippetRepository {
    fun getAllSnippets(): Flow<List<Snippet>>
    suspend fun addSnippet(snippet: Snippet)
    suspend fun deleteSnippet(snippet: Snippet)
}
