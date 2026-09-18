package com.DevG2Studios.seguromovilultima.data.repository

import android.os.Build
import com.DevG2Studios.seguromovilultima.data.local.DeviceIdProvider
import com.DevG2Studios.seguromovilultima.data.local.SecureTokenStorage
import com.DevG2Studios.seguromovilultima.data.remote.api.CommandApi
import com.DevG2Studios.seguromovilultima.data.remote.api.AuthApi
import com.DevG2Studios.seguromovilultima.data.remote.dto.LoginRequestDto
import com.DevG2Studios.seguromovilultima.data.remote.dto.RegisterRequestDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val email: String, val deviceId: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val commandApi: CommandApi,
    private val tokenStorage: SecureTokenStorage,
    private val deviceIdProvider: DeviceIdProvider
) {
    suspend fun register(email: String, password: String): AuthResult {
        val deviceId = deviceIdProvider.getDeviceId()
        return try {
            val response = authApi.register(
                RegisterRequestDto(
                    email = email,
                    password = password,
                    deviceId = deviceId,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                )
            )
            if (response.isSuccessful) {
                tokenStorage.saveEmail(email)
                tokenStorage.saveToken(deviceId)
                reportFcmTokenIfNeeded(deviceId)
                AuthResult.Success(email, deviceId)
            } else {
                AuthResult.Error(parseError(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = authApi.login(LoginRequestDto(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                val deviceId = body?.get("deviceId") ?: deviceIdProvider.getDeviceId()
                tokenStorage.saveEmail(email)
                tokenStorage.saveToken(deviceId)
                reportFcmTokenIfNeeded(deviceId)
                AuthResult.Success(email, deviceId)
            } else {
                AuthResult.Error(parseError(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    private fun parseError(body: String?): String {
        return body?.substringAfter("\"error\":\"")?.substringBefore("\"") ?: "Error desconocido"
    }

    private suspend fun reportFcmTokenIfNeeded(deviceId: String) {
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            commandApi.updateFcmToken(deviceId, com.DevG2Studios.seguromovilultima.data.remote.dto.FcmTokenDto(token))
        } catch (e: Exception) {
            // no crítico, onNewToken lo reintentará después
        }
    }


    fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()
    fun getSavedDeviceId(): String? = tokenStorage.getToken()
    fun getSavedEmail(): String? = tokenStorage.getEmail()

    fun logout() {
        tokenStorage.clearToken()
        tokenStorage.clearEmail()
    }
}