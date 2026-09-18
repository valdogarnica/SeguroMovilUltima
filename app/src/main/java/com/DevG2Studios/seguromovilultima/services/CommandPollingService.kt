package com.DevG2Studios.seguromovilultima.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.DevG2Studios.seguromovilultima.R
import com.DevG2Studios.seguromovilultima.data.local.SecureTokenStorage
import com.DevG2Studios.seguromovilultima.data.remote.api.CommandApi
import com.DevG2Studios.seguromovilultima.data.remote.service.CameraTriggerActivity
import com.DevG2Studios.seguromovilultima.receivers.PollingWatchdogScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CommandPollingService : Service() {

    @Inject
    lateinit var commandApi: CommandApi

    @Inject
    lateinit var tokenStorage: SecureTokenStorage

    private val mideviceId: String get() = tokenStorage.getToken() ?: "unknown-device"

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var pollingJob: Job? = null

    companion object {
        private const val TAG = "COMMAND_POLLING"
        private const val POLL_INTERVAL_MS = 10_000L

        private const val CHANNEL_ID = "command_polling_channel"
        private const val NOTIFICATION_ID = 3001
    }
    //private const val DEVICE_ID = "test-device-002"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        PollingWatchdogScheduler.scheduleNext(this)
        startPolling()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "Sincronización de comandos", NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SeguroMovilUltima")
            .setContentText("Sincronizando con el servidor")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (true) {
                try {
                    val response = commandApi.getPendingCommands(mideviceId)
                    if (response.isSuccessful) {
                        response.body()?.forEach { command ->
                            if(command.deviceId == mideviceId){
                                Log.d(TAG, "Comando recibido: ${command.type}")
                                executeCommand(command.type, command.id)
                                commandApi.acknowledgeCommand(command.id)
                            }
                            else{
                                Log.e(TAG, "Comando ${command.id} no pertenece a este dispositivo")
                            }

                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al consultar comandos: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun executeCommand(type: String, commandId: Long) {
        when (type) {
            "PHOTO", "VIDEO", "AUDIO" -> {
                // Requiere pasar por una Activity para tener estado "en primer plano"
                val activityIntent = Intent(this, CameraTriggerActivity::class.java).apply {
                    putExtra(CommandExecutorService.EXTRA_COMMAND, mapToInternalCommand(type))
                    putExtra(CommandExecutorService.EXTRA_COMMAND_ID, commandId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(activityIntent)

            }
            "ALARM", "STOP_ALARM", "LOCATE" -> {
                val mappedCommand = mapToInternalCommand(type) ?: return
                startForegroundService(Intent(this, CommandExecutorService::class.java).apply {
                    putExtra(CommandExecutorService.EXTRA_COMMAND, mappedCommand)
                    putExtra(CommandExecutorService.EXTRA_COMMAND_ID, commandId)
                })
            }
        }
    }

    private fun mapToInternalCommand(type: String): String? = when (type) {
        "ALARM" -> CommandExecutorService.COMMAND_ALARM
        "STOP_ALARM" -> CommandExecutorService.COMMAND_STOP_ALARM
        "PHOTO" -> CommandExecutorService.COMMAND_PHOTO
        "VIDEO" -> CommandExecutorService.COMMAND_VIDEO
        "AUDIO" -> CommandExecutorService.COMMAND_AUDIO
        "LOCATE" -> CommandExecutorService.COMMAND_LOCATE
        else -> null
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Tarea removida por el usuario, reprogramando reinicio del servicio")

        val restartIntent = Intent(applicationContext, CommandPollingService::class.java).apply {
            setPackage(packageName)
        }

        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000, // reinicia en 1 segundo
            pendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}