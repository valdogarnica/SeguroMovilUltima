package com.devg2studios.seguromovilultima.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.devg2studios.seguromovilultima.services.CommandPollingService
import com.devg2studios.seguromovilultima.utils.ServiceUtils

class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //if (ProtectionManager.isEnabled(context)) {
            val isRunning = ServiceUtils.isServiceRunning(context, CommandPollingService::class.java)
            if (!isRunning) {
                Log.d("WATCHDOG", "Servicio caído, reiniciando")
                runCatching {
                    context.startForegroundService(Intent(context, CommandPollingService::class.java))
                }.onFailure { Log.w("WATCHDOG", "No se pudo arrancar el servicio", it) }
                //ProtectionManager.start(context)
            }
            PollingWatchdogScheduler.scheduleNext(context)
        //}
    }
}