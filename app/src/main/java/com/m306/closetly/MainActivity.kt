package com.m306.closetly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import com.m306.closetly.auth.ui.LoginScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = FirebaseApp.initializeApp(this)

        setContent {
            LoginScreen()
        }
    }
}