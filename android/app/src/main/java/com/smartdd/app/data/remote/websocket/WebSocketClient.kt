package com.smartdd.app.data.remote.websocket

import android.util.Log
import com.google.gson.Gson
import com.smartdd.app.data.local.preferences.TokenManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var ws: WebSocket? = null
    private val gson = Gson()
    private val _events = Channel<WSMessage>(Channel.BUFFERED)
    val events: Flow<WSMessage> = _events.receiveAsFlow()
    private val reconnectDelay = 3000L
    private var shouldReconnect = true

    fun connect() {
        val token = tokenManager.getAccessToken() ?: return
        val request = Request.Builder()
            .url("ws://192.168.100.101:8000/ws")
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("WS", "Conectado")
                ws.send(gson.toJson(mapOf("type" to "auth", "token" to token)))
            }
            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, WSMessage::class.java)
                    _events.trySend(msg)
                } catch (e: Exception) {
                    Log.e("WS", "Error parsing: $text", e)
                }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "Error: ${t.message}")
                if (shouldReconnect) reconnect()
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (shouldReconnect) reconnect()
            }
        })
    }

    private fun reconnect() {
        Thread.sleep(reconnectDelay)
        connect()
    }

    fun send(message: Map<String, Any>) {
        ws?.send(gson.toJson(message))
    }

    fun disconnect() {
        shouldReconnect = false
        ws?.close(1000, "Cliente desconectado")
    }
}

data class WSMessage(
    val type: String? = null,
    val sessionId: String? = null,
    val roomId: String? = null,
    val emisorName: String? = null,
    val previewVideo: Boolean? = null,
    val action: String? = null,
    val mode: String? = null,
    val message: String? = null,
    val from: String? = null,
    val userId: String? = null
)
