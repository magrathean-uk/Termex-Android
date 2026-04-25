package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.domain.Snippet
import com.termex.app.domain.SnippetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class SnippetEditorState(
    val id: String? = null,
    val name: String = "",
    val command: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@HiltViewModel
class SnippetsViewModel @Inject constructor(
    private val snippetRepository: SnippetRepository
) : ViewModel() {
    
    val snippets: StateFlow<List<Snippet>> = snippetRepository.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _editorState = MutableStateFlow<SnippetEditorState?>(null)
    val editorState: StateFlow<SnippetEditorState?> = _editorState.asStateFlow()
    
    fun showAddDialog() {
        _editorState.value = SnippetEditorState()
    }
    
    fun dismissAddDialog() {
        _editorState.value = null
    }
    
    fun editSnippet(snippet: Snippet) {
        _editorState.value = SnippetEditorState(
            id = snippet.id,
            name = snippet.name,
            command = snippet.command,
            createdAtMillis = snippet.createdAt.time
        )
    }
    
    fun updateEditorName(name: String) {
        _editorState.update { current ->
            current?.copy(name = name)
        }
    }

    fun updateEditorCommand(command: String) {
        _editorState.update { current ->
            current?.copy(command = command)
        }
    }

    fun addSnippet(name: String, command: String) {
        viewModelScope.launch {
            persistSnippet(
                Snippet(
                    name = name,
                    command = command
                )
            )
            _editorState.value = null
        }
    }

    fun saveEditorSnippet() {
        val editor = _editorState.value ?: return
        if (editor.command.isBlank()) return

        viewModelScope.launch {
            persistSnippet(
                Snippet(
                    id = editor.id ?: UUID.randomUUID().toString(),
                    name = editor.name,
                    command = editor.command,
                    createdAt = Date(editor.createdAtMillis)
                )
            )
            _editorState.value = null
        }
    }

    fun deleteSnippet(snippet: Snippet) {
        viewModelScope.launch {
            snippetRepository.deleteSnippet(snippet)
        }
    }

    fun duplicateSnippet(snippet: Snippet) {
        viewModelScope.launch {
            val duplicatedName = if (snippet.name.isBlank()) {
                ""
            } else {
                "${snippet.name} Copy"
            }
            snippetRepository.addSnippet(
                snippet.copy(
                    id = UUID.randomUUID().toString(),
                    name = duplicatedName,
                    createdAt = Date()
                )
            )
        }
    }

    private suspend fun persistSnippet(snippet: Snippet) {
        snippetRepository.addSnippet(snippet)
    }
}
