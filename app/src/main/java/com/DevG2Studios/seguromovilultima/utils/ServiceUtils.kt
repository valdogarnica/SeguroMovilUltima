package com.DevG2Studios.seguromovilultima.utils

import android.app.ActivityManager
import android.content.Context

object ServiceUtils {
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            it.service.className == serviceClass.name
        }
    }
}