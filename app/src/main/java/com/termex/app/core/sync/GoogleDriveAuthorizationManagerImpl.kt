package com.termex.app.core.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveAuthorizationManagerImpl @Inject constructor() : GoogleDriveAuthorizationManager {
    override suspend fun signIn(activity: Activity): Result<GoogleAccountIdentity> {
        return Result.failure(UnsupportedOperationException("Google Drive sync is not included in the open-source build."))
    }

    override suspend fun authorizeDriveAccess(
        activity: Activity,
        accountEmail: String?
    ): Result<GoogleDriveAuthorizationResult> {
        return Result.failure(UnsupportedOperationException("Google Drive sync is not included in the open-source build."))
    }

    override suspend fun handleAuthorizationResult(context: Context, data: Intent?): Result<String> {
        return Result.failure(UnsupportedOperationException("Google Drive sync is not included in the open-source build."))
    }

    override suspend fun disconnect(context: Context) = Unit
}
