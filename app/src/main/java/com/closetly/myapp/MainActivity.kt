package com.closetly.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import com.closetly.myapp.navigation.ClosetlyApp
import com.closetly.myapp.premium.func.createPremiumNotificationChannel
import com.closetly.myapp.ui.theme.ClosetlyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = FirebaseApp.initializeApp(this)
        createPremiumNotificationChannel(this)

        setContent {
            ClosetlyTheme {
                ClosetlyApp()
            }
        }
    }
}
