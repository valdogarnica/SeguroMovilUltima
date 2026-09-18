package com.DevG2Studios.seguromovilultima.services


import android.content.Intent
import android.os.Build
import android.util.Log
import com.DevG2Studios.seguromovilultima.data.local.SecureTokenStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CommandFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var commandApi: com.DevG2Studios.seguromovilultima.data.remote.api.CommandApi

    @Inject
    lateinit var tokenStorage: SecureTokenStorage

    companion object {
        private const val TAG = "COMMAND_FCM"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"] ?: return
        val commandId = message.data["commandId"]?.toLongOrNull() ?: return

        Log.d(TAG, "Push FCM recibido: type=$type, commandId=$commandId")

        // Este onMessageReceived se ejecuta con la excepción de estado
        // elegible activa: aquí SÍ podemos iniciar un foreground service
        // tipo cámara/micrófono, incluso en Android 15/16.
        when (type) {
            "PHOTO", "VIDEO", "AUDIO" -> {
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
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM generado: $token")

        val deviceId = tokenStorage.getToken() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                commandApi.updateFcmToken(
                    deviceId,
                    com.DevG2Studios.seguromovilultima.data.remote.dto.FcmTokenDto(token)
                )
                Log.d(TAG, "Token FCM reportado al backend correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al reportar token FCM: ${e.message}")
            }
        }
    }

    private fun mapToInternalCommand(type: String): String? = when (type) {
        "PHOTO" -> CommandExecutorService.COMMAND_PHOTO
        "VIDEO" -> CommandExecutorService.COMMAND_VIDEO
        "AUDIO" -> CommandExecutorService.COMMAND_AUDIO
        else -> null
    }
}