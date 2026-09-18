package com.DevG2Studios.seguromovilultima.services.apagadoFalso

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.DevG2Studios.seguromovilultima.admin.SecurityDeviceAdminReceiver
import com.DevG2Studios.seguromovilultima.data.local.SecuritySettingsCache
import com.DevG2Studios.seguromovilultima.services.CommandExecutorService

class ShutdownMenuAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SHUTDOWN_ACCESSIBILITY"
        private val SHUTDOWN_KEYWORDS = listOf(
            "apagar", "reiniciar", "power off", "restart", "shut down"
        )
        private const val DEBOUNCE_MS = 1000L
        private const val SHADE_POLL_MS = 150L
        @Volatile
        var isFakeShutdownUiShowing: Boolean = false
    }

    @Volatile
    private var isPollingActive = false
    private var lastTriggerTime = 0L

    private val shadeDismissHandler = Handler(Looper.getMainLooper())
    private val shadeDismissRunnable = object : Runnable {
        override fun run() {
            if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
            shadeDismissHandler.postDelayed(this, SHADE_POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startPollingIfNeeded()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        startPollingIfNeeded()
    }

    private fun startPollingIfNeeded() {
        if (!isPollingActive) {
            isPollingActive = true
            Log.d(TAG, "Iniciando ciclo de cierre de panel")
            shadeDismissHandler.post(shadeDismissRunnable)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Auto-recuperación: si por cualquier motivo el ciclo no está corriendo, lo arrancamos aquí también
        startPollingIfNeeded()

        if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
            val eventPackage = event?.packageName?.toString()
            if (eventPackage == "com.android.systemui") {
                Log.d(TAG, "Panel de sistema detectado durante OFF, cerrando de inmediato")
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
        }

        if (!SecuritySettingsCache.current.fakeShutdownEnabled) return
        if (isFakeShutdownUiShowing) return

        val eventPackage = event?.packageName?.toString()
        if (eventPackage == packageName) return

        val rootNode = rootInActiveWindow ?: return
        if (rootNode.packageName?.toString() == packageName) return

        val screenText = buildString {
            collectText(rootNode, this)
        }.lowercase()

        val matches = SHUTDOWN_KEYWORDS.count { screenText.contains(it) }
        if (matches >= 2) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTime < DEBOUNCE_MS) {
                Log.d(TAG, "Evento duplicado ignorado (debounce)")
                return
            }
            lastTriggerTime = now

            Log.d(TAG, "Menú de apagado detectado, interceptando")
            performGlobalAction(GLOBAL_ACTION_BACK)
            launchFakeShutdown()
        }
    }

    private fun collectText(node: android.view.accessibility.AccessibilityNodeInfo, builder: StringBuilder) {
        node.text?.let { builder.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, builder) }
        }
    }

    private fun launchFakeShutdown() {
        forceLockScreen()
        val settings = SecuritySettingsCache.current

        val flashIntent = Intent(this, FakeShutdownActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(flashIntent)

        if (settings.fakeShutdownSendPhoto) {
            startForegroundService(Intent(this, CommandExecutorService::class.java).apply {
                putExtra(CommandExecutorService.EXTRA_COMMAND, CommandExecutorService.COMMAND_PHOTO)
            })
        }
        if (settings.fakeShutdownSendVideo) {
            startForegroundService(Intent(this, CommandExecutorService::class.java).apply {
                putExtra(CommandExecutorService.EXTRA_COMMAND, CommandExecutorService.COMMAND_VIDEO)
            })
        }
        if (settings.fakeShutdownSendLocation) {
            startForegroundService(Intent(this, CommandExecutorService::class.java).apply {
                putExtra(CommandExecutorService.EXTRA_COMMAND, CommandExecutorService.COMMAND_LOCATE)
            })
        }
    }

    private fun forceLockScreen() {
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(
            this,
            SecurityDeviceAdminReceiver::class.java
        )
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            try {
                devicePolicyManager.lockNow()
                Log.d(TAG, "lockNow() ejecutado — pantalla bloqueada forzosamente")
            } catch (e: SecurityException) {
                Log.e(TAG, "No se pudo forzar el bloqueo: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPollingActive = false
        shadeDismissHandler.removeCallbacks(shadeDismissRunnable)
    }

    override fun onInterrupt() {}
}