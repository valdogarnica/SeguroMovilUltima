package com.DevG2Studios.seguromovilultima.services.apagadoFalso

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class FakeShutdownActivity : ComponentActivity() {

    companion object {
        @Volatile
        var instance: FakeShutdownActivity? = null
    }

    private var volumeUpPressStart: Long = 0L
    private val exitHandler = Handler(Looper.getMainLooper())
    private var exitRunnable: Runnable? = null

    private val hideBarsHandler = Handler(Looper.getMainLooper())
    private val hideBarsRunnable = object : Runnable {
        override fun run() {
            hideSystemBars()
            hideBarsHandler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        Log.d("FAKE_SHUTDOWN_LIFECYCLE", "onCreate — stage actual: ${FakeShutdownStageHolder.current}")
        configureOverLockScreen()
        hideSystemBars()
        enforceImmersiveMode()
        hideBarsHandler.post(hideBarsRunnable)
        ShutdownMenuAccessibilityService.isFakeShutdownUiShowing = true

        setContent {
            var stage by remember { mutableStateOf(FakeShutdownStageHolder.current) }

            LaunchedEffect(stage) {
                FakeShutdownStageHolder.current = stage
                if (stage == ShutdownStage.POWERING_OFF) {
                    VibrationHelper.vibrateShutdownPulse(this@FakeShutdownActivity)
                    delay(2500)
                    applyOffState()
                    stage = ShutdownStage.OFF
                }
            }

            when (stage) {
                ShutdownStage.MENU -> FakePowerMenu(
                    onShutdownClick = { stage = ShutdownStage.POWERING_OFF }
                )
                ShutdownStage.POWERING_OFF -> PoweringOffScreen()
                ShutdownStage.OFF -> OffScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
            applyOffState()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("FAKE_SHUTDOWN_LIFECYCLE", "onStop — isFinishing=$isFinishing, stage=${FakeShutdownStageHolder.current}")
        if (isFinishing) return

        if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
            val relaunch = Intent(this, FakeShutdownActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(relaunch)
        } else {
            ShutdownMenuAccessibilityService.isFakeShutdownUiShowing = false
            FakeShutdownStageHolder.current = ShutdownStage.MENU
        }
    }

    private fun applyOffState() {
        setBrightnessToZero()
        enableDoNotDisturb()
        ShutdownMenuAccessibilityService.instance?.showBlackOverlay() // NUEVO
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun enforceImmersiveMode() {
        val decorView = window.decorView

        // Capa: reacciona a cambios de insets
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            val isStatusBarVisible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
            val isNavBarVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())
            if (isStatusBarVisible || isNavBarVisible) {
                hideSystemBars()
            }
            insets
        }

        // Capa: reacciona a cada redibujado de layout, más granular que los insets
        decorView.viewTreeObserver.addOnGlobalLayoutListener {
            if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
                hideSystemBars()
            }
        }
    }

    private fun configureOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // NO llamamos a keyguardManager.requestDismissKeyguard(): queremos que
        // el keyguard siga bloqueado de verdad, solo mostramos el overlay encima.
    }

    private fun setBrightnessToZero() {
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
        }
        val params = window.attributes
        params.screenBrightness = 0.01f
        window.attributes = params
    }

    private fun restoreNormalBrightness() {
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }

    private fun enableDoNotDisturb() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    private fun restoreNotifications() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    fun closeFakeShutdown() {
        ShutdownMenuAccessibilityService.instance?.hideBlackOverlay() // NUEVO
        restoreNotifications()
        restoreNormalBrightness()
        ShutdownMenuAccessibilityService.isFakeShutdownUiShowing = false
        FakeShutdownStageHolder.current = ShutdownStage.MENU
        finish()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (volumeUpPressStart == 0L) {
                                volumeUpPressStart = System.currentTimeMillis()
                                exitRunnable = Runnable { closeFakeShutdown() }
                                exitHandler.postDelayed(exitRunnable!!, 3000)
                            }
                        }
                        KeyEvent.ACTION_UP -> {
                            volumeUpPressStart = 0L
                            exitRunnable?.let { exitHandler.removeCallbacks(it) }
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // NUEVO: intenta consumir el toque en el borde superior antes de que
    // el sistema inicie el gesto de swipe del status bar.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (FakeShutdownStageHolder.current == ShutdownStage.OFF) {
            if (ev.action == MotionEvent.ACTION_DOWN && ev.rawY < 30) {
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FAKE_SHUTDOWN_LIFECYCLE", "onDestroy — reseteando bandera")
        exitRunnable?.let { exitHandler.removeCallbacks(it) }
        hideBarsHandler.removeCallbacks(hideBarsRunnable)
        ShutdownMenuAccessibilityService.isFakeShutdownUiShowing = false
        ShutdownMenuAccessibilityService.instance?.hideBlackOverlay()
        if (instance == this) instance = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }
}

@Composable
private fun FakePowerMenu(onShutdownClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onShutdownClick)
                .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .padding(horizontal = 32.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Apagar", color = Color.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun PoweringOffScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Apagando...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun OffScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}