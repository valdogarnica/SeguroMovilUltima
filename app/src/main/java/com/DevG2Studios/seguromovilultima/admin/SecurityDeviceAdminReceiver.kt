package com.DevG2Studios.seguromovilultima.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import com.DevG2Studios.seguromovilultima.data.local.SecuritySettingsCache
import com.DevG2Studios.seguromovilultima.services.CommandExecutorService

class SecurityDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DEVICE_ADMIN_TEST"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin ACTIVADO correctamente")
        FailedAttemptTracker.reset()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin DESACTIVADO")
    }

    // Versión moderna (API 24+), la que realmente invoca el sistema en la mayoría de dispositivos actuales
    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        handlePasswordFailed(context)
    }



    private fun handlePasswordFailed(context: Context) {
        if (!SecuritySettingsCache.current.failedAttemptDetectionEnabled) return

        Log.d(TAG, "onPasswordFailed DISPARADO")
        val attempts = FailedAttemptTracker.incrementAndGet()
        Log.d(TAG, "Intentos fallidos acumulados: $attempts")

        if (attempts >= FailedAttemptTracker.THRESHOLD) {
            Log.d(TAG, "Umbral alcanzado, disparando respuesta de seguridad")
            FailedAttemptTracker.reset()
            triggerSecurityResponse(context)
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordSucceeded(context, intent, user)
        Log.d(TAG, "Password correcto, reset contador")
        FailedAttemptTracker.reset()
    }



    private fun triggerSecurityResponse(context: Context) {
        val settings = SecuritySettingsCache.current

        if(settings.sendPhotoOnFailedAttempt) {
            Log.d(TAG, "Iniciando CommandExecutorService: PHOTO")
            val photoIntent = Intent(context, CommandExecutorService::class.java).apply {
                putExtra(CommandExecutorService.EXTRA_COMMAND, CommandExecutorService.COMMAND_PHOTO)
            }
            context.startForegroundService(photoIntent)
        }
        if (settings.sendVideoOnFailedAttempt) {
            Log.d(TAG, "Iniciando CommandExecutorService: VIDEO")
            val videoIntent = Intent(context, CommandExecutorService::class.java).apply {
                putExtra(CommandExecutorService.EXTRA_COMMAND, CommandExecutorService.COMMAND_VIDEO)
            }
            context.startForegroundService(videoIntent)
        }

        if(settings.sendLocationOnFailedAttempt) {
            Log.d(TAG, "Iniciando CommandExecutorService: LOCATE")
            val locateIntent = Intent(context, CommandExecutorService::class.java).apply {
                putExtra(
                    CommandExecutorService.EXTRA_COMMAND,
                    CommandExecutorService.COMMAND_LOCATE
                )
            }
            context.startForegroundService(locateIntent)
        }
        if(settings.startAlarmOnFailedAttempt){
            Log.d(TAG, "Iniciando ComandExecutoService: ALARM")
            val alarmIntent = Intent(context, CommandExecutorService::class.java).apply {
                putExtra(CommandExecutorService.EXTRA_COMMAND, CommandExecutorService.COMMAND_ALARM)
            }
            context.startForegroundService(alarmIntent)
        }
    }
}

object FailedAttemptTracker {
    const val THRESHOLD = 3
    private var count = 0

    fun incrementAndGet(): Int {
        count++
        return count
    }

    fun reset() {
        count = 0
    }
}