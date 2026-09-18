package com.DevG2Studios.seguromovilultima.ui.views.codigoIncorrecto.flash




import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.DevG2Studios.seguromovilultima.services.AlarmHelper

class FlashScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenDisplay()
        setMaxBrightness()

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable {
                        AlarmHelper.stopAlarm(this)
                        finish()
                    }
            )
        }
    }

    private fun configureLockScreenDisplay() {
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

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)
    }

    private fun setMaxBrightness() {
        val params = window.attributes
        params.screenBrightness = 1f
        window.attributes = params
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmHelper.stopAlarm(this) // por si se cierra de otra forma
    }
}