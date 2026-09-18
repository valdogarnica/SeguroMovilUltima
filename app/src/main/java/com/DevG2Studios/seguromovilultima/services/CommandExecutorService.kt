package com.DevG2Studios.seguromovilultima.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.DevG2Studios.seguromovilultima.R
import com.DevG2Studios.seguromovilultima.data.local.SecureTokenStorage
import com.DevG2Studios.seguromovilultima.data.local.SecuritySettingsCache
import com.DevG2Studios.seguromovilultima.data.remote.api.CommandApi
import com.DevG2Studios.seguromovilultima.data.remote.dto.LocationReportDto
import com.DevG2Studios.seguromovilultima.service.codigoIncorrecto.VideoCaptureHelper
import com.DevG2Studios.seguromovilultima.services.codigoIncorrecto.AudioCaptureHelper
import com.DevG2Studios.seguromovilultima.services.codigoIncorrecto.OneShotLocationHelper
import com.DevG2Studios.seguromovilultima.services.codigoIncorrecto.ResultReporter
import com.DevG2Studios.seguromovilultima.ui.views.codigoIncorrecto.flash.FlashScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class CommandExecutorService : LifecycleService() {
    @Inject
    lateinit var commandApi: CommandApi

    @Inject
    lateinit var tokenStorage: SecureTokenStorage

    private val activeTasks = AtomicInteger(0)

    private val deviceId: String get() = tokenStorage.getToken() ?: "unknown-device"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)



        val command = intent?.getStringExtra(EXTRA_COMMAND) ?: return START_NOT_STICKY
        val serviceType = when (command) {
            COMMAND_PHOTO, COMMAND_VIDEO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            COMMAND_LOCATE -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            COMMAND_AUDIO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), serviceType)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        activeTasks.incrementAndGet()
        Log.d(TAG, "Comando '$command' iniciado. Tareas activas: ${activeTasks.get()}")

        when (command) {
            COMMAND_ALARM -> {
                AlarmHelper.playAlarm(this)
                if (SecuritySettingsCache.current.showFlashScreenOnAlarm) {
                    launchFlashScreen()
                }
                stopAlarmAfterDelay()
            }
            COMMAND_STOP_ALARM -> {
                AlarmHelper.stopAlarm(this)
                finishTask()
            }
            COMMAND_PHOTO -> {
                val commandId = intent.getLongExtra(EXTRA_COMMAND_ID, -1L)
                lifecycleScope.launch {
                    try {
                        val uri = PhotoCaptureHelper.captureSilentPhoto(
                            context = this@CommandExecutorService,
                            lifecycleOwner = this@CommandExecutorService,
                            useFrontCamera = true
                        )
                        ResultReporter.report(commandApi, commandId, deviceId, success = true, fileUri = uri.toUri(), context = this@CommandExecutorService)
                        Log.d(TAG, "Foto capturada exitosamente: $uri  commandApi: $commandApi commandId: $commandId deciveID: $deviceId ")
                    } catch (e: Exception) {
                        ResultReporter.report(commandApi, commandId, deviceId, success = false, errorMessage = e.message)
                        Log.e(TAG, "ERROR al capturar foto: ${e.message}", e)
                    } finally {
                        finishTask()
                    }
                }
            }
            COMMAND_VIDEO -> {
                val commandId = intent.getLongExtra(EXTRA_COMMAND_ID, -1L)
                lifecycleScope.launch {
                    try {
                        val uri = VideoCaptureHelper.captureSilentVideo(
                            context = this@CommandExecutorService,
                            lifecycleOwner = this@CommandExecutorService,
                            useFrontCamera = true,
                            durationMillis = 10_000L
                        )
                        ResultReporter.report(commandApi, commandId, deviceId, success = true, fileUri = uri.toUri(), context = this@CommandExecutorService)
                        Log.d(TAG, "Video capturado exitosamente: $uri")
                    } catch (e: Exception) {
                        ResultReporter.report(commandApi, commandId, deviceId, success = false, errorMessage = e.message)
                        Log.e(TAG, "ERROR al capturar video: ${e.message}", e)
                    } finally {
                        finishTask()
                    }
                }
            }
            COMMAND_AUDIO -> {
                val commandId = intent.getLongExtra(EXTRA_COMMAND_ID, -1L)
                lifecycleScope.launch {
                    try {
                        val uri = AudioCaptureHelper.captureSilentAudio(context = this@CommandExecutorService, durationMillis = 10_000L)
                        ResultReporter.report(commandApi, commandId, deviceId, success = true, fileUri = uri.toUri(), context = this@CommandExecutorService)
                        Log.d(TAG, "Audio capturado exitosamente: $uri")
                    } catch (e: Exception) {
                        ResultReporter.report(commandApi, commandId, deviceId, success = false, errorMessage = e.message)
                        Log.e(TAG, "ERROR al capturar audio: ${e.message}", e)
                    } finally {
                        finishTask()
                    }
                }
            }
            COMMAND_LOCATE -> {
                val commandId = intent.getLongExtra(EXTRA_COMMAND_ID, -1L)
                lifecycleScope.launch {
                    try {
                        val location = OneShotLocationHelper.getCurrentLocation(this@CommandExecutorService)
                        Log.d(TAG, "Ubicación obtenida: ${location.latitude}, ${location.longitude}")

                        commandApi.reportLocation(
                            deviceId = deviceId,
                            location = LocationReportDto(location.latitude, location.longitude)
                        )
                        ResultReporter.report(commandApi, commandId, deviceId, success = true)
                    } catch (e: Exception) {
                        ResultReporter.report(commandApi, commandId, deviceId, success = false, errorMessage = e.message)
                        Log.e(TAG, "ERROR al obtener/reportar ubicación: ${e.message}", e)
                    } finally {
                        finishTask()
                    }
                }
            }
            else -> finishTask()
        }

        return START_NOT_STICKY
    }

    private fun launchFlashScreen() {
        val flashIntent = Intent(this, FlashScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(flashIntent)
    }

    private fun stopAlarmAfterDelay() {
        lifecycleScope.launch {
            delay(30_000)
            AlarmHelper.stopAlarm(this@CommandExecutorService)
            finishTask()
        }
    }

    private fun finishTask() {
        val remaining = activeTasks.decrementAndGet()
        Log.d(TAG, "Tarea finalizada. Tareas restantes: $remaining")
        if (remaining <= 0) {
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        createChannelIfNeeded()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ejecutando comando de seguridad")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun createChannelIfNeeded() {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val channel = NotificationChannel(
            CHANNEL_ID, "Comandos remotos", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "COMMAND_EXECUTOR"
        const val EXTRA_COMMAND = "extra_command"
        const val COMMAND_ALARM = "ALARM"
        const val COMMAND_STOP_ALARM = "STOP_ALARM"
        const val COMMAND_PHOTO = "PHOTO"
        const val COMMAND_VIDEO = "VIDEO"
        const val COMMAND_LOCATE = "LOCATE"
        const val COMMAND_AUDIO = "AUDIO"
        private const val CHANNEL_ID = "command_channel"
        private const val NOTIFICATION_ID = 2002
        const val EXTRA_COMMAND_ID = "extra_command_id"
    }
}