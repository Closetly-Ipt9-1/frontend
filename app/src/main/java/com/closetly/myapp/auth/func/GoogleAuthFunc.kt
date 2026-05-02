package com.closetly.myapp.auth.func

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.closetly.myapp.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthFunc(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun signIn(context: Context) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = try {
            CredentialManager.create(context)
                .getCredential(context = context, request = request)
                .credential
        } catch (exception: Exception) {
            Log.w(TAG, "signInWithGoogle:getCredentialFailure", exception)
            throw exception
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Credential is not a Google ID token.")
        }

        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )
            auth.signInWithCredential(firebaseCredential).await()
            Log.d(TAG, "signInWithGoogle:success")
        } catch (exception: GoogleIdTokenParsingException) {
            Log.w(TAG, "signInWithGoogle:invalidToken", exception)
            throw exception
        } catch (exception: Exception) {
            Log.w(TAG, "signInWithGoogle:firebaseFailure", exception)
            throw exception
        }
    }

    companion object {
        private const val TAG = "GoogleAuthFunc"
    }
}
