package com.termex.app.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.core.sync.GoogleDriveAuthorizationManager
import com.termex.app.core.sync.GoogleDriveAuthorizationResult
import com.termex.app.core.sync.MetadataSyncService
import com.termex.app.core.sync.MissingSecretIssue
import com.termex.app.core.sync.SyncAuthorizationState
import com.termex.app.data.prefs.SyncMode
import com.termex.app.data.prefs.SyncStatus
import com.termex.app.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val metadataSyncService: MetadataSyncService,
    private val googleDriveAuthorizationManager: GoogleDriveAuthorizationManager
) : ViewModel() {

    val googleSyncEnabled: Boolean = false

    val syncMode: StateFlow<SyncMode> = userPreferencesRepository.syncModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, SyncMode.LOCAL_ONLY)

    val syncStatus: StateFlow<SyncStatus> = userPreferencesRepository.syncStatusFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, SyncStatus())

    val googleAccountEmail: StateFlow<String?> = userPreferencesRepository.syncGoogleAccountEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _authState = MutableStateFlow<SyncAuthorizationState>(SyncAuthorizationState.Disconnected)
    val authState: StateFlow<SyncAuthorizationState> = _authState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _missingSecretIssues = MutableStateFlow<List<MissingSecretIssue>>(emptyList())
    val missingSecretIssues: StateFlow<List<MissingSecretIssue>> = _missingSecretIssues.asStateFlow()

    private val _authorizationRequests = MutableSharedFlow<IntentSenderRequest>(extraBufferCapacity = 1)
    val authorizationRequests = _authorizationRequests.asSharedFlow()

    private var pendingSyncAfterAuthorization = false

    init {
        viewModelScope.launch {
            userPreferencesRepository.syncGoogleAccountEmailFlow.collect { email ->
                _authState.value = if (googleSyncEnabled) {
                    email?.let(SyncAuthorizationState::Connected)
                        ?: SyncAuthorizationState.Disconnected
                } else {
                    SyncAuthorizationState.Disconnected
                }
            }
        }
        if (!googleSyncEnabled) {
            viewModelScope.launch {
                userPreferencesRepository.setSyncMode(SyncMode.LOCAL_ONLY)
                userPreferencesRepository.setSyncGoogleAccountEmail(null)
            }
        }
        refreshRepairState()
    }

    fun setSyncMode(mode: SyncMode) {
        viewModelScope.launch {
            val nextMode = if (googleSyncEnabled) mode else SyncMode.LOCAL_ONLY
            userPreferencesRepository.setSyncMode(nextMode)
            _errorMessage.value = null
        }
    }

    fun connectAccount(activity: Activity) {
        if (!googleSyncEnabled) {
            _errorMessage.value = "Cloud sync is hidden right now."
            return
        }
        viewModelScope.launch {
            _authState.value = SyncAuthorizationState.Authorizing
            _errorMessage.value = null
            val result = googleDriveAuthorizationManager.signIn(activity)
            result.onSuccess { identity ->
                userPreferencesRepository.setSyncMode(SyncMode.GOOGLE_DRIVE)
                userPreferencesRepository.setSyncGoogleAccountEmail(identity.email)
                _authState.value = SyncAuthorizationState.Connected(identity.email)
            }.onFailure { error ->
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Cloud sync is not included in this build."
                _errorMessage.value = message
                _authState.value = SyncAuthorizationState.Error(message)
            }
        }
    }

    fun syncNow(activity: Activity) {
        if (!googleSyncEnabled) {
            _errorMessage.value = "Cloud sync is hidden right now."
            return
        }
        val email = googleAccountEmail.value
        if (email.isNullOrBlank()) {
            val message = "Cloud sync is not included in this build."
            _errorMessage.value = message
            _authState.value = SyncAuthorizationState.Error(message)
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            val authorization = googleDriveAuthorizationManager.authorizeDriveAccess(activity, email)
            authorization.onSuccess { result ->
                when (result) {
                    is GoogleDriveAuthorizationResult.Authorized -> {
                        runSync(result.accessToken, email)
                    }
                    is GoogleDriveAuthorizationResult.NeedsResolution -> {
                        pendingSyncAfterAuthorization = true
                        _isSyncing.value = false
                        _authorizationRequests.tryEmit(result.request)
                    }
                }
            }.onFailure { error ->
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Google Drive authorization failed."
                applyError(message, email)
            }
        }
    }

    fun completeAuthorization(context: Context, data: Intent?) {
        if (!googleSyncEnabled) {
            return
        }
        val email = googleAccountEmail.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = googleDriveAuthorizationManager.handleAuthorizationResult(context, data)
            result.onSuccess { accessToken ->
                if (pendingSyncAfterAuthorization) {
                    pendingSyncAfterAuthorization = false
                    runSync(accessToken, email)
                } else {
                    _isSyncing.value = false
                }
            }.onFailure { error ->
                pendingSyncAfterAuthorization = false
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Google Drive authorization failed."
                applyError(message, email)
            }
        }
    }

    fun cancelAuthorization(message: String = "Google Drive access was cancelled.") {
        if (!googleSyncEnabled) {
            return
        }
        pendingSyncAfterAuthorization = false
        _isSyncing.value = false
        _errorMessage.value = message
        googleAccountEmail.value?.let {
            _authState.value = SyncAuthorizationState.Connected(it)
        }
    }

    fun disconnect(context: Context) {
        viewModelScope.launch {
            if (googleSyncEnabled) {
                googleDriveAuthorizationManager.disconnect(context)
            }
            userPreferencesRepository.setSyncGoogleAccountEmail(null)
            userPreferencesRepository.setSyncMode(SyncMode.LOCAL_ONLY)
            _errorMessage.value = null
            _authState.value = SyncAuthorizationState.Disconnected
        }
    }

    fun refreshRepairState() {
        viewModelScope.launch {
            _missingSecretIssues.value = metadataSyncService.findMissingSecretIssues()
        }
    }

    private suspend fun runSync(accessToken: String, email: String) {
        try {
            val result = metadataSyncService.syncWithGoogleDrive(
                accessToken = accessToken,
                googleAccountEmail = email
            )
            userPreferencesRepository.setSyncMode(SyncMode.GOOGLE_DRIVE)
            userPreferencesRepository.setSyncStatus(result)
            userPreferencesRepository.setSyncGoogleAccountEmail(email)
            _authState.value = SyncAuthorizationState.Connected(email)
            refreshRepairState()
        } catch (error: Exception) {
            val message = error.message?.takeIf { it.isNotBlank() } ?: "Sync failed."
            applyError(message, email)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun applyError(message: String, email: String?) {
        _isSyncing.value = false
        _errorMessage.value = message
        _authState.value = if (email.isNullOrBlank()) {
            SyncAuthorizationState.Error(message)
        } else {
            SyncAuthorizationState.Connected(email)
        }
        userPreferencesRepository.setSyncStatus(
            SyncStatus(
                message = message,
                googleAccountEmail = email
            )
        )
    }
}
