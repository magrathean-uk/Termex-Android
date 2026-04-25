package com.termex.app.data.repository

import com.termex.app.data.local.SnippetDao
import com.termex.app.data.local.toDomain
import com.termex.app.data.local.toEntity
import com.termex.app.domain.Snippet
import com.termex.app.domain.SnippetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnippetRepositoryImpl @Inject constructor(
    private val snippetDao: SnippetDao
) : SnippetRepository {
    
    override fun getAllSnippets(): Flow<List<Snippet>> {
        return snippetDao.getAllSnippets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addSnippet(snippet: Snippet) {
        snippetDao.insertSnippet(snippet.toEntity())
    }

    override suspend fun deleteSnippet(snippet: Snippet) {
        snippetDao.deleteSnippet(snippet.toEntity())
    }
}
