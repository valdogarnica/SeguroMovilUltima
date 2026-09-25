package com.DevG2Studios.seguromovilultima.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.DevG2Studios.seguromovilultima.R
import com.DevG2Studios.seguromovilultima.data.local.DeviceIdProvider
import com.DevG2Studios.seguromovilultima.data.local.SecureTokenStorage
import com.DevG2Studios.seguromovilultima.data.local.SecuritySettingsCache
import com.DevG2Studios.seguromovilultima.data.remote.api.CommandApi
import com.DevG2Studios.seguromovilultima.data.remote.dto.LocationReportDto
import com.DevG2Studios.seguromovilultima.service.codigoIncorrecto.VideoCaptureHelper
import com.DevG2Studios.seguromovilultima.services.apagadoFalso.FakeShutdownActivity
import com.DevG2Studios.seguromovilultima.services.apagadoFalso.FakeShutdownStageHolder
import com.DevG2Studios.seguromovilultima.services.apagadoFalso.ShutdownMenuAccessibilityService
import com.DevG2Studios.seguromovilultima.services.apagadoFalso.ShutdownStage
import com.DevG2Studios.seguromovilultima.services.codigoIncorrecto.AudioCaptureHelper
import com.DevG2Studios.seguromovilultima.services.codigoIncorrecto.OneShotLocationHelper
import com.DevG2Studios.seguromovilultima.services.codigoIncorrecto.ResultReporter
import com.DevG2Studios.seguromovilultima.services.pantallaRemota.RemoteScreenService
import com.DevG2Studios.seguromovilultima.ui.views.codigoIncorrecto.flash.FlashScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

// imports para la captura de pantall remota
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@AndroidEntryPoint
class CommandExecutorService : LifecycleService() {
    @Inject
    lateinit var commandApi: CommandApi

    @Inject
    lateinit var tokenStorage: SecureTokenStorage

    @Inject
    lateinit var deviceIdProvider: DeviceIdProvider

    private val activeTasks = AtomicInteger(0)

    //private val deviceId: String get() = tokenStorage.getToken() ?: "unknown-device"
    private val deviceId: String get() = deviceIdProvider.getDeviceId()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val command = intent?.getStringExtra(EXTRA_COMMAND) ?: return START_NOT_STICKY
        val serviceType = when (command) {
            COMMAND_PHOTO, COMMAND_VIDEO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            COMMAND_LOCATE -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            COMMAND_AUDIO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            //else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, buildNotification(), serviceType)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar FGS para comando $command", e)
            finishTask()
            return START_NOT_STICKY
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
                        Log.d(TAG, "entrando al comando foto")
                        val uri = PhotoCaptureHelper.captureSilentPhoto(
                            context = this@CommandExecutorService,
                            lifecycleOwner = this@CommandExecutorService,
                            useFrontCamera = true
                        )
                        Log.d(TAG, "me quede aqui colgdo")
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
            COMMAND_FAKE_SHUTDOWN_OFF -> {
                val commandId = intent.getLongExtra(EXTRA_COMMAND_ID, -1L)
                lifecycleScope.launch {
                    try {
                        val activity = FakeShutdownActivity.instance
                        if (activity != null) {
                            // Camino normal: la Activity sigue viva, hace la limpieza completa
                            // (restaura notificaciones, brillo, oculta overlay, se cierra)
                            activity.closeFakeShutdown()
                        } else {
                            // Camino de emergencia: la Activity ya no existe (crasheó o el
                            // sistema la mató), pero el overlay podría seguir visible.
                            // Lo removemos directo desde el servicio de accesibilidad.
                            ShutdownMenuAccessibilityService.instance?.hideBlackOverlay()
                            FakeShutdownStageHolder.current = ShutdownStage.MENU
                            ShutdownMenuAccessibilityService.isFakeShutdownUiShowing = false
                        }
                        Log.d(TAG, "Apagado falso cerrado remotamente")
                        ResultReporter.report(commandApi, commandId, deviceId, success = true)
                    } catch (e: Exception) {
                        Log.e(TAG, "ERROR al cerrar apagado falso remotamente: ${e.message}", e)
                        ResultReporter.report(commandApi, commandId, deviceId, success = false, errorMessage = e.message)
                    } finally {
                        finishTask()
                    }
                }
            }
            COMMAND_SCREENSHOT -> {
                val commandId = intent.getLongExtra(EXTRA_COMMAND_ID, -1L)
                lifecycleScope.launch {
                    try {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            throw IllegalStateException("Captura de pantalla requiere Android 11 o superior")
                        }
                        val service = ShutdownMenuAccessibilityService.instance
                            ?: throw IllegalStateException("Servicio de accesibilidad no disponible")

                        val (uri, error) = suspendCancellableCoroutine { cont ->
                            service.captureScreenshot { resultUri, errorMsg ->
                                cont.resume(resultUri to errorMsg)
                            }
                        }

                        if (uri != null) {
                            ResultReporter.report(commandApi, commandId, deviceId, success = true, fileUri = uri, context = this@CommandExecutorService)
                            Log.d(TAG, "Captura de pantalla exitosa: $uri")
                        } else {
                            ResultReporter.report(commandApi, commandId, deviceId, success = false, errorMessage = error ?: "Error desconocido")
                            Log.e(TAG, "Error al capturar pantalla: $error")
                        }
                    } catch (e: Exception) {
                        ResultReporter.report(commandApi, commandId, deviceId, success = false, errorMessage = e.message)
                        Log.e(TAG, "ERROR al capturar pantalla: ${e.message}", e)
                    } finally {
                        finishTask()
                    }
                }
            }
            COMMAND_REMOTE_SCREEN_ON -> {
                startService(Intent(this, RemoteScreenService::class.java).apply {
                    action = RemoteScreenService.ACTION_START
                })
                finishTask()
            }
            COMMAND_REMOTE_SCREEN_OFF -> {
                startService(Intent(this, RemoteScreenService::class.java).apply {
                    action = RemoteScreenService.ACTION_STOP
                })
                finishTask()
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
        const val COMMAND_FAKE_SHUTDOWN_OFF = "FAKE_SHUTDOWN_OFF"
        const val COMMAND_SCREENSHOT = "SCREENSHOT"
        const val COMMAND_REMOTE_SCREEN_ON = "REMOTE_SCREEN_ON"
        const val COMMAND_REMOTE_SCREEN_OFF = "REMOTE_SCREEN_OFF"
        private const val CHANNEL_ID = "command_channel"
        private const val NOTIFICATION_ID = 2002
        const val EXTRA_COMMAND_ID = "extra_command_id"
    }
}