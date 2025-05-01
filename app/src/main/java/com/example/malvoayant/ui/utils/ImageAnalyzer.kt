package com.example.malvoayant.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageAnalyzer {

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private var lastSpokenText: String? = null
    private var stableCount = 0
    private const val STABILITY_THRESHOLD = 3

    private lateinit var speechHelper: SpeechHelper

    fun initSpeech(context: Context) {
        speechHelper = SpeechHelper(context)
        speechHelper.initializeSpeech {}
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun analyze(imageProxy: ImageProxy, onResult: (String) -> Unit) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detected = visionText.text.trim()
                onResult(detected)

                // Stabilisation simple : parler seulement si le texte est répété plusieurs fois
                if (detected.isNotEmpty()) {
                    if (detected == lastSpokenText) {
                        stableCount++
                    } else {
                        lastSpokenText = detected
                        stableCount = 1
                    }

                    if (stableCount == STABILITY_THRESHOLD) {
                        speechHelper.speak("It's written: $detected")
                        stableCount = 0
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ImageAnalyzer", "OCR Failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    // 🆕 Nouvelle fonction pour analyse à partir d'un Bitmap (via ImageCapture)
    fun analyze(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detected = visionText.text.trim()
                onResult(detected)

                // Même logique de stabilisation
                if (detected.isNotEmpty()) {
                    if (detected == lastSpokenText) {
                        stableCount++
                    } else {
                        lastSpokenText = detected
                        stableCount = 1
                    }


                    speechHelper.speak("It's written: $detected")


                }
            }
            .addOnFailureListener { e ->
                Log.e("ImageAnalyzer", "OCR Failed (bitmap)", e)
            }
    }
    fun shutdownSpeech() {
        if (::speechHelper.isInitialized) {
            speechHelper.shutdown()
        }
    }
}
