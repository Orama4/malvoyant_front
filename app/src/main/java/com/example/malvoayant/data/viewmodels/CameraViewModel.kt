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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URI

class CameraViewModel(application: Application) : AndroidViewModel(application), Detector.DetectorListener {

    private val frameChannel = Channel<Bitmap>(Channel.UNLIMITED)

    private val _currentFrame = MutableStateFlow<Bitmap?>(null)
    val currentFrame = _currentFrame.asStateFlow()

    private val _boundingBoxes = MutableStateFlow<List<BoundingBox>>(emptyList())
    val boundingBoxes = _boundingBoxes.asStateFlow()

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime = _inferenceTime.asStateFlow()

    private lateinit var detector: Detector
    private val speechHelper = SpeechHelper(application.applicationContext)


    private val webSocketClient = CameraWebSocketClient(
        URI("ws://192.168.250.205:8765"),
        frameChannel
    )

    init {
        Log.d("CameraViewModel", "ViewModel initialized")
        connectWebSocket()
        observeFrames()
        speechHelper.initializeSpeech {}
        setupDetector()
        Log.d("CameraViewModel", "ViewModel end")

    }

    private fun setupDetector() {
        Log.d("Detector", "Detector setup haifaa")
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
        webSocketClient.connect()
        Log.d("CameraViewModel", "WebSocket connected")
    }

    private fun observeFrames() {

        viewModelScope.launch {
            for (frame in frameChannel) {
                Log.d("CameraViewModel", "Observing frames")
                _currentFrame.value = frame

                detector.detect(frame)  // RUN DETECTION HERE!
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
        detector.clear()
        webSocketClient.close()
    }
}
