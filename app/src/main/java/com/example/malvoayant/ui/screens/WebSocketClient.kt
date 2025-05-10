package com.example.malvoayant.ui.screens

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.*

class WebSocketClient(private var url: String) {
    private val TAG = "WebSocketClient"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for reading
        .build()

    // Connection status
    private var isConnected = false
    private var reconnectJob: Job? = null

    // Message handler callback
    private var onMessageReceived: ((Float, Float) -> Unit)? = null

    // Connection status handler
    private var onConnectionStatusChanged: ((Boolean) -> Unit)? = null

    fun connect() {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection established")
                isConnected = true
                onConnectionStatusChanged?.invoke(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val x = json.getDouble("x").toFloat()
                    val y = json.getDouble("y").toFloat()

                    // Pass coordinates to handler
                    CoroutineScope(Dispatchers.Main).launch {
                        onMessageReceived?.invoke(x, y)
                        Log.d(TAG, "Received point: x=$x, y=$y")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
                isConnected = false
                onConnectionStatusChanged?.invoke(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                isConnected = false
                onConnectionStatusChanged?.invoke(false)

                // Attempt reconnection
                reconnect()
            }
        })
    }

    private fun reconnect() {
        // Avoid multiple reconnection attempts
        if (reconnectJob?.isActive == true) return

        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            // Wait a bit before reconnecting
            kotlinx.coroutines.delay(3000)
            connect()
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User requested disconnect")
        webSocket = null
        reconnectJob?.cancel()
        isConnected = false
        onConnectionStatusChanged?.invoke(false)
    }

    fun setMessageHandler(handler: (Float, Float) -> Unit) {
        onMessageReceived = handler
    }

    fun setConnectionStatusHandler(handler: (Boolean) -> Unit) {
        onConnectionStatusChanged = handler
    }

    fun setUrl(newUrl: String) {
        url = newUrl
    }
}