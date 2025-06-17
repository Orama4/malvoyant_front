package com.example.malvoayant.network

import android.util.Log
import com.example.malvoayant.data.api.RetrofitClient
import com.example.malvoayant.data.models.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
/*
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
*/

class LocationWebSocketService {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(70, TimeUnit.SECONDS) // Augmenté pour correspondre au timeout serveur
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS) // Ping automatique toutes les 25 secondes
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Variables pour le système de heartbeat
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var userId: Int = 0
    private var helperId: Int = 0
    private var isIntentionalDisconnect = false
    private var lastHeartbeatResponse = System.currentTimeMillis()

    // Configuration du heartbeat
    private val HEARTBEAT_INTERVAL = 25000L // 25 secondes
    private val HEARTBEAT_TIMEOUT = 60000L // 60 secondes
    private val RECONNECT_DELAY = 5000L // 5 secondes

    fun connect(userId: Int, helperId: Int) {
        this.userId = userId
        this.helperId = helperId
        isIntentionalDisconnect = false

        disconnect() // Fermer la connexion existante

        val url = "ws://${RetrofitClient.IP_ADR}:3002/ws?role=enduser&userId=$userId&helperId=$helperId"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connexion établie")
                lastHeartbeatResponse = System.currentTimeMillis()

                _connectionState.value = _connectionState.value.copy(
                    isConnected = true,
                    error = null,
                    lastConnectionTime = System.currentTimeMillis()
                )

                // Démarrer le système de heartbeat
                startHeartbeat()

                // Annuler toute tentative de reconnexion en cours
                reconnectJob?.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Message reçu: $text")

                // Mettre à jour le timestamp de la dernière réponse
                lastHeartbeatResponse = System.currentTimeMillis()

                try {
                    val json = JSONObject(text)
                    val type = json.getString("type")

                    when (type) {
                        "location_confirmed" -> {
                            Log.d("WebSocket", "Position confirmée par le serveur")
                            _connectionState.value = _connectionState.value.copy(
                                lastConfirmationTime = System.currentTimeMillis()
                            )
                        }
                        "heartbeat_response" -> {
                            Log.d("WebSocket", "Heartbeat confirmé par le serveur")
                            _connectionState.value = _connectionState.value.copy(
                                lastHeartbeatTime = System.currentTimeMillis()
                            )
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
                Log.d("WebSocket", "Connexion fermée: $reason (code: $code)")

                stopHeartbeat()

                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    error = if (code != 1000) reason else null
                )

                // Tenter une reconnexion automatique si ce n'est pas intentionnel
                if (!isIntentionalDisconnect && code != 1000) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Erreur connexion: ${t.message}")

                stopHeartbeat()

                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    error = t.message
                )

                // Tenter une reconnexion automatique
                if (!isIntentionalDisconnect) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun startHeartbeat() {
        stopHeartbeat() // Arrêter le heartbeat existant

        heartbeatJob = coroutineScope.launch {
            while (isActive && !isIntentionalDisconnect) {
                delay(HEARTBEAT_INTERVAL)

                // Vérifier si la connexion est toujours vivante
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastHeartbeatResponse > HEARTBEAT_TIMEOUT) {
                    Log.w("WebSocket", "Heartbeat timeout détecté, reconnexion...")
                    webSocket?.close(1001, "Heartbeat timeout")
                    break
                }

                // Envoyer un heartbeat
                sendHeartbeat()
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun sendHeartbeat() {
        webSocket?.let { ws ->
            if (_connectionState.value.isConnected) {
                val heartbeatData = JSONObject().apply {
                    put("type", "heartbeat")
                    put("timestamp", System.currentTimeMillis())
                }

                val success = ws.send(heartbeatData.toString())
                if (success) {
                    Log.d("WebSocket", "Heartbeat envoyé")
                } else {
                    Log.w("WebSocket", "Échec envoi heartbeat")
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (isIntentionalDisconnect) return

        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            var retryCount = 0
            val maxRetries = 5

            while (retryCount < maxRetries && !isIntentionalDisconnect) {
                delay(RECONNECT_DELAY * (retryCount + 1)) // Délai croissant

                Log.d("WebSocket", "Tentative de reconnexion ${retryCount + 1}/$maxRetries")

                _connectionState.value = _connectionState.value.copy(
                    error = "Reconnexion en cours... (${retryCount + 1}/$maxRetries)"
                )

                try {
                    connect(userId, helperId)
                    break // Sortir de la boucle si la connexion réussit
                } catch (e: Exception) {
                    Log.e("WebSocket", "Échec reconnexion: ${e.message}")
                    retryCount++
                }
            }

            if (retryCount >= maxRetries) {
                _connectionState.value = _connectionState.value.copy(
                    error = "Impossible de se reconnecter après $maxRetries tentatives"
                )
            }
        }
    }

    fun sendLocationUpdate(position: Pair<Float, Float>) {
        webSocket?.let { ws ->
            if (_connectionState.value.isConnected) {
                val locationData = JSONObject().apply {
                    put("type", "location_update")
                    put("lat", position.first.toDouble())
                    put("lng", position.second.toDouble())
                    put("timestamp", System.currentTimeMillis())
                }

                val success = ws.send(locationData.toString())
                if (success) {
                    lastHeartbeatResponse = System.currentTimeMillis() // Mise à jour du timestamp
                    _connectionState.value = _connectionState.value.copy(
                        lastLocationSent = position,
                        lastLocationTime = System.currentTimeMillis()
                    )
                    Log.d("WebSocket", "Position envoyée: ${position.first}, ${position.second}")
                } else {
                    Log.e("WebSocket", "Échec envoi position")
                }
            } else {
                Log.w("WebSocket", "Tentative d'envoi de position sans connexion")
                // Tenter une reconnexion si pas déjà en cours
                if(!isIntentionalDisconnect && reconnectJob?.isActive != true) {
                    scheduleReconnect()
                } else {

                }
            }
        }
    }

    // Nouvelle méthode pour forcer une reconnexion manuelle
    fun forceReconnect() {
        if (userId != 0 && helperId != 0) {
            Log.d("WebSocket", "Reconnexion forcée")
            connect(userId, helperId)
        }
    }

    // Méthode pour vérifier l'état de la connexion
    fun isConnectionHealthy(): Boolean {
        val currentTime = System.currentTimeMillis()
        return _connectionState.value.isConnected &&
                (currentTime - lastHeartbeatResponse) < HEARTBEAT_TIMEOUT
    }

    fun disconnect() {
        isIntentionalDisconnect = true

        // Arrêter tous les jobs
        stopHeartbeat()
        reconnectJob?.cancel()

        webSocket?.close(1000, "Déconnexion normale")
        webSocket = null

        _connectionState.value = _connectionState.value.copy(
            isConnected = false,
            error = null
        )
    }

    fun cleanup() {
        disconnect()
        coroutineScope.cancel()
    }
}