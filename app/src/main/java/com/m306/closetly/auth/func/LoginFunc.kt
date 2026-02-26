package com.m306.closetly.auth.func

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class LoginFunc (private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                }
            }
    }

    companion object {
        private const val TAG = "LoginFunc"
    }
}