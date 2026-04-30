package com.m306.closetly.premium.func

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.m306.closetly.R
import com.m306.closetly.premium.model.SubscriptionStatus

private const val CHANNEL_ID = "premium_subscription"
private const val EXPIRY_NOTIFICATION_ID = 3061
private const val WARNING_WINDOW_MILLIS = 7L * 24L * 60L * 60L * 1000L

fun createPremiumNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        CHANNEL_ID,
        "Premium subscription",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Subscription expiry reminders"
    }

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(channel)
}

fun shouldWarnAboutExpiry(status: SubscriptionStatus, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val expiry = status.estimatedExpiryMillis ?: return false
    return status.isPremium &&
        !status.isAutoRenewing &&
        expiry > nowMillis &&
        expiry - nowMillis <= WARNING_WINDOW_MILLIS
}

fun showPremiumExpiryNotification(context: Context, status: SubscriptionStatus) {
    if (!shouldWarnAboutExpiry(status)) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Your Closetly Premium plan is ending soon")
        .setContentText("Open Closetly to keep Premium benefits active.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context)
        .notify(EXPIRY_NOTIFICATION_ID, notification)
}
