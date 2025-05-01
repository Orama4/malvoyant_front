package com.example.malvoayant.data.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.data.websocket.CameraWebSocketClient
import com.example.malvoayant.ui.utils.BoundingBox
import com.example.malvoayant.ui.utils.Constants
import com.example.malvoayant.ui.utils.Detector
import com.example.malvoayant.ui.utils.SpeechHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

class CameraViewModel(application: Application) : AndroidViewModel(application), Detector.DetectorListener {

    private val frameChannel = Channel<Bitmap>(Channel.UNLIMITED)

    private val _currentFrame = MutableStateFlow<Bitmap?>(null)
    val currentFrame = _currentFrame.asStateFlow()

    private val _boundingBoxes = MutableStateFlow<List<BoundingBox>>(emptyList())
    val boundingBoxes = _boundingBoxes.asStateFlow()

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime = _inferenceTime.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Connecting...")
    val connectionStatus = _connectionStatus.asStateFlow()

    private lateinit var detector: Detector
    private val speechHelper = SpeechHelper(application.applicationContext)

    // Use an IP that's correct for your network
    private val webSocketClient = CameraWebSocketClient(
        URI("ws://192.168.250.205:8765"),
        frameChannel
    )

    init {
        Log.d("CameraViewModel", "ViewModel initialized")
        connectWebSocket()
        observeFrames()

        // Comment out detector setup if you don't need it for now
        // speechHelper.initializeSpeech {}
        // setupDetector()

        // Add frame monitoring
        startFrameMonitoring()
    }

    private fun setupDetector() {
        Log.d("Detector", "Setting up detector")
        detector = Detector(
            getApplication<Application>().applicationContext,
            Constants.MODEL_PATH,
            Constants.LABELS_PATH,
            this,
            speechHelper
        )
        detector.setup()
    }

    private fun connectWebSocket() {
        try {
            Log.d("CameraViewModel", "Connecting to WebSocket...")
            _connectionStatus.value = "Connecting to camera..."
            webSocketClient.connect()
            Log.d("CameraViewModel", "WebSocket connection initiated")
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Failed to connect to WebSocket", e)
            _connectionStatus.value = "Connection failed: ${e.message}"
        }
    }

    private fun observeFrames() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("CameraViewModel", "Starting frame observation")
            try {
                for (frame in frameChannel) {
                    Log.d("CameraViewModel", "Received frame: ${frame.width}x${frame.height}")

                    // Post UI updates on Main thread
                    withContext(Dispatchers.Main) {
                        _currentFrame.value = frame
                        _connectionStatus.value = "Connected"
                    }

                    // Run detection on a different dispatcher
                    if (::detector.isInitialized) {
                        launch(Dispatchers.Default) {
                            Log.d("CameraViewModel", "Running detection on frame")
                            detector.detect(frame)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error processing frames", e)
                _connectionStatus.value = "Frame processing error: ${e.message}"
            }
        }
    }


    // Monitor frame reception to detect stalls
    private fun startFrameMonitoring() {
        viewModelScope.launch {
            var lastFrameTime = System.currentTimeMillis()
            var lastReconnectTime = 0L
            val RECONNECT_TIMEOUT = 10000 // 10 seconds

            while (true) {
                kotlinx.coroutines.delay(5000) // Check every 5 seconds

                val currentTime = System.currentTimeMillis()
                val timeSinceLastFrame = currentTime - lastFrameTime

                // Update last frame time when a new frame is received
                _currentFrame.value?.let {
                    lastFrameTime = currentTime
                }

                // If no frames for more than 10 seconds, try reconnecting
                if (timeSinceLastFrame > RECONNECT_TIMEOUT &&
                    (currentTime - lastReconnectTime > RECONNECT_TIMEOUT)) {

                    Log.w("CameraViewModel", "No frames received for ${timeSinceLastFrame/1000} seconds, reconnecting...")
                    _connectionStatus.value = "Reconnecting..."

                    webSocketClient.close()
                    kotlinx.coroutines.delay(1000)
                    webSocketClient.reconnect()

                    lastReconnectTime = currentTime
                }
            }
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        _boundingBoxes.value = boundingBoxes
        _inferenceTime.value = inferenceTime
    }

    override fun onEmptyDetect() {
        _boundingBoxes.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("CameraViewModel", "ViewModel cleared, cleaning up resources")
        webSocketClient.close()

        // Only clear detector if it was initialized
        if (::detector.isInitialized) {
            detector.clear()
        }
    }
}