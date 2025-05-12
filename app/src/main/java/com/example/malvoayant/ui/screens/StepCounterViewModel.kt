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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class StepCounterViewModel(application: Application) : AndroidViewModel(application) {
    private var isReconnecting = false

    // Sensor-related properties
    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Step counting and position tracking
    private val _steps = MutableLiveData(0)
    val steps: LiveData<Int> = _steps

    private val _currentHeadingLive = MutableLiveData(0f)
    val currentHeadingLive: LiveData<Float> = _currentHeadingLive

    private var currentHeading = 0f
    private var currentPosition = Pair(0f, 0f)
    private var stepLength = 0.7f  // Average step length in meters

    private val _currentPositionLive = MutableLiveData(Pair(0f, 0f))
    val currentPositionLive: LiveData<Pair<Float, Float>> = _currentPositionLive

    private val _pathPoints = MutableLiveData(listOf(Pair(0f, 0f)))
    val pathPoints: LiveData<List<Pair<Float, Float>>> = _pathPoints

    // WebSocket properties
    private var webSocketClient: WebSocketClient? = null
    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    // WiFi position properties
    private val _wifiPositionLive = MutableLiveData<Pair<Float, Float>?>(null)
    val wifiPositionLive: LiveData<Pair<Float, Float>?> = _wifiPositionLive

    private val _wifiConfidence = MutableLiveData(0.0f)
    val wifiConfidence: LiveData<Float> = _wifiConfidence

    // Fused position properties
    private val _fusedPositionLive = MutableLiveData(Pair(0f, 0f))
    val fusedPositionLive: LiveData<Pair<Float, Float>> = _fusedPositionLive

    // Position update time tracking
    private var lastWifiUpdateTime = 0L
    private val _lastWifiUpdateAgo = MutableLiveData("Never")
    val lastWifiUpdateAgo: LiveData<String> = _lastWifiUpdateAgo

    // Fusion settings
    private var useWifiFusion = true
    private var maxWifiAgeMs = 10000  // Maximum age of WiFi data to use (10 seconds)

    // Start a timer to update the "last update" text
    private val updateTimer = CoroutineScope(Dispatchers.Default)

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    private var reconnectionJob: kotlinx.coroutines.Job? = null

    // Navigation-related additions
    private val _currentOrientation = MutableLiveData<Float>()
    val currentOrientation: LiveData<Float> = _currentOrientation

    private val _navigationInstructions = MutableLiveData<String>()
    val navigationInstructions: LiveData<String> = _navigationInstructions

    private val _nearestLandmark = MutableLiveData<Pair<String, Float>?>(null)
    val nearestLandmark: LiveData<Pair<String, Float>?> = _nearestLandmark

    private val _hazardWarning = MutableLiveData<String?>(null)
    val hazardWarning: LiveData<String?> = _hazardWarning

    // Navigation calibration
    private var calibratedStepLength = 0.7f // Default, will be updated from GeoJSON
    private var magneticDeclination = 0f // Will be updated from GeoJSON
    private var northReferenceWallId: Int? = null

    var floorPlanState: FloorPlanState? = null
    init {
        // Start timer to update "last update ago" text
        updateTimer.launch {
            while (isActive) {
                updateLastUpdateText()
                delay(1000)  // Update every second
            }
        }
    }

    // Sensor listeners
    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                _steps.postValue((_steps.value ?: 0) + 1)
                updatePosition()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private val rotationListener = object : SensorEventListener {
        private var calibrationOffset = 0f

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                // Get raw azimuth (-π to π radians, where 0 = magnetic north)
                val azimuthRad = orientation[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()

                // Normalize to 0-360
                azimuthDeg = (azimuthDeg + 360) % 360

                // Apply calibration offset
                val calibratedHeading = (azimuthDeg - calibrationOffset + 360) % 360

                _currentHeadingLive.value = calibratedHeading
                currentHeading = calibratedHeading
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    // Navigation functions
    fun updateNavigationSettings(settings: NavigationSettings?) {
        settings?.let {
            calibratedStepLength = it.defaultStepLength
            magneticDeclination = it.magneticDeclination
            northReferenceWallId = it.northReference.wallId
            stepLength = calibratedStepLength
        }
    }

    fun getNearestLandmark(currentPos: Pair<Float, Float>): Pair<String, Float>? {
        floorPlanState?.let { state ->
            // Check POIs first
            state.pois.minByOrNull { poi ->
                sqrt((poi.x - currentPos.first).pow(2) + (poi.y - currentPos.second).pow(2))
            }?.let { nearestPoi ->
                val distance = sqrt((nearestPoi.x - currentPos.first).pow(2) +
                        (nearestPoi.y - currentPos.second).pow(2))
                return Pair(nearestPoi.name, distance)
            }

            // Could add checks for doors/other landmarks here
        }
        return null
    }

    // Add these to StepCounterViewModel class
    private val _calibrationState = MutableLiveData(CalibrationState.IDLE)
    val calibrationState: LiveData<CalibrationState> = _calibrationState

    private var xAxisHeading: Float? = null
    private var yAxisHeading: Float? = null
    private var calibrationStartPosition: Pair<Float, Float> = Pair(0f, 0f)

    enum class CalibrationState {
        IDLE, CALIBRATING_X, CALIBRATING_Y, CALIBRATED
    }

    // New calibration functions
    fun startXAxisCalibration() {
        _calibrationState.value = CalibrationState.CALIBRATING_X
        calibrationStartPosition = currentPosition
    }

    fun completeXAxisCalibration() {
        xAxisHeading = currentHeading
        _calibrationState.value = CalibrationState.CALIBRATING_Y
        calibrationStartPosition = currentPosition
    }

    fun completeYAxisCalibration() {
        yAxisHeading = currentHeading
        _calibrationState.value = CalibrationState.CALIBRATED
        saveCalibration()
    }

    fun resetCalibration() {
        xAxisHeading = null
        yAxisHeading = null
        _calibrationState.value = CalibrationState.IDLE
    }

    private fun saveCalibration() {
        // Save calibration to preferences if needed
    }

    // Modified position update to use calibrated axes
    private fun updatePosition() {
        val adjustedHeading = getAdjustedHeading()
        val rad = Math.toRadians(adjustedHeading.toDouble())

        val deltaX = (stepLength * cos(rad)).toFloat()
        val deltaY = (stepLength * sin(rad)).toFloat()

        currentPosition = Pair(currentPosition.first + deltaX, currentPosition.second + deltaY)
        _currentPositionLive.postValue(currentPosition)
        _pathPoints.postValue(_pathPoints.value.orEmpty() + currentPosition)

        // Navigation updates
        _nearestLandmark.postValue(getNearestLandmark(currentPosition))
        _hazardWarning.postValue(checkHazards(currentPosition, floorPlanState?.hazards ?: emptyList()))
        performPositionFusion()
    }

    private fun getAdjustedHeading(): Float {
        return when (_calibrationState.value) {
            CalibrationState.CALIBRATED -> {
                // Convert current heading to calibrated coordinate system
                val rawHeading = currentHeading
                val xAxis = xAxisHeading ?: 0f
                val yAxis = yAxisHeading ?: (xAxis + 90) % 360

                // Calculate angle between current heading and x-axis
                var angle = (rawHeading - xAxis + 360) % 360

                // For simplicity, we'll just return the raw heading when calibrated
                // In a full implementation, we'd convert to the calibrated coordinate system
                rawHeading
            }
            else -> currentHeading
        }
    }

    fun generateNavigationInstructions(
        currentPos: Pair<Float, Float>,
        targetPos: Pair<Float, Float>
    ): String {
        val dx = targetPos.first - currentPos.first
        val dy = targetPos.second - currentPos.second
        val distance = sqrt(dx * dx + dy * dy)
        val bearing = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

        // Calculate relative angle to target
        var relativeAngle = (bearing - (currentHeading ?: 0f) + 360) % 360

        return when {
            distance < 0.5 -> "You have arrived at your destination"
            relativeAngle < 45 || relativeAngle > 315 -> "Walk straight for ${"%.1f".format(distance)} meters"
            relativeAngle < 135 -> "Turn right and walk ${"%.1f".format(distance)} meters"
            else -> "Turn left and walk ${"%.1f".format(distance)} meters"
        }
    }

    fun checkHazards(currentPos: Pair<Float, Float>, hazards: List<HazardMarker>): String? {
        hazards.forEach { hazard ->
            val distance = sqrt(
                (hazard.coordinates[0] - currentPos.first).pow(2) +
                        (hazard.coordinates[1] - currentPos.second).pow(2)
            )
            if (distance < hazard.warningDistance) {
                return "Warning: ${hazard.description} ahead"
            }
        }
        return null
    }

    // Enhanced position update with navigation checks
   /* private fun updatePosition() {
        val adjustedHeading = (currentHeading + 90) % 360 // Adjust for coordinate system
        val rad = Math.toRadians(adjustedHeading.toDouble())

        val deltaX = (stepLength * cos(rad)).toFloat()
        val deltaY = (stepLength * sin(rad)).toFloat()

        currentPosition = Pair(currentPosition.first + deltaX, currentPosition.second + deltaY)
        _currentPositionLive.postValue(currentPosition)

        // Update path
        _pathPoints.postValue(_pathPoints.value.orEmpty() + currentPosition)

        // Navigation updates
        _nearestLandmark.postValue(getNearestLandmark(currentPosition))
        _hazardWarning.postValue(checkHazards(currentPosition, floorPlanState?.hazards ?: emptyList()))

        performPositionFusion()
    }

    */

    // Calibration functions
    fun calibrateStepLength(knownDistance: Float, stepsTaken: Int) {
        if (stepsTaken > 0) {
            calibratedStepLength = knownDistance / stepsTaken
            stepLength = calibratedStepLength
        }
    }

    fun calibrateOrientation(referenceHeading: Float = 0f) {
        // Get current raw sensor reading
        val currentAzimuth = _currentHeadingLive.value ?: 0f

        // Calculate new offset (current reading vs desired heading)
        var calibrationOffset = (currentAzimuth - referenceHeading + 360) % 360

        // Apply immediately
        _currentHeadingLive.value = referenceHeading
        currentHeading = referenceHeading

        Log.d("Calibration", "Set offset to: $calibrationOffset°")
    }

    // WebSocket functions
    fun initWebSocket() {
        connectWebSocket()
        startReconnectionTimer()
    }


    private val _connectionStatus = MutableLiveData("Disconnected")
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

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
                message?.let {
                    try {
                        val json = JSONObject(it)
                        when (json.getString("type")) {
                            "wifi_position" -> {
                                val position = json.getJSONObject("position")
                                val x = position.getDouble("x").toFloat()
                                val y = position.getDouble("y").toFloat()
                                val confidence = position.getDouble("confidence").toFloat()

                                _wifiPositionLive.postValue(Pair(x, y))
                                _wifiConfidence.postValue(confidence)
                                lastWifiUpdateTime = System.currentTimeMillis()
                                performPositionFusion()
                            }
                        }
                    } catch (e: Exception) {
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


    // Enhanced position fusion algorithm
    private fun performPositionFusion() {
        val sensorPosition = currentPosition
        val wifiPosition = _wifiPositionLive.value
        val wifiAge = System.currentTimeMillis() - lastWifiUpdateTime
        val wifiConfidence = _wifiConfidence.value ?: 0f

        if (!useWifiFusion || wifiPosition == null || wifiAge >= maxWifiAgeMs) {
            _fusedPositionLive.postValue(sensorPosition)
            return
        }

        // Calculate dynamic weights
        val maxSensorDrift = 0.5f // Maximum expected sensor drift per second (m/s)
        val expectedDrift =
            maxSensorDrift * (wifiAge / 1000f) // Expected drift since last WiFi update

        // Base WiFi weight based on confidence (0.2-0.8)
        val baseWifiWeight = 0.2f + (wifiConfidence * 0.6f)

        // Reduce WiFi weight based on age (linear from 1 to 0 over maxWifiAgeMs)
        val ageFactor = 1f - (wifiAge.toFloat() / maxWifiAgeMs.toFloat())

        // Calculate distance between sensor and WiFi positions
        val dx = sensorPosition.first - wifiPosition.first
        val dy = sensorPosition.second - wifiPosition.second
        val distance = sqrt(dx * dx + dy * dy)

        // Reduce WiFi weight if distance is larger than expected drift
        val distanceFactor = if (expectedDrift > 0) {
            minOf(1f, expectedDrift / distance)
        } else {
            1f
        }

        val wifiWeight = baseWifiWeight * ageFactor * distanceFactor
        val sensorWeight = 1f - wifiWeight

        // Perform weighted average
        val fusedX = sensorPosition.first * sensorWeight + wifiPosition.first * wifiWeight
        val fusedY = sensorPosition.second * sensorWeight + wifiPosition.second * wifiWeight

        _fusedPositionLive.postValue(Pair(fusedX, fusedY))
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
        sensorManager.registerListener(
            stepListener,
            stepDetector,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        sensorManager.registerListener(
            rotationListener,
            rotationVector,
            SensorManager.SENSOR_DELAY_GAME
        )

        // Initialize WebSocket connection
        initWebSocket()
    }

    fun stopListening() {
        sensorManager.unregisterListener(stepListener)
        sensorManager.unregisterListener(rotationListener)

        // Close WebSocket connection
        webSocketClient?.close()
    }

    fun resetAll() {
        _steps.value = 0
        currentPosition = Pair(0f, 0f)
        _currentPositionLive.value = currentPosition
        _pathPoints.value = listOf(currentPosition)
        _fusedPositionLive.value = currentPosition
    }

    fun calibrateWithWifi() {
        // Request a single Wi-Fi position update
        requestSingleWifiUpdate()

        // Wait briefly for response, then use it for calibration
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)

            // Get the latest Wi-Fi position
            val wifiPosition = _wifiPositionLive.value

            if (wifiPosition != null) {
                // Reset position to Wi-Fi position
                currentPosition = wifiPosition
                _currentPositionLive.value = wifiPosition
                _pathPoints.value = listOf(wifiPosition)
                _fusedPositionLive.value = wifiPosition
            }
        }
    }

    fun toggleWifiFusion(enabled: Boolean) {
        useWifiFusion = enabled
        performPositionFusion()
    }

    // Helper functions
    private fun calculateDistance(p1: Pair<Float, Float>, p2: Pair<Float, Float>): Float {
        val dx = p1.first - p2.first
        val dy = p1.second - p2.second
        return sqrt(dx * dx + dy * dy)
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
}
