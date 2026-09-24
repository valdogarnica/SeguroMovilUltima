package com.DevG2Studios.seguromovilultima.services.apagadoFalso

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.DevG2Studios.seguromovilultima.admin.SecurityDeviceAdminReceiver
import com.DevG2Studios.seguromovilultima.data.local.SecuritySettingsCache
import com.DevG2Studios.seguromovilultima.services.CommandExecutorService

@SuppressLint("AccessibilityPolicy")
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

        // Referencia estática al servicio conectado, para que la Activity
        // pueda pedirle que muestre/oculte el overlay real de accesibilidad.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        var instance: ShutdownMenuAccessibilityService? = null
    }

    @Volatile
    private var isPollingActive = false
    private var lastTriggerTime = 0L

    // --- NUEVO: overlay negro real, dibujado por el propio servicio de accesibilidad ---
    private var overlayView: View? = null
    private var overlayWindowManager: WindowManager? = null

    private val shadeDismissHandler = Handler(Looper.getMainLooper())




    private var volumeUpPressStart: Long = 0L
    private val failsafeExitHandler = Handler(Looper.getMainLooper())
    private var failsafeExitRunnable: Runnable? = null
    private val shadeDismissRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.S)
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
        instance = this
        startPollingIfNeeded()
    }

    private fun startPollingIfNeeded() {
        if (!isPollingActive) {
            isPollingActive = true
            Log.d(TAG, "Iniciando ciclo de cierre de panel")
            shadeDismissHandler.post(shadeDismissRunnable)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
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
    override fun onKeyEvent(event: android.view.KeyEvent): Boolean {
        if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
            when (event.keyCode) {
                android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                    when (event.action) {
                        android.view.KeyEvent.ACTION_DOWN -> {
                            if (volumeUpPressStart == 0L) {
                                volumeUpPressStart = System.currentTimeMillis()
                                failsafeExitRunnable = Runnable {
                                    Log.w(TAG, "Salida de emergencia activada desde el servicio de accesibilidad")
                                    hideBlackOverlay()
                                    FakeShutdownStageHolder.current = ShutdownStage.MENU
                                    isFakeShutdownUiShowing = false
                                }
                                failsafeExitHandler.postDelayed(failsafeExitRunnable!!, 3000)
                            }
                        }
                        android.view.KeyEvent.ACTION_UP -> {
                            volumeUpPressStart = 0L
                            failsafeExitRunnable?.let { failsafeExitHandler.removeCallbacks(it) }
                        }
                    }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
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

    // --- NUEVO: overlay negro real por encima del status bar ---

    @Suppress("DEPRECATION")
    fun showBlackOverlay() {
        if (overlayView != null) return

        overlayWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics
        val view = View(this).apply {
            setBackgroundColor(Color.BLACK)
            // Igual que en la técnica de referencia: fuerza modo inmersivo
            // también sobre la propia ventana del overlay, no solo la Activity.
            systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE
                    )
        }

        // Clave del fix: MATCH_PARENT NO incluye la franja del status bar.
        // Sobredimensionamos la ventana (más grande que la pantalla) y la
        // desplazamos en negativo para que físicamente se extienda por
        // encima de esa franja, no solo por debajo de ella.
        val pixelOffset = 400
        val rectDim = maxOf(dm.widthPixels, dm.heightPixels)

        val params = WindowManager.LayoutParams(
            rectDim + 2 * pixelOffset,
            rectDim + 2 * pixelOffset,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = -pixelOffset
            y = -pixelOffset
            // NUEVO: fuerza el mismo brillo mínimo directamente en esta ventana,
            // que ahora es la que queda por encima de la Activity.
            screenBrightness = 0.01f
            buttonBrightness = 0f
        }

        try {
            overlayWindowManager?.addView(view, params)
            overlayView = view
            Log.d(TAG, "Overlay negro de accesibilidad agregado (sobredimensionado)")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo agregar el overlay de accesibilidad: ${e.message}")
            overlayView = null
        }
    }

    fun hideBlackOverlay() {
        try {
            overlayView?.let { overlayWindowManager?.removeView(it) }
            Log.d(TAG, "Overlay negro de accesibilidad removido")
        } catch (e: Exception) {
            Log.e(TAG, "Error al remover el overlay: ${e.message}")
        } finally {
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPollingActive = false
        shadeDismissHandler.removeCallbacks(shadeDismissRunnable)
        hideBlackOverlay() // red de seguridad: nunca dejar una ventana negra huérfana
        instance = null
    }

    override fun onInterrupt() {
        hideBlackOverlay() // misma red de seguridad si el sistema interrumpe el servicio
    }
}