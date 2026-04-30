package com.closetly.myapp.auth.func

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterFunc(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun register(
        name: String,
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user == null) {
                        onSuccess()
                        return@addOnCompleteListener
                    }

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.ifBlank { username })
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d(TAG, "createUserWithEmail:success")
                                onSuccess()
                            } else {
                                Log.w(TAG, "updateProfile:failure", updateTask.exception)
                                onError(updateTask.exception)
                            }
                        }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    onError(task.exception)
                }
            }
    }

    companion object {
        private const val TAG = "RegisterFunc"
    }
}