package com.m306.closetly.auth.func

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

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
    ){

    }
}