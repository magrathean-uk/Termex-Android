package com.termex.app.core.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncMode(val raw: String) {
    LOCAL_ONLY("local_only"),
    GOOGLE_DRIVE("google_drive");

    companion object {
        fun fromRaw(raw: String?): SyncMode {
            return entries.firstOrNull { it.raw == raw } ?: LOCAL_ONLY
        }
    }
}

@Serializable
enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

@Serializable
data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val message: String = "Not synced yet.",
    val syncedAtMillis: Long? = null,
    val missingSecretCount: Int = 0,
    val googleAccountEmail: String? = null
)

@Serializable
sealed class SyncResult {
    abstract val status: SyncStatus

    @Serializable
    data class Success(
        val snapshot: MetadataSnapshot,
        override val status: SyncStatus = SyncStatus(state = SyncState.SUCCESS),
        val remoteFileId: String? = null,
        val remoteRevisionId: String? = null
    ) : SyncResult()

    @Serializable
    data class NoChange(
        override val status: SyncStatus = SyncStatus(state = SyncState.SUCCESS, message = "Already up to date.")
    ) : SyncResult()

    @Serializable
    data class Failure(
        val errorMessage: String,
        override val status: SyncStatus = SyncStatus(state = SyncState.ERROR, message = "Sync failed.")
    ) : SyncResult()
}
