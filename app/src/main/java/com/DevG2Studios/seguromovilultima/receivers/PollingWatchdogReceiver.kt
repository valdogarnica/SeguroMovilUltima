
package com.DevG2Studios.seguromovilultima.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.DevG2Studios.seguromovilultima.services.CommandPollingService

class PollingWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("POLLING_WATCHDOG", "Verificando/reiniciando CommandPollingService")
        context.startForegroundService(Intent(context, CommandPollingService::class.java))
        PollingWatchdogScheduler.scheduleNext(context) // reprograma la siguiente verificación
    }
}