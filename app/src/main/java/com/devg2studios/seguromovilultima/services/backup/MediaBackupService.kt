package com.devg2studios.seguromovilultima.services.backup

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.devg2studios.seguromovilultima.R
import com.devg2studios.seguromovilultima.data.local.SecuritySettingsCache
import com.devg2studios.seguromovilultima.data.local.backup.SyncedMediaDao
import com.devg2studios.seguromovilultima.services.notification.PersistentNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MediaBackupService : LifecycleService() {

    @Inject lateinit var dao: SyncedMediaDao
    @Inject lateinit var scanner: MediaBackupScanner

    private var observer: ContentObserver? = null
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    companion object {
        private const val TAG = "MEDIA_BACKUP"
        private const val CHANNEL_ID = "media_backup_channel"
        private const val NOTIFICATION_ID = 6001
        private const val DEBOUNCE_MS = 3000L
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaBackupService onCreate 1")
        Log.d(TAG, "MediaBackupService onCreate 2")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand llamado")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                startForeground(PersistentNotificationManager.NOTIFICATION_ID, PersistentNotificationManager.build(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(PersistentNotificationManager.NOTIFICATION_ID, PersistentNotificationManager.build(this))
            }
            Log.d(TAG, "startForeground exitoso")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en startForeground: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        registerObserverIfNeeded()
        lifecycleScope.launch {
            val wifiOnly = SecuritySettingsCache.current.mediaBackupWifiOnly
            WhatsAppMediaIndexer.indexPrivateMedia(applicationContext)
            scanner.resetQueueAndScan(wifiOnly) // limpia huérfanos y vuelve a escanear
        }
        return START_STICKY
    }

    private fun registerObserverIfNeeded() {
        if (observer != null) return
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                // Debounce: varias fotos seguidas (ej. ráfaga) disparan
                // múltiples eventos; agrupamos en un solo escaneo.
                debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
                debounceRunnable = Runnable { triggerScan() }.also {
                    debounceHandler.postDelayed(it, DEBOUNCE_MS)
                }
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer!!
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer!!
        )
    }

    private fun triggerScan() {
        Log.d(TAG, "triggerScan() invocado")
        lifecycleScope.launch {
            val wifiOnly = SecuritySettingsCache.current.mediaBackupWifiOnly
            Log.d(TAG, "Ejecutando escaneo, wifiOnly=$wifiOnly")
            scanner.scanAndEnqueuePending(wifiOnly)
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "Copia de seguridad de fotos", NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SeguroMovilUltima")
            .setContentText("Respaldando fotos y videos")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        observer?.let { contentResolver.unregisterContentObserver(it) }
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Tarea removida, reprogramando reinicio de MediaBackupService")

        val restartIntent = Intent(applicationContext, MediaBackupService::class.java).apply {
            setPackage(packageName)
        }

        val pendingIntent = PendingIntent.getService(
            this, 2, restartIntent,  // request code distinto al de CommandPollingService (usa 1)
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }
}