package com.DevG2Studios.seguromovilultima.data.remote.service



import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.DevG2Studios.seguromovilultima.services.CommandExecutorService

class CameraTriggerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val command = intent.getStringExtra(CommandExecutorService.EXTRA_COMMAND)
        val commandId = intent.getLongExtra(CommandExecutorService.EXTRA_COMMAND_ID, -1L)

        // Ahora que esta Activity está en primer plano, el sistema
        // permite iniciar el foreground service tipo camera.
        val serviceIntent = Intent(this, CommandExecutorService::class.java).apply {
            putExtra(CommandExecutorService.EXTRA_COMMAND, command)
            putExtra(CommandExecutorService.EXTRA_COMMAND_ID, commandId)
        }
        startForegroundService(serviceIntent)

        // Nos cerramos de inmediato: ya cumplimos nuestro propósito
        // (darle el estado "en primer plano" al proceso para esta transacción).
        finish()
    }
}