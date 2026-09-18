package com.DevG2Studios.seguromovilultima.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            buildEncryptedPrefs()
        } catch (e: Exception) {
            Log.e("SecureTokenStorage", "Keystore corrupto, recreando: ${e.message}")
            // Borra el archivo dañado y las claves asociadas, luego reintenta una vez
            context.deleteSharedPreferences("secure_prefs")
            try {
                buildEncryptedPrefs()
            } catch (e2: Exception) {
                Log.e("SecureTokenStorage", "Fallo también al reintentar: ${e2.message}")
                // Último recurso: SharedPreferences normal (sin cifrar) para no crashear la app.
                // El token se re-generará en el próximo login de todas formas.
                context.getSharedPreferences("secure_prefs_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
    fun clearEmail(){
        prefs.edit().remove(KEY_EMAIL).apply()
    }



    fun saveEmail(email: String) {
        prefs.edit().putString(KEY_EMAIL, email).apply()
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = getToken() != null

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_EMAIL = "user_email"
    }


}