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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnippetsViewModel @Inject constructor(
    private val snippetRepository: SnippetRepository
) : ViewModel() {
    
    val snippets: StateFlow<List<Snippet>> = snippetRepository.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()
    
    fun showAddDialog() {
        _showAddDialog.value = true
    }
    
    fun dismissAddDialog() {
        _showAddDialog.value = false
    }
    
    fun addSnippet(name: String, command: String) {
        viewModelScope.launch {
            val snippet = Snippet(
                name = name,
                command = command
            )
            snippetRepository.addSnippet(snippet)
            _showAddDialog.value = false
        }
    }
    
    fun deleteSnippet(snippet: Snippet) {
        viewModelScope.launch {
            snippetRepository.deleteSnippet(snippet)
        }
    }
}
