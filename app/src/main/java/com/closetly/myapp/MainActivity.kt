package com.closetly.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.closetly.myapp.BuildConfig
import com.google.firebase.FirebaseApp
import com.closetly.myapp.navigation.ClosetlyApp
import com.closetly.myapp.premium.func.createPremiumNotificationChannel
import com.closetly.myapp.ui.theme.ClosetlyTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = FirebaseApp.initializeApp(this)
        if (BuildConfig.DEBUG) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf("2D08780D61ACD21FDEBF47DD6BCB4CA9"))
                    .build()
            )
        }
        MobileAds.initialize(this) {}
        createPremiumNotificationChannel(this)

        setContent {
            ClosetlyTheme {
                ClosetlyApp()
            }
        }
    }
}
