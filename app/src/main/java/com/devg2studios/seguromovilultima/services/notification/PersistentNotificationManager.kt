package com.devg2studios.seguromovilultima.services.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.devg2studios.seguromovilultima.R

object PersistentNotificationManager {
    const val CHANNEL_ID = "app_background_channel"

    const val NOTIFICATION_ID = 9001

    fun build(context: Context, text: String = "Protegiendo usuario"): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "Protección Activa", NotificationManager.IMPORTANCE_MIN
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentText(text)
            .setSmallIcon(R.drawable.baseline_bolt_24)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
}