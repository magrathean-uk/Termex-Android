package com.termex.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.domain.AuthMode
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.PortForward
import com.termex.app.domain.SSHCertificate
import com.termex.app.domain.SSHKey
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME,
    DESTINATION,
    AUTHENTICATION,
    JUMP_HOST,
    FORWARDING,
    REVIEW
}

enum class JumpHostMode {
    DIRECT,
    EXISTING,
    CUSTOM
}

enum class IdentityImportTarget {
    DESTINATION_KEY,
    DESTINATION_CERTIFICATE,
    JUMP_KEY,
    JUMP_CERTIFICATE
}

data class OnboardingAuthDraft(
    val authMode: AuthMode = AuthMode.PASSWORD,
    val password: String = "",
    val keyPath: String? = null,
    val keyName: String? = null,
    val certificatePath: String? = null,
    val certificateName: String? = null,
    val identitiesOnly: Boolean = false
)

data class OnboardingJumpDraft(
    val reusePrimaryAuth: Boolean = true,
    val name: String = "",
    val host: String = "",
    val port: String = "22",
    val username: String = "",
    val auth: OnboardingAuthDraft = OnboardingAuthDraft()
)

data class OnboardingDraftState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val serverName: String = "",
    val host: String = "",
    val port: String = "22",
    val username: String = "",
    val auth: OnboardingAuthDraft = OnboardingAuthDraft(),
    val jumpHostMode: JumpHostMode = JumpHostMode.DIRECT,
    val existingJumpHostId: String? = null,
    val jump: OnboardingJumpDraft = OnboardingJumpDraft(),
    val portForwards: List<PortForward> = emptyList(),
    val persistentSessionEnabled: Boolean = false,
    val startupCommand: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completionReady: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
    private val keyRepository: KeyRepository,
    private val certificateRepository: CertificateRepository,
    private val passwordStore: SecurePasswordStore
) : ViewModel() {

    val keys: StateFlow<List<SSHKey>> = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val certificates: StateFlow<List<SSHCertificate>> = certificateRepository.getAllCertificates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val servers: StateFlow<List<Server>> = serverRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _draft = MutableStateFlow(OnboardingDraftState())
    val draft: StateFlow<OnboardingDraftState> = _draft.asStateFlow()

    fun setStep(step: OnboardingStep) {
        _draft.value = _draft.value.copy(step = step, errorMessage = null)
    }

    fun back() {
        val current = _draft.value.step
        if (current == OnboardingStep.WELCOME) return
        setStep(OnboardingStep.entries[current.ordinal - 1])
    }

    fun advance() {
        val current = _draft.value.step
        if (!canAdvance(current)) return
        if (current == OnboardingStep.REVIEW) return
        setStep(OnboardingStep.entries[current.ordinal + 1])
    }

    fun skipToReview() {
        setStep(OnboardingStep.REVIEW)
    }

    fun updateServerName(value: String) {
        _draft.value = _draft.value.copy(serverName = value)
    }

    fun updateHost(value: String) {
        _draft.value = _draft.value.copy(host = value)
    }

    fun updatePort(value: String) {
        _draft.value = _draft.value.copy(port = value)
    }

    fun updateUsername(value: String) {
        _draft.value = _draft.value.copy(username = value)
    }

    fun updateAuthMode(value: AuthMode) {
        _draft.value = _draft.value.copy(auth = _draft.value.auth.copy(authMode = value))
    }

    fun updatePassword(value: String) {
        _draft.value = _draft.value.copy(auth = _draft.value.auth.copy(password = value))
    }

    fun updateIdentitiesOnly(value: Boolean) {
        _draft.value = _draft.value.copy(auth = _draft.value.auth.copy(identitiesOnly = value))
    }

    fun selectKey(target: IdentityImportTarget, keyPath: String?) {
        val key = keys.value.firstOrNull { it.path == keyPath }
        updateAuthTarget(
            target = target,
            transform = {
                it.copy(
                    keyPath = keyPath,
                    keyName = key?.name
                )
            }
        )
    }

    fun selectCertificate(target: IdentityImportTarget, certificatePath: String?) {
        val certificate = certificates.value.firstOrNull { it.path == certificatePath }
        updateAuthTarget(
            target = target,
            transform = {
                it.copy(
                    certificatePath = certificatePath,
                    certificateName = certificate?.name
                )
            }
        )
    }

    fun updateJumpHostMode(mode: JumpHostMode) {
        _draft.value = _draft.value.copy(jumpHostMode = mode, errorMessage = null)
    }

    fun updateExistingJumpHost(serverId: String?) {
        _draft.value = _draft.value.copy(existingJumpHostId = serverId)
    }

    fun updateJumpReusePrimaryAuth(value: Boolean) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(reusePrimaryAuth = value))
    }

    fun updateJumpName(value: String) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(name = value))
    }

    fun updateJumpHost(value: String) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(host = value))
    }

    fun updateJumpPort(value: String) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(port = value))
    }

    fun updateJumpUsername(value: String) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(username = value))
    }

    fun updateJumpAuthMode(value: AuthMode) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(auth = _draft.value.jump.auth.copy(authMode = value)))
    }

    fun updateJumpPassword(value: String) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(auth = _draft.value.jump.auth.copy(password = value)))
    }

    fun updateJumpIdentitiesOnly(value: Boolean) {
        _draft.value = _draft.value.copy(jump = _draft.value.jump.copy(auth = _draft.value.jump.auth.copy(identitiesOnly = value)))
    }

    fun updatePersistentSessionEnabled(value: Boolean) {
        _draft.value = _draft.value.copy(persistentSessionEnabled = value)
    }

    fun updateStartupCommand(value: String) {
        _draft.value = _draft.value.copy(startupCommand = value)
    }

    fun addPortForward(portForward: PortForward) {
        _draft.value = _draft.value.copy(portForwards = _draft.value.portForwards + portForward)
    }

    fun removePortForward(id: String) {
        _draft.value = _draft.value.copy(
            portForwards = _draft.value.portForwards.filterNot { it.id == id }
        )
    }

    fun importKey(target: IdentityImportTarget, name: String, privateKey: String, publicKey: String? = null) {
        viewModelScope.launch {
            val sanitizedName = sanitizeFileName(name)
            keyRepository.importKey(sanitizedName, privateKey.trim(), publicKey?.trim()?.ifBlank { null })
            updateAuthTarget(
                target = target,
                transform = {
                    it.copy(
                        authMode = AuthMode.KEY,
                        keyPath = keyRepository.getKeyPath(sanitizedName),
                        keyName = sanitizedName
                    )
                }
            )
        }
    }

    fun importCertificate(target: IdentityImportTarget, name: String, content: String) {
        viewModelScope.launch {
            val sanitizedName = sanitizeFileName(name)
            certificateRepository.importCertificate(sanitizedName, content.trim())
            updateAuthTarget(
                target = target,
                transform = {
                    it.copy(
                        certificatePath = certificatePathFor(sanitizedName),
                        certificateName = sanitizedName
                    )
                }
            )
        }
    }

    fun finish(demoModeActivated: Boolean) {
        val state = _draft.value
        if (demoModeActivated || !state.hasServerDraft()) {
            _draft.value = state.copy(completionReady = true, errorMessage = null)
            return
        }

        viewModelScope.launch {
            runCatching {
                _draft.value = _draft.value.copy(isSaving = true, errorMessage = null)
                saveDraft(_draft.value)
            }.onSuccess {
                _draft.value = _draft.value.copy(isSaving = false, completionReady = true)
            }.onFailure { error ->
                _draft.value = _draft.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "Unable to save server"
                )
            }
        }
    }

    fun consumeCompletion() {
        _draft.value = _draft.value.copy(completionReady = false)
    }

    fun canAdvance(step: OnboardingStep = _draft.value.step): Boolean {
        val state = _draft.value
        return when (step) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.DESTINATION -> state.hasValidDestination()
            OnboardingStep.AUTHENTICATION -> state.hasValidPrimaryAuth()
            OnboardingStep.JUMP_HOST -> state.hasValidJumpConfig()
            OnboardingStep.FORWARDING -> true
            OnboardingStep.REVIEW -> true
        }
    }

    private suspend fun saveDraft(state: OnboardingDraftState) {
        require(state.hasValidDestination()) { "Host, port, and username are required" }
        require(state.hasValidPrimaryAuth()) { "Authentication is incomplete" }
        val jumpHostId = when (state.jumpHostMode) {
            JumpHostMode.DIRECT -> null
            JumpHostMode.EXISTING -> requireNotNull(state.existingJumpHostId) { "Choose a jump host" }
            JumpHostMode.CUSTOM -> saveCustomJumpHost(state)
        }

        val serverId = UUID.randomUUID().toString()
        val passwordKeychainId = persistPassword(serverId, state.auth)
        serverRepository.addServer(
            Server(
                id = serverId,
                name = state.serverName.ifBlank { state.host },
                hostname = state.host.trim(),
                port = state.port.toInt(),
                username = state.username.trim(),
                authMode = state.auth.authMode,
                passwordKeychainID = passwordKeychainId,
                keyId = state.auth.keyPath,
                certificatePath = state.auth.certificatePath,
                jumpHostId = jumpHostId,
                identitiesOnly = state.auth.identitiesOnly,
                persistentSessionEnabled = state.persistentSessionEnabled,
                startupCommand = state.startupCommand.trim().ifBlank { null },
                portForwards = state.portForwards
            )
        )
    }

    private suspend fun saveCustomJumpHost(state: OnboardingDraftState): String {
        require(state.hasValidJumpConfig()) { "Jump host details are incomplete" }
        val jumpAuth = state.resolveJumpAuth()
        val jumpId = UUID.randomUUID().toString()
        val passwordKeychainId = persistPassword(jumpId, jumpAuth)
        serverRepository.addServer(
            Server(
                id = jumpId,
                name = state.jump.name.ifBlank { state.jump.host },
                hostname = state.jump.host.trim(),
                port = state.jump.port.toInt(),
                username = state.jump.username.trim(),
                authMode = jumpAuth.authMode,
                passwordKeychainID = passwordKeychainId,
                keyId = jumpAuth.keyPath,
                certificatePath = jumpAuth.certificatePath,
                identitiesOnly = jumpAuth.identitiesOnly
            )
        )
        return jumpId
    }

    private fun persistPassword(serverId: String, auth: OnboardingAuthDraft): String? {
        val password = auth.password.trim()
        return if (auth.authMode == AuthMode.PASSWORD && password.isNotEmpty()) {
            passwordStore.savePasswordForServer(serverId, password)
        } else {
            null
        }
    }

    private fun updateAuthTarget(
        target: IdentityImportTarget,
        transform: (OnboardingAuthDraft) -> OnboardingAuthDraft
    ) {
        _draft.value = when (target) {
            IdentityImportTarget.DESTINATION_KEY,
            IdentityImportTarget.DESTINATION_CERTIFICATE -> {
                _draft.value.copy(auth = transform(_draft.value.auth))
            }
            IdentityImportTarget.JUMP_KEY,
            IdentityImportTarget.JUMP_CERTIFICATE -> {
                _draft.value.copy(jump = _draft.value.jump.copy(auth = transform(_draft.value.jump.auth)))
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        return File(name.trim()).name.replace("..", "")
    }

    private fun certificatePathFor(name: String): String {
        return File(File(context.filesDir, "ssh_certs"), name).absolutePath
    }
}

internal fun OnboardingDraftState.hasServerDraft(): Boolean {
    return host.isNotBlank() && username.isNotBlank()
}

internal fun OnboardingDraftState.hasValidDestination(): Boolean {
    return host.isNotBlank() && username.isNotBlank() && port.toIntOrNull() != null
}

internal fun OnboardingDraftState.hasValidPrimaryAuth(): Boolean {
    return auth.isValid()
}

internal fun OnboardingDraftState.hasValidJumpConfig(): Boolean {
    return when (jumpHostMode) {
        JumpHostMode.DIRECT -> true
        JumpHostMode.EXISTING -> existingJumpHostId != null
        JumpHostMode.CUSTOM -> {
            jump.host.isNotBlank() &&
                jump.username.isNotBlank() &&
                jump.port.toIntOrNull() != null &&
                resolveJumpAuth().isValid()
        }
    }
}

internal fun OnboardingDraftState.resolveJumpAuth(): OnboardingAuthDraft {
    return if (jump.reusePrimaryAuth) auth else jump.auth
}

private fun OnboardingAuthDraft.isValid(): Boolean {
    return when (authMode) {
        AuthMode.PASSWORD -> password.isNotBlank()
        AuthMode.KEY -> !keyPath.isNullOrBlank()
        AuthMode.AUTO -> !keyPath.isNullOrBlank() || password.isNotBlank()
    }
}
