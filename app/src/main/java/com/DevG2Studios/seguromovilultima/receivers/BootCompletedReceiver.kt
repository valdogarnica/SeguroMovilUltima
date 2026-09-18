
package com.DevG2Studios.seguromovilultima.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.DevG2Studios.seguromovilultima.services.CommandPollingService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(Intent(context, CommandPollingService::class.java))
            PollingWatchdogScheduler.scheduleNext(context)
        }
    }
}