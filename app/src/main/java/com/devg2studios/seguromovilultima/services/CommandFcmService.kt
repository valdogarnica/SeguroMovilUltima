package com.devg2studios.seguromovilultima.services

import android.content.Intent
import android.util.Log
import com.devg2studios.seguromovilultima.data.local.DeviceIdProvider
import com.devg2studios.seguromovilultima.data.remote.api.CommandApi
import com.devg2studios.seguromovilultima.data.remote.dto.FcmTokenDto
import com.devg2studios.seguromovilultima.data.remote.service.CameraTriggerActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CommandFcmService : FirebaseMessagingService() {

    @Inject lateinit var commandApi: CommandApi
    @Inject lateinit var deviceIdProvider: DeviceIdProvider   // <-- corregido, ya no tokenStorage

    companion object {
        private const val TAG = "COMMAND_FCM"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Push FCM recibido: ${message.data}")

        val type = message.data["type"]
        val commandId = message.data["commandId"]?.toLongOrNull()

        if (type != null && commandId != null) {
            // Camino directo: sabemos exactamente qué comando ejecutar
            when (type) {
                "PHOTO", "VIDEO", "AUDIO" -> {
                    val mappedCommand = mapToInternalCommand(type) ?: return
                    val activityIntent = Intent(this, CameraTriggerActivity::class.java).apply {
                        putExtra(CommandExecutorService.EXTRA_COMMAND, mappedCommand)
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
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    commandApi.acknowledgeCommand(commandId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al confirmar comando vía FCM: ${e.message}")
                }
            }
        } else {
            // Fallback: push genérico sin data específica, forzamos un ciclo de polling
            Log.d(TAG, "Push sin type/commandId, disparando polling genérico")
            startForegroundService(Intent(this, CommandPollingService::class.java))
        }
        runCatching {
            startForegroundService(Intent(this, CommandPollingService::class.java))
            startForegroundService(Intent(this, LocationForegroundService::class.java))
        }.onFailure { Log.w(TAG, "No se pudo revivir servicios", it) }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM generado: $token")

        val deviceId = deviceIdProvider.getDeviceId()  // <-- corregido
        CoroutineScope(Dispatchers.IO).launch {
            try {
                commandApi.updateFcmToken(deviceId, FcmTokenDto(token))
                Log.d(TAG, "Token FCM reportado al backend correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al reportar token FCM: ${e.message}")
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
}