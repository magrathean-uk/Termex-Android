package com.termex.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.PersistedRootRoute
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.Workplace
import com.termex.app.domain.WorkplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkplaceFormState(
    val name: String = "",
    val isEditing: Boolean = false,
    val editingId: String? = null
)

@HiltViewModel
class WorkplacesViewModel @Inject constructor(
    private val workplaceRepository: WorkplaceRepository,
    private val serverRepository: ServerRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val workplaces: StateFlow<List<Workplace>> = workplaceRepository.getAllWorkplaces()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allServers: StateFlow<List<Server>> = serverRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _formState = MutableStateFlow(WorkplaceFormState())
    val formState: StateFlow<WorkplaceFormState> = _formState.asStateFlow()

    private val _showAddServerDialog = MutableStateFlow<String?>(null)
    val showAddServerDialog: StateFlow<String?> = _showAddServerDialog.asStateFlow()

    private val _expandedWorkplace = MutableStateFlow<String?>(null)
    val expandedWorkplace: StateFlow<String?> = _expandedWorkplace.asStateFlow()

    private val _workplaceToDelete = MutableStateFlow<Workplace?>(null)
    val workplaceToDelete: StateFlow<Workplace?> = _workplaceToDelete.asStateFlow()

    fun toggleExpanded(workplaceId: String) {
        _expandedWorkplace.value = if (_expandedWorkplace.value == workplaceId) null else workplaceId
    }

    fun showAddDialog() {
        _formState.value = WorkplaceFormState()
        _showDialog.value = true
    }

    fun showEditDialog(workplace: Workplace) {
        _formState.value = WorkplaceFormState(
            name = workplace.name,
            isEditing = true,
            editingId = workplace.id
        )
        _showDialog.value = true
    }

    fun dismissDialog() {
        _showDialog.value = false
        _formState.value = WorkplaceFormState()
    }

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun saveWorkplace() {
        val form = _formState.value
        if (form.name.isBlank()) return

        viewModelScope.launch {
            if (form.isEditing && form.editingId != null) {
                val workplace = Workplace(id = form.editingId, name = form.name)
                workplaceRepository.updateWorkplace(workplace)
            } else {
                val workplace = Workplace(name = form.name)
                workplaceRepository.addWorkplace(workplace)
            }
            dismissDialog()
        }
    }

    fun requestDeleteWorkplace(workplace: Workplace) {
        _workplaceToDelete.value = workplace
    }

    fun dismissDeleteWorkplaceDialog() {
        _workplaceToDelete.value = null
    }

    fun confirmDeleteWorkplace() {
        val workplace = _workplaceToDelete.value ?: return
        viewModelScope.launch {
            workplaceRepository.deleteWorkplace(workplace)
            if (_expandedWorkplace.value == workplace.id) {
                _expandedWorkplace.value = null
            }
            _workplaceToDelete.value = null
        }
    }

    fun showAddServerToWorkplace(workplaceId: String) {
        _showAddServerDialog.value = workplaceId
    }

    fun dismissAddServerDialog() {
        _showAddServerDialog.value = null
    }

    fun addServerToWorkplace(server: Server, workplaceId: String) {
        viewModelScope.launch {
            val updatedServer = server.copy(workplaceId = workplaceId)
            serverRepository.updateServer(updatedServer)
        }
    }

    fun rememberWorkplaceRoute(workplaceId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setPersistedRootRoute(PersistedRootRoute.workplace(workplaceId))
        }
    }

    fun removeServerFromWorkplace(server: Server) {
        viewModelScope.launch {
            val updatedServer = server.copy(workplaceId = null)
            serverRepository.updateServer(updatedServer)
        }
    }
}
