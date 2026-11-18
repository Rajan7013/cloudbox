package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.DriveUser
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "CloudBoxAuth"

const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

fun FirebaseUser.toDriveUser(): DriveUser =
  DriveUser(
    uid = uid,
    displayName = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@') ?: "CloudBox User",
    email = email.orEmpty(),
    photoUrl = photoUrl?.toString(),
  )

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

sealed interface DriveTokenAttemptResult {
  data class Token(val accessToken: String) : DriveTokenAttemptResult
  data class NeedsConsent(val authorizationResult: AuthorizationResult) : DriveTokenAttemptResult
  data class Error(val message: String) : DriveTokenAttemptResult
}

class DriveAuthManager(private val appContext: Context) {
  private val _accessToken = MutableStateFlow<String?>(null)
  val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

  fun currentTokenOrNull(): String? = _accessToken.value

  fun clearToken() {
    _accessToken.value = null
  }

  fun setToken(token: String?) {
    _accessToken.value = token
  }

  suspend fun requestDriveAuthorization(
    activityOrContext: Context = appContext,
  ): DriveTokenAttemptResult {
    return try {
      val scopes = listOf(Scope(DRIVE_FILE_SCOPE))
      val request =
        AuthorizationRequest.builder()
          .setRequestedScopes(scopes)
          .build()

      val result =
        Identity.getAuthorizationClient(activityOrContext)
          .authorize(request)
          .await()

      if (result.hasResolution() && result.pendingIntent != null) {
        DriveTokenAttemptResult.NeedsConsent(result)
      } else {
        val token = result.accessToken
        if (!token.isNullOrBlank()) {
          _accessToken.value = token
          DriveTokenAttemptResult.Token(token)
        } else {
          DriveTokenAttemptResult.Error("Google Drive did not return an access token.")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Drive authorization failed: ${e.message}", e)
      DriveTokenAttemptResult.Error(e.localizedMessage ?: "Unable to connect to Google Drive.")
    }
  }

  fun extractTokenFromConsentIntent(
    context: Context,
    intentData: Intent?,
  ): Result<String> {
    return try {
      val result =
        Identity.getAuthorizationClient(context)
          .getAuthorizationResultFromIntent(intentData)
      val token = result.accessToken
      if (!token.isNullOrBlank()) {
        _accessToken.value = token
        Result.success(token)
      } else {
        Result.failure(IllegalStateException("Google Drive authorization was not granted."))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed extracting Drive token from consent intent", e)
      Result.failure(e)
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: (DriveUser) -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  val existing = Firebase.auth.currentUser
  if (existing != null) {
    onAuthSuccess(existing.toDriveUser())
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return
    }
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  scope.launch {
    try {
      val cred = credentialManager.getCredential(context, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        val authRes = Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        val user = authRes.user?.toDriveUser()
        if (user != null) onAuthSuccess(user) else onUnauthenticated()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: (DriveUser) -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onAuthError("Google Sign-In configuration missing.")
      return
    }
  val activityContext = context as? Activity
  if (activityContext == null) {
    onAuthError("Unable to start sign-in from this context.")
    return
  }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val cred = credentialManager.getCredential(activityContext, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        val authRes = Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        val user = authRes.user?.toDriveUser()
        if (user != null) {
          onAuthSuccess(user)
        } else {
          onAuthError("Google Sign-In succeeded but no user profile was returned.")
        }
      } else {
        onAuthError("Unexpected credential type returned.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In cancelled", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
    }
  }
}

fun signOutUser(
  credentialManager: CredentialManager,
  driveAuthManager: DriveAuthManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        driveAuthManager.clearToken()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}
