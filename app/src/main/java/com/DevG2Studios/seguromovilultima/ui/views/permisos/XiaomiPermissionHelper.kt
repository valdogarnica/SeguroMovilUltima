
package com.DevG2Studios.seguromovilultima.ui.views.permisos

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.content.edit

object XiaomiPermissionHelper {

    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco")
    }

    /**
     * Intenta abrir la pantalla de Autostart de MIUI/HyperOS.
     * No hay API pública: se prueban varias activities conocidas
     * porque cambian entre versiones de MIUI/HyperOS.
     */
    fun openAutoStartSettings(context: Context) {
        val intents = listOf(
            Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            Intent("miui.intent.action.OP_AUTO_START").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
            }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                continue
            }
        }

        // Ningún intent específico funcionó: fallback a ajustes de la app
        Toast.makeText(
            context,
            "Abre Ajustes de la app y busca 'Inicio automático'",
            Toast.LENGTH_LONG
        ).show()
        openAppDetailsSettings(context)
    }

    /**
     * Abre la pantalla de optimización de batería del sistema (AOSP estándar).
     * En Xiaomi esta pantalla suele redirigir al selector "Sin restricciones /
     * Ahorro de batería / Restringido" propio de MIUI.
     */
    fun requestIgnoreBatteryOptimizations(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    /**
     * Fallback genérico: ajustes de la app, para autostart o
     * "Permitir ajustes restringidos" cuando el intent directo falla.
     */
    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
        context.startActivity(intent)
    }

    // En XiaomiPermissionHelper
    fun isAutoStartEnabled(context: Context): Boolean? {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(
                appOps, 10008, android.os.Process.myUid(), context.packageName
            ) as Int
            result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            null // no se puede determinar
        }
    }

    private const val PREFS = "xiaomi_prefs"
    private const val KEY_AUTOSTART_CONFIRMED = "autostart_confirmed"

    fun isAutoStartConfirmed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTOSTART_CONFIRMED, false)

    fun setAutoStartConfirmed(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_AUTOSTART_CONFIRMED, value) }
    }

    // Dentro de object XiaomiPermissionHelper, agrega:

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(context.packageName)
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    // Agrega a XiaomiPermissionHelper
    fun openMiuiOptimizationSettings(context: Context) {
        val intents = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.powercenter.PowerSettings"
                )
            },
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) // fallback: dev options, donde vive el toggle en HyperOS
        )
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) { continue }
        }
        Toast.makeText(context, "Ve a Ajustes → Opciones de desarrollador → Desactivar optimización de MIUI", Toast.LENGTH_LONG).show()
    }
    fun openXiaomiAppBatterySettings(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager))
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Ve a Ajustes → Batería → Uso de batería por app → tu app → Sin restricciones", Toast.LENGTH_LONG).show()
            XiaomiPermissionHelper.openAppDetailsSettings(context)
        }
    }

}