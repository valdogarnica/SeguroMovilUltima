package com.DevG2Studios.seguromovilultima

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.DevG2Studios.seguromovilultima.receivers.PollingWatchdogScheduler
import com.DevG2Studios.seguromovilultima.services.CommandPollingService
import com.DevG2Studios.seguromovilultima.ui.navigation.AppNavHost
import com.DevG2Studios.seguromovilultima.ui.theme.SeguroMovilUltimaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // TEMPORAL: arranca el polling de comandos para pruebas.
        // Cuando tengamos login real conectando Android con el backend,
        // esto se moverá a un lugar más apropiado (tras autenticación).
        startForegroundService(Intent(this, CommandPollingService::class.java))
        PollingWatchdogScheduler.scheduleNext(this) // es para sacar del modo doze profundo
        // setExactAndAllowWhileIdle es clave: es uno de los pocos mecanismos que Android respeta incluso en Doze Mode profundo,
        // diseñado exactamente para casos como este.
        setContent {
            SeguroMovilUltimaTheme {
                AppNavHost()
            }
        }
    }
}