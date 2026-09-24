
package com.DevG2Studios.seguromovilultima.utils

import android.content.Context
import android.content.Intent
import com.DevG2Studios.seguromovilultima.services.CommandPollingService
import com.DevG2Studios.seguromovilultima.services.LocationForegroundService
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object ProtectionManager {
    private const val PREFS_NAME = "app_state"
    private const val KEY_PROTECTION_ENABLED = "protection_enabled"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PROTECTION_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
    }
    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(context: Context) {
        if (!hasLocationPermission(context)) {
            // No arrancamos nada: sin permiso, el foreground service de ubicación
            // haría crashear la app (SecurityException). Solo desactivamos el flag
            // para que la UI no siga mostrando el switch como "debería estar activo".
            setEnabled(context, false)
            return
        }
        context.startForegroundService(Intent(context, LocationForegroundService::class.java))
        context.startForegroundService(Intent(context, CommandPollingService::class.java))
        setEnabled(context, true)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, LocationForegroundService::class.java))
        context.stopService(Intent(context, CommandPollingService::class.java))
        setEnabled(context, false)
    }

    /** Verdad real: ambos servicios corriendo, no solo el flag guardado. */
    fun isActuallyRunning(context: Context): Boolean {
        return ServiceUtils.isServiceRunning(context, LocationForegroundService::class.java) &&
                ServiceUtils.isServiceRunning(context, CommandPollingService::class.java)
    }
}