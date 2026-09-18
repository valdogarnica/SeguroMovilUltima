package com.DevG2Studios.seguromovilultima.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

object PollingWatchdogScheduler {

    private const val TAG = "POLLING_WATCHDOG"
    private const val INTERVAL_MS = 60_000L

    fun scheduleNext(context: Context) {
        val intent = Intent(context, PollingWatchdogReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "No se puede programar alarma exacta: falta el permiso SCHEDULE_EXACT_ALARM")
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL_MS,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException al programar alarma: ${e.message}")
        }
    }
}