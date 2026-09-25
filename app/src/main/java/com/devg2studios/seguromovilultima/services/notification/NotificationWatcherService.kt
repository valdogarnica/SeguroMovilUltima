package com.devg2studios.seguromovilultima.services.notification



import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.devg2studios.seguromovilultima.services.CommandPollingService

class NotificationWatcherService : NotificationListenerService() {

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        if (sbn.packageName == packageName && sbn.id == 3001) {
            Log.d("NOTIF_WATCHER", "Notificación de polling removida, reiniciando servicio")
            val restartIntent = Intent(this, CommandPollingService::class.java)
            startForegroundService(restartIntent)
        }
    }
}