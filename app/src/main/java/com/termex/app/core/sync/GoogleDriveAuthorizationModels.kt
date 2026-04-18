package com.termex.app.core.sync

import androidx.activity.result.IntentSenderRequest

sealed interface SyncAuthorizationState {
    data object Disconnected : SyncAuthorizationState
    data object Authorizing : SyncAuthorizationState
    data class Connected(val accountEmail: String) : SyncAuthorizationState
    data class Error(val message: String) : SyncAuthorizationState
}

data class GoogleAccountIdentity(
    val email: String
)

sealed interface GoogleDriveAuthorizationResult {
    data class Authorized(val accessToken: String) : GoogleDriveAuthorizationResult
    data class NeedsResolution(val request: IntentSenderRequest) : GoogleDriveAuthorizationResult
}

enum class MissingSecretKind {
    PASSWORD,
    PRIVATE_KEY,
    CERTIFICATE
}

data class MissingSecretIssue(
    val kind: MissingSecretKind,
    val label: String,
    val detail: String,
    val serverId: String? = null,
    val keyPath: String? = null,
    val certificatePath: String? = null
)
