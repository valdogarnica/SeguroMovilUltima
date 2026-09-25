package com.DevG2Studios.seguromovilultima.data.remote.ws

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import java.util.concurrent.TimeUnit

class StompOverWebSocketClient(
    private val client: OkHttpClient,
    private val url: String
) {
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val subscriptions = mutableMapOf<String, (String) -> Unit>()
    private var onConnectedCallback: (() -> Unit)? = null

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (isConnected) {
                // Un heartbeat STOMP es simplemente un salto de línea suelto
                webSocket?.send("\n")
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val TAG = "STOMP_CLIENT"
        private const val NULL_CHAR = '\u0000'
        private const val HEARTBEAT_INTERVAL_MS = 10_000L
    }

    fun connect(onConnected: () -> Unit) {
        onConnectedCallback = onConnected
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket abierto, enviando CONNECT")
                // Le decimos al servidor: yo te mando un heartbeat cada 10s,
                // y espero que tú también me mandes uno cada 10s.
                val connectFrame = "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n$NULL_CHAR"
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleFrame(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Fallo en WebSocket: ${t.message}")
                isConnected = false
                heartbeatHandler.removeCallbacks(heartbeatRunnable)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket cerrado: $reason")
                isConnected = false
                heartbeatHandler.removeCallbacks(heartbeatRunnable)
            }
        })
    }

    private fun handleFrame(raw: String) {
        val frame = raw.trimEnd(NULL_CHAR)
        if (frame.isBlank()) return // heartbeat entrante del servidor, ignorar

        val lines = frame.split("\n")
        val command = lines.firstOrNull() ?: return

        when (command) {
            "CONNECTED" -> {
                Log.d(TAG, "STOMP conectado")
                isConnected = true
                heartbeatHandler.post(heartbeatRunnable)
                onConnectedCallback?.invoke()
            }
            "MESSAGE" -> {
                val destination = lines.firstOrNull { it.startsWith("destination:") }
                    ?.substringAfter("destination:")
                val bodyStart = frame.indexOf("\n\n")
                val body = if (bodyStart != -1) frame.substring(bodyStart + 2) else ""
                if (destination != null) {
                    subscriptions[destination]?.invoke(body)
                }
            }
            "ERROR" -> Log.e(TAG, "STOMP ERROR: $frame")
        }
    }

    fun subscribe(destination: String, onMessage: (String) -> Unit) {
        subscriptions[destination] = onMessage
        val id = "sub-${destination.hashCode()}"
        val frame = "SUBSCRIBE\nid:$id\ndestination:$destination\n\n$NULL_CHAR"
        webSocket?.send(frame)
    }

    fun send(destination: String, jsonBody: String) {
        if (!isConnected) {
            Log.w(TAG, "send() llamado pero isConnected=false, frame descartado")
            return
        }
        val frame = "SEND\ndestination:$destination\ncontent-type:application/json\n\n$jsonBody$NULL_CHAR"
        val success = webSocket?.send(frame) ?: false
        if (!success) {
            Log.e(TAG, "webSocket.send() devolvió FALSE — el frame no se pudo encolar")
        }
    }

    fun disconnect() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        webSocket?.send("DISCONNECT\n\n$NULL_CHAR")
        webSocket?.close(1000, "Cierre normal")
        isConnected = false
    }
}