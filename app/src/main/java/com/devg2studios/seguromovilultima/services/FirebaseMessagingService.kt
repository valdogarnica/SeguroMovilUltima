

package com.devg2studios.seguromovilultima.services

import android.content.Intent
import android.util.Log
import com.devg2studios.seguromovilultima.data.local.SecureTokenStorage
import com.devg2studios.seguromovilultima.data.remote.api.CommandApi
import com.devg2studios.seguromovilultima.data.remote.dto.FcmTokenDto
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SeguroFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var commandApi: CommandApi
    @Inject lateinit var tokenStorage: SecureTokenStorage

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM_SERVICE", "Push recibido: ${message.data}")

        // Disparamos el polling service para que haga su ciclo de inmediato
        startForegroundService(Intent(this, CommandPollingService::class.java))
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val deviceId = tokenStorage.getToken() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                commandApi.updateFcmToken(deviceId, FcmTokenDto(token))
            } catch (e: Exception) {
                Log.e("FCM_SERVICE", "Error actualizando token: ${e.message}")
            }
        }
    }
}