package com.example.malvoayant.network

import android.util.Log
import com.example.malvoayant.data.models.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LocationWebSocketService {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(userId: Int, helperId: Int) {
        disconnect() // Fermer la connexion existante

        val url = "ws://172.20.10.3:3002/ws?role=enduser&userId=$userId&helperId=$helperId"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connexion établie")
                _connectionState.value = _connectionState.value.copy(
                    isConnected = true,
                    error = null
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Message reçu: $text")
                try {
                    val json = JSONObject(text)
                    val type = json.getString("type")

                    when (type) {
                        "location_confirmed" -> {
                            Log.d("WebSocket", "Position confirmée par le serveur")
                        }
                        "error" -> {
                            val errorMsg = json.getString("message")
                            _connectionState.value = _connectionState.value.copy(error = errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Erreur parsing message: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Connexion en cours de fermeture: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Connexion fermée: $reason")
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    error = if (code != 1000) reason else null
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Erreur connexion: ${t.message}")
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    error = t.message
                )
            }
        })
    }

    fun sendLocationUpdate(position: Pair<Float, Float>) {
        webSocket?.let { ws ->
            if (_connectionState.value.isConnected) {
                val locationData = JSONObject().apply {
                    put("type", "location_update")
                    put("lat", position.first.toDouble())
                    put("lng", position.second.toDouble())
                }

                val success = ws.send(locationData.toString())
                if (success) {
                    _connectionState.value = _connectionState.value.copy(
                        lastLocationSent = position
                    )
                    Log.d("WebSocket", "Position envoyée: ${position.first}, ${position.second}")
                } else {
                    Log.e("WebSocket", "Échec envoi position")
                }
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Déconnexion normale")
        webSocket = null
        _connectionState.value = _connectionState.value.copy(isConnected = false)
    }

    fun cleanup() {
        disconnect()
        coroutineScope.cancel()
    }
}
