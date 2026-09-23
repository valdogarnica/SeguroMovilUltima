package com.DevG2Studios.seguromovilultima.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.DevG2Studios.seguromovilultima.services.CommandPollingService
import com.DevG2Studios.seguromovilultima.services.LocationForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BOOT_RECEIVER", "Reiniciando polling tras arranque del sistema")
            context.startForegroundService(Intent(context, CommandPollingService::class.java))

            val prefs = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
            if (prefs.getBoolean("protection_enabled", false)) {
                Log.d("BOOT_RECEIVER", "Reiniciando ubicación (el usuario la había activado)")
                context.startForegroundService(Intent(context, LocationForegroundService::class.java))
            }
        }
    }
}