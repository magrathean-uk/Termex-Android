package com.termex.app.core.sync

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.termex.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

@Singleton
class GoogleDriveAuthorizationManagerImpl @Inject constructor() : GoogleDriveAuthorizationManager {

    override suspend fun signIn(activity: Activity): Result<GoogleAccountIdentity> {
        return runCatching {
            val webClientId = requireWebClientId()
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )
            val credential = response.credential as? CustomCredential
                ?: error("Google sign-in did not return a Google credential.")
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAccountIdentity(email = googleCredential.id)
        }
    }

    override suspend fun authorizeDriveAccess(
        activity: Activity,
        accountEmail: String?
    ): Result<GoogleDriveAuthorizationResult> {
        return runCatching {
            val authorizationClient = Identity.getAuthorizationClient(activity)
            val builder = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))

            if (!accountEmail.isNullOrBlank()) {
                builder.setAccount(Account(accountEmail, "com.google"))
            }

            val result = authorizationClient.authorize(builder.build()).awaitResult()
            if (result.hasResolution()) {
                val pendingIntent = requireNotNull(result.pendingIntent) {
                    "Google Drive authorization needs a resolution."
                }
                GoogleDriveAuthorizationResult.NeedsResolution(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } else {
                val accessToken = result.accessToken?.takeIf { it.isNotBlank() }
                    ?: error("Google Drive authorization did not return an access token.")
                GoogleDriveAuthorizationResult.Authorized(accessToken)
            }
        }
    }

    override suspend fun handleAuthorizationResult(context: Context, data: Intent?): Result<String> {
        return runCatching {
            val authorizationResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data ?: error("Missing authorization result intent."))
            authorizationResult.accessToken?.takeIf { it.isNotBlank() }
                ?: error("Google Drive authorization did not return an access token.")
        }
    }

    override suspend fun disconnect(context: Context) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
        runCatching {
            Identity.getAuthorizationClient(context)
                .revokeAccess(RevokeAccessRequest.builder().build())
                .awaitResult()
        }
    }

    private fun requireWebClientId(): String {
        return BuildConfig.GOOGLE_WEB_CLIENT_ID.takeIf { it.isNotBlank() }
            ?: error("Google auth is not configured. Set TERMEX_GOOGLE_WEB_CLIENT_ID before building.")
    }
}
