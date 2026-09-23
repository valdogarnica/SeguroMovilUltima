package com.DevG2Studios.seguromovilultima.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.DevG2Studios.seguromovilultima.services.CommandPollingService
import com.DevG2Studios.seguromovilultima.utils.ServiceUtils

class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //if (ProtectionManager.isEnabled(context)) {
            val isRunning = ServiceUtils.isServiceRunning(context, CommandPollingService::class.java)
            if (!isRunning) {
                Log.d("WATCHDOG", "Servicio caído, reiniciando")
                context.startForegroundService(Intent(context, CommandPollingService::class.java))
                //ProtectionManager.start(context)
            }
            PollingWatchdogScheduler.scheduleNext(context)
        //}
    }
}