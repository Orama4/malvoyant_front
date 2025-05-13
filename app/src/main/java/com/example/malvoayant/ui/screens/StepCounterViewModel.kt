package com.example.malvoayant.ui.screens

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class StepCounterViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private var isReconnecting = false

    // WebSocket properties
    private var webSocketClient: WebSocketClient? = null
    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    // WiFi position properties
    private val _wifiPositionLive = MutableLiveData<Pair<Float, Float>?>(null)
    val wifiPositionLive: LiveData<Pair<Float, Float>?> = _wifiPositionLive

    private val _wifiConfidence = MutableLiveData(0.0f)
    val wifiConfidence: LiveData<Float> = _wifiConfidence

    // For visualization - path tracking with only WiFi positions
    private val _pathPoints = MutableLiveData(listOf<Pair<Float, Float>>())
    val pathPoints: LiveData<List<Pair<Float, Float>>> = _pathPoints

    // Current position (only from WiFi)
    private val _currentPositionLive = MutableLiveData(Pair(0f, 0f))
    val currentPositionLive: LiveData<Pair<Float, Float>> = _currentPositionLive

    // Position update time tracking
    private var lastWifiUpdateTime = 0L
    private val _lastWifiUpdateAgo = MutableLiveData("Never")
    val lastWifiUpdateAgo: LiveData<String> = _lastWifiUpdateAgo

    // Connection status
    private val _connectionStatus = MutableLiveData("Disconnected")
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Step detection properties
    private val sensorManager: SensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepDetectorSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null

    private val _isMoving = MutableLiveData(false)
    val isMoving: LiveData<Boolean> = _isMoving

    private val _stepCount = MutableLiveData(0)
    val stepCount: LiveData<Int> = _stepCount

    // Movement detection properties
    private var lastStepTimestamp = 0L
    private val movementTimeoutMs = 3000L  // Consider user stationary after 3 seconds without steps
    private var movementDetectionJob: kotlinx.coroutines.Job? = null

    // Position stability properties
    private val positionBuffer = mutableListOf<Pair<Float, Float>>()
    private val positionBufferSize = 5  // Number of positions to average
    private val positionThreshold = 0.5f  // Threshold for position change (in meters)

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    private var reconnectionJob: kotlinx.coroutines.Job? = null

    // Start a timer to update the "last update" text
    private val updateTimer = CoroutineScope(Dispatchers.Default)

    init {
        // Initialize sensors
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Start timer to update "last update ago" text
        updateTimer.launch {
            while (isActive) {
                updateLastUpdateText()
                checkMovementTimeout()
                delay(1000)  // Update every second
            }
        }

        // Start movement detection monitoring
        startMovementDetection()
    }

    // WebSocket functions
    fun initWebSocket() {
        connectWebSocket()
        startReconnectionTimer()
    }

    private fun connectWebSocket() {
        if (isReconnecting) return
        isReconnecting = true

        try {
            webSocketClient?.close()
            webSocketClient = null
        } catch (e: Exception) {
            // Ignore any exceptions during cleanup
        }

        val serverUri = URI("ws://192.168.205.138:8000/ws/position") // Update with Pi's IP

        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                reconnectAttempts = 0  // Reset counter on successful connection
                _isConnected.postValue(true)
                _connectionStatus.postValue("Connected to Raspberry Pi")
                _errorMessage.postValue(null)
                requestWifiPositionUpdates()

                // Cancel reconnection timer since we're connected
                reconnectionJob?.cancel()
                reconnectionJob = null
            }

            override fun onMessage(message: String?) {
                Log.d("WebSocket", "Message received: $message")
                message?.let {
                    try {
                        val json = JSONObject(it)
                        when (json.getString("type")) {
                            "wifi_position" -> {
                                val position = json.getJSONObject("position")
                                val x = position.getDouble("x").toFloat()
                                val y = position.getDouble("y").toFloat()

                                // Optional: default confidence to 1.0 if not present
                                val confidence = if (position.has("confidence")) {
                                    position.getDouble("confidence").toFloat()
                                } else {
                                    1.0f
                                }

                                // Process incoming WiFi position
                                processWifiPosition(Pair(x, y), confidence)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WebSocket", "Error parsing message: ${e.message}", e)
                        _errorMessage.postValue("Error parsing message: ${e.message}")
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                _isConnected.postValue(false)
                _connectionStatus.postValue("Disconnected: $reason")

                // reconnect with exponential backoff
                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++
                }

                startReconnectionTimer()
            }

            override fun onError(ex: Exception?) {
                _isConnected.postValue(false)
                _errorMessage.postValue("WebSocket error: ${ex?.message}")
                // Attempt reconnect after delay
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000)
                    connectWebSocket()
                }
            }
        }

        // connection timeout and handle reconnection
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(10000)
                if (webSocketClient != null && !_isConnected.value!!) {
                    _connectionStatus.postValue("Connection timed out")
                    _errorMessage.postValue("Failed to connect to server")
                    webSocketClient?.close()
                    webSocketClient = null
                }
            } finally {
                isReconnecting = false
            }
        }

        try {
            webSocketClient?.connect()
        } catch (e: Exception) {
            _errorMessage.postValue("Connection error: ${e.message}")
            isReconnecting = false
        }
    }

    private fun requestWifiPositionUpdates(intervalSeconds: Double = 1.0) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "start_position_updates")
        jsonObject.put("interval", intervalSeconds)

        webSocketClient?.let { client ->
            if (client.isOpen) {
                client.send(jsonObject.toString())
            }
        }
    }

    fun requestSingleWifiUpdate() {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("type", "single_position_request")

            webSocketClient?.let { client ->
                if (client.isOpen) {
                    client.send(jsonObject.toString())
                }
            }
        } catch (e: Exception) {
            _errorMessage.postValue("Error sending request: ${e.message}")
        }
    }

    // Step detector implementation
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_DETECTOR -> {
                    // Each time a step is detected, this will be called
                    Log.d("PositionTracking", "You are moving")
                    _stepCount.value = (_stepCount.value ?: 0) + 1
                    lastStepTimestamp = System.currentTimeMillis()
                    _isMoving.value = true
                }
                Sensor.TYPE_STEP_COUNTER -> {
                    // This provides the total steps since device boot
                    // Usually used for step counting rather than movement detection
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun startMovementDetection() {
        movementDetectionJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                checkMovementTimeout()
                delay(1000)
            }
        }
    }

    private fun checkMovementTimeout() {
        val currentTime = System.currentTimeMillis()
        if (lastStepTimestamp > 0 && (currentTime - lastStepTimestamp) > movementTimeoutMs) {
            // No steps detected for the timeout period, user is likely not moving
            if (_isMoving.value == true) {
                _isMoving.postValue(false)
            }
        }
    }

    // Process WiFi position considering step detection
    private fun processWifiPosition(position: Pair<Float, Float>, confidence: Float) {
        // Store raw values
        _wifiPositionLive.postValue(position)
        _wifiConfidence.postValue(confidence)
        lastWifiUpdateTime = System.currentTimeMillis()

        // Only update position if user is moving or it's the first position
        if (_isMoving.value == true || _currentPositionLive.value == Pair(0f, 0f)) {
            // Apply position smoothing
            updatePositionWithSmoothing(position)

            // Add to path only if user is moving
            if (_isMoving.value == true) {
                val currentPath = _pathPoints.value ?: listOf()
                val updatedPath = currentPath + position
                _pathPoints.postValue(updatedPath)
            }
        } else {
            // User is not moving - don't update position
            Log.d("PositionTracking", "Ignoring WiFi position update because user is not moving")
        }
    }

    private fun updatePositionWithSmoothing(newPosition: Pair<Float, Float>) {
        // Add new position to buffer
        positionBuffer.add(newPosition)

        // Keep buffer at fixed size
        if (positionBuffer.size > positionBufferSize) {
            positionBuffer.removeAt(0)
        }

        // Calculate the average position from the buffer
        val avgX = positionBuffer.map { it.first }.average().toFloat()
        val avgY = positionBuffer.map { it.second }.average().toFloat()

        val smoothedPosition = Pair(avgX, avgY)

        // Update current position
        _currentPositionLive.postValue(smoothedPosition)
    }

    private fun updateLastUpdateText() {
        if (lastWifiUpdateTime == 0L) {
            _lastWifiUpdateAgo.postValue("Never")
            return
        }

        val age = System.currentTimeMillis() - lastWifiUpdateTime
        val text = when {
            age < 1000 -> "Just now"
            age < 60000 -> "${age / 1000} seconds ago"
            else -> "${age / 60000} minutes ago"
        }

        _lastWifiUpdateAgo.postValue(text)
    }

    // Public functions for controlling the system
    fun startListening() {
        // Register sensors
        stepDetectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Initialize WebSocket connection
        initWebSocket()
    }

    fun stopListening() {
        // Unregister sensors
        sensorManager.unregisterListener(this)

        // Close WebSocket connection
        webSocketClient?.close()
    }

    fun resetAll() {
        // Clear path points
        _pathPoints.value = listOf()

        // Reset step counter
        _stepCount.value = 0

        // Clear position buffer
        positionBuffer.clear()

        // Keep the last WiFi position but reset the path
        val lastWifiPos = _wifiPositionLive.value ?: Pair(0f, 0f)
        _currentPositionLive.value = lastWifiPos
    }

    override fun onCleared() {
        super.onCleared()
        reconnectionJob?.cancel()
        movementDetectionJob?.cancel()
        webSocketClient?.close()
        updateTimer.cancel()
        sensorManager.unregisterListener(this)
    }

    private fun startReconnectionTimer() {
        // Cancel any existing job first
        reconnectionJob?.cancel()

        // Start a new job that periodically attempts to reconnect
        reconnectionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && (!_isConnected.value!!)) {
                // Calculate delay based on reconnect attempts
                val delayMs = if (reconnectAttempts < maxReconnectAttempts) {
                    1000L * (1 shl reconnectAttempts) // Exponential backoff
                } else {
                    30000L // Once every 30 seconds after max attempts
                }

                delay(delayMs)
                if (!_isConnected.value!!) {
                    _connectionStatus.postValue("Attempting to reconnect...")
                    connectWebSocket()
                }
            }
        }
    }
}
/*package com.example.malvoayant.ui.screens

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class StepCounterViewModel(application: Application) : AndroidViewModel(application) {
    private var isReconnecting = false

    // WebSocket properties
    private var webSocketClient: WebSocketClient? = null
    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    // WiFi position properties
    private val _wifiPositionLive = MutableLiveData<Pair<Float, Float>?>(null)
    val wifiPositionLive: LiveData<Pair<Float, Float>?> = _wifiPositionLive

    private val _wifiConfidence = MutableLiveData(0.0f)
    val wifiConfidence: LiveData<Float> = _wifiConfidence

    // For visualization - path tracking with only WiFi positions
    private val _pathPoints = MutableLiveData(listOf<Pair<Float, Float>>())
    val pathPoints: LiveData<List<Pair<Float, Float>>> = _pathPoints

    // Current position (only from WiFi)
    private val _currentPositionLive = MutableLiveData(Pair(0f, 0f))
    val currentPositionLive: LiveData<Pair<Float, Float>> = _currentPositionLive

    // Position update time tracking
    private var lastWifiUpdateTime = 0L
    private val _lastWifiUpdateAgo = MutableLiveData("Never")
    val lastWifiUpdateAgo: LiveData<String> = _lastWifiUpdateAgo

    // Connection status
    private val _connectionStatus = MutableLiveData("Disconnected")
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    private var reconnectionJob: kotlinx.coroutines.Job? = null

    // Start a timer to update the "last update" text
    private val updateTimer = CoroutineScope(Dispatchers.Default)

    init {
        // Start timer to update "last update ago" text
        updateTimer.launch {
            while (isActive) {
                updateLastUpdateText()
                delay(1000)  // Update every second
            }
        }
    }

    // WebSocket functions
    fun initWebSocket() {
        connectWebSocket()
        startReconnectionTimer()
    }

    private fun connectWebSocket() {
        if (isReconnecting) return
        isReconnecting = true

        try {
            webSocketClient?.close()
            webSocketClient = null
        } catch (e: Exception) {
            // Ignore any exceptions during cleanup
        }

        val serverUri = URI("ws://192.168.205.205:8000/ws/position") // Update with Pi's IP

        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                reconnectAttempts = 0  // Reset counter on successful connection
                _isConnected.postValue(true)
                _connectionStatus.postValue("Connected to Raspberry Pi")
                _errorMessage.postValue(null)
                requestWifiPositionUpdates()

                // Cancel reconnection timer since we're connected
                reconnectionJob?.cancel()
                reconnectionJob = null
            }

            override fun onMessage(message: String?) {
                Log.d("WebSocket", "Message received: $message")
                message?.let {
                    try {
                        val json = JSONObject(it)
                        when (json.getString
                            ("type")) {
                            "wifi_position" -> {
                                val position = json.getJSONObject("position")
                                val x = position.getDouble("x").toFloat()
                                val y = position.getDouble("y").toFloat()

                                // Optional: default confidence to 1.0 if not present
                                val confidence = if (position.has("confidence")) {
                                    position.getDouble("confidence").toFloat()
                                } else {
                                    1.0f
                                }

                                _wifiPositionLive.postValue(Pair(x, y))
                                _wifiConfidence.postValue(confidence)
                                lastWifiUpdateTime = System.currentTimeMillis()

                                // Update current position with WiFi position
                                _currentPositionLive.postValue(Pair(x, y))

                                // Update path points with WiFi position
                                val currentPath = _pathPoints.value ?: listOf()
                                val updatedPath = currentPath + Pair(x, y)
                                _pathPoints.postValue(updatedPath)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WebSocket", "Error parsing message: ${e.message}", e)
                        _errorMessage.postValue("Error parsing message: ${e.message}")
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                _isConnected.postValue(false)
                _connectionStatus.postValue("Disconnected: $reason")

                // reconnect with exponential backoff
                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++
                }

                startReconnectionTimer()
            }

            override fun onError(ex: Exception?) {
                _isConnected.postValue(false)
                _errorMessage.postValue("WebSocket error: ${ex?.message}")
                // Attempt reconnect after delay
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000)
                    connectWebSocket()
                }
            }
        }

        // connection timeout and handle reconnection
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(10000)
                if (webSocketClient != null && !_isConnected.value!!) {
                    _connectionStatus.postValue("Connection timed out")
                    _errorMessage.postValue("Failed to connect to server")
                    webSocketClient?.close()
                    webSocketClient = null
                }
            } finally {
                isReconnecting = false
            }
        }

        try {
            webSocketClient?.connect()
        } catch (e: Exception) {
            _errorMessage.postValue("Connection error: ${e.message}")
            isReconnecting = false
        }
    }

    private fun requestWifiPositionUpdates(intervalSeconds: Double = 1.0) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "start_position_updates")
        jsonObject.put("interval", intervalSeconds)

        webSocketClient?.let { client ->
            if (client.isOpen) {
                client.send(jsonObject.toString())
            }
        }
    }

    fun requestSingleWifiUpdate() {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("type", "single_position_request")

            webSocketClient?.let { client ->
                if (client.isOpen) {
                    client.send(jsonObject.toString())
                }
            }
        } catch (e: Exception) {
            _errorMessage.postValue("Error sending request: ${e.message}")
        }
    }

    private fun updateLastUpdateText() {
        if (lastWifiUpdateTime == 0L) {
            _lastWifiUpdateAgo.postValue("Never")
            return
        }

        val age = System.currentTimeMillis() - lastWifiUpdateTime
        val text = when {
            age < 1000 -> "Just now"
            age < 60000 -> "${age / 1000} seconds ago"
            else -> "${age / 60000} minutes ago"
        }

        _lastWifiUpdateAgo.postValue(text)
    }

    // Public functions for controlling the system
    fun startListening() {
        // Only initialize WebSocket connection - no sensors
        initWebSocket()
    }

    fun stopListening() {
        // Close WebSocket connection
        webSocketClient?.close()
    }

    fun resetAll() {
        // Clear path points
        _pathPoints.value = listOf()

        // Keep the last WiFi position but reset the path
        val lastWifiPos = _wifiPositionLive.value ?: Pair(0f, 0f)
        _currentPositionLive.value = lastWifiPos
    }

    override fun onCleared() {
        super.onCleared()
        reconnectionJob?.cancel()
        webSocketClient?.close()
        updateTimer.cancel()
    }

    private fun startReconnectionTimer() {
        // Cancel any existing job first
        reconnectionJob?.cancel()

        // Start a new job that periodically attempts to reconnect
        reconnectionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && (!_isConnected.value!!)) {
                // Calculate delay based on reconnect attempts
                val delayMs = if (reconnectAttempts < maxReconnectAttempts) {
                    1000L * (1 shl reconnectAttempts) // Exponential backoff
                } else {
                    30000L // Once every 30 seconds after max attempts
                }

                delay(delayMs)
                if (!_isConnected.value!!) {
                    _connectionStatus.postValue("Attempting to reconnect...")
                    connectWebSocket()
                }
            }
        }
    }
}*/