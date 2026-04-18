package com.termex.app.core.sync

import android.app.Activity
import android.content.Context
import android.content.Intent

interface GoogleDriveAuthorizationManager {
    suspend fun signIn(activity: Activity): Result<GoogleAccountIdentity>

    suspend fun authorizeDriveAccess(
        activity: Activity,
        accountEmail: String?
    ): Result<GoogleDriveAuthorizationResult>

    suspend fun handleAuthorizationResult(context: Context, data: Intent?): Result<String>

    suspend fun disconnect(context: Context)
}
