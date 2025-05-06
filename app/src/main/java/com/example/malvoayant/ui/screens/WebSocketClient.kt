package com.example.malvoayant.ui.screens


import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.*

class WebSocketClient(private val url: String) {
    private val TAG = "WebSocketClient"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for reading
        .build()

    // Store path points
    val pathPoints = mutableStateListOf<Offset>()

    // Connection status
    private var isConnected = false
    private var reconnectJob: Job? = null

    fun connect() {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection established")
                isConnected = true
                // Reset path on new connection
                pathPoints.clear()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val x = json.getDouble("x").toFloat()
                    val y = json.getDouble("y").toFloat()

                    // Add new point to path
                    CoroutineScope(Dispatchers.Main).launch {
                        pathPoints.add(Offset(x, y))
                        Log.d(TAG, "Received point: x=$x, y=$y, total points: ${pathPoints.size}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
                isConnected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                isConnected = false

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
    }
}