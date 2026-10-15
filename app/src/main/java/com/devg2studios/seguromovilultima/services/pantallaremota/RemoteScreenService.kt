package com.devg2studios.seguromovilultima.services.pantallaremota


import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.devg2studios.seguromovilultima.R
import com.devg2studios.seguromovilultima.data.local.SecureTokenStorage
import com.devg2studios.seguromovilultima.data.remote.ws.*
import com.devg2studios.seguromovilultima.services.apagadofalso.ShutdownMenuAccessibilityService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import androidx.core.graphics.scale
import com.devg2studios.seguromovilultima.di.BaseUrl
import com.devg2studios.seguromovilultima.services.notification.PersistentNotificationManager
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class RemoteScreenService : Service() {

    @Inject
    lateinit var tokenStorage: SecureTokenStorage

    @Inject
    @BaseUrl
    lateinit var baseUrl: String

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var captureJob: Job? = null
    private var stompClient: StompOverWebSocketClient? = null
    private val json = Json { ignoreUnknownKeys = true }



    companion object {
        private const val TAG = "REMOTE_SCREEN"
        private const val CHANNEL_ID = "remote_screen_channel"
        private const val NOTIFICATION_ID = 4001
        private const val FRAME_INTERVAL_MS = 400L
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"

    }
    private fun buildWsUrl(): String {
        // https:// -> wss://   |   http:// -> ws://
        val wsBase = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/')
        return "$wsBase/ws"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startStreaming()
            ACTION_STOP -> stopStreaming()
        }
        return START_NOT_STICKY
    }

    private fun startStreaming() {
        //startForeground(NOTIFICATION_ID, buildNotification())
        startForeground(PersistentNotificationManager.NOTIFICATION_ID, PersistentNotificationManager.build(this))
        val deviceId = tokenStorage.getToken() ?: return

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // sin timeout, es un socket persistente
            .build()

        stompClient = StompOverWebSocketClient(client, buildWsUrl()).apply {
            connect {
                Log.d(TAG, "Conectado, suscribiendo canales")
                subscribe("/topic/touch/$deviceId") { body ->
                    handleTouchMessage(body)
                }
                subscribe("/topic/remote-toggle/$deviceId") { body ->
                    handleToggleMessage(body)
                }
                startCaptureLoop(deviceId)
            }
        }
    }

    private fun startCaptureLoop(deviceId: String) {
        captureJob?.cancel()
        captureJob = scope.launch {
            while (isActive) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bitmap = captureFrame()
                    if (bitmap != null) {
                        sendFrame(deviceId, bitmap)
                        bitmap.recycle()
                    } else {
                        Log.w(TAG, "Frame nulo, no se envió (revisar log de captureFrame)")
                    }
                } else {
                    Log.w(TAG, "Android < 11, captura de pantalla no soportada")
                }
                delay(FRAME_INTERVAL_MS.milliseconds)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun captureFrame(): Bitmap? = suspendCancellableCoroutine { cont ->
        val service = ShutdownMenuAccessibilityService.instance
        if (service == null) {
            Log.e(TAG, "ShutdownMenuAccessibilityService.instance es NULL — el servicio de accesibilidad no está conectado")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        service.captureScreenshotBitmap { bitmap, error ->
            if (error != null) Log.e(TAG, "Error capturando frame: $error")
            if (bitmap == null) Log.e(TAG, "captureScreenshotBitmap devolvió bitmap null sin mensaje de error")
            cont.resume(bitmap)
        }
    }

    private fun sendFrame(deviceId: String, bitmap: Bitmap) {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scale = 400f / bitmap.width
        val scaled = bitmap.scale(400, (bitmap.height * scale).toInt())
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 45, output)
        scaled.recycle()

        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        val frame = ScreenFrameDto(deviceId, base64, System.currentTimeMillis(), originalWidth, originalHeight)
        val jsonBody = json.encodeToString(ScreenFrameDto.serializer(), frame)
        Log.d(TAG, "Enviando frame: ${jsonBody.length} bytes a /app/screen/$deviceId/frame")
        stompClient?.send("/app/screen/$deviceId/frame", jsonBody)
    }

    private fun handleTouchMessage(body: String) {
        try {
            val touch = json.decodeFromString(TouchCommandDto.serializer(), body)
            val service = ShutdownMenuAccessibilityService.instance ?: return
            when (touch.action) {
                "TAP" -> service.performTap(touch.x, touch.y)
                "SWIPE" -> service.performSwipe(touch.x, touch.y, touch.endX, touch.endY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando toque: ${e.message}")
        }
    }

    private fun handleToggleMessage(body: String) {
        try {
            val toggle = json.decodeFromString(RemoteControlToggleDto.serializer(), body)
            ShutdownMenuAccessibilityService.instance?.isRemoteTouchEnabled = toggle.touchEnabled
            Log.d(TAG, "Control de toques remoto: ${toggle.touchEnabled}")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando toggle: ${e.message}")
        }
    }

    private fun stopStreaming() {
        captureJob?.cancel()
        stompClient?.disconnect()
        ShutdownMenuAccessibilityService.instance?.isRemoteTouchEnabled = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Pantalla remota", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText("Transmitiendo pantalla")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureJob?.cancel()
        stompClient?.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}