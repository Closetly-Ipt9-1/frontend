package com.closetly.myapp.auth.func

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth



object AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

}