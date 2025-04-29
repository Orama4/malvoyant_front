package com.example.malvoayant.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.malvoayant.ui.components.DetectionOverlay
import com.example.malvoayant.ui.utils.BoundingBox
import com.example.malvoayant.ui.utils.Constants
import com.example.malvoayant.ui.utils.Detector
import com.example.malvoayant.ui.utils.SpeechHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun OD1Screen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val boundingBoxes = remember { mutableStateOf<List<BoundingBox>>(emptyList()) }
    val inferenceTime = remember { mutableStateOf(0L) }
    val speechHelper = remember { SpeechHelper(context) }
    speechHelper.initializeSpeech {}

    val detector = remember {
        Detector(context, Constants.MODEL_PATH, Constants.LABELS_PATH, object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                boundingBoxes.value = emptyList()
            }

            override fun onDetect(boxes: List<BoundingBox>, time: Long) {
                boundingBoxes.value = boxes
                inferenceTime.value = time
            }
        }, speechHelper).apply { setup() }
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        DetectionOverlay(boundingBoxes = boundingBoxes.value)

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Text(text = "Inference Time: ${inferenceTime.value}ms")
        }
    }

    LaunchedEffect(Unit) {
        //startCamera(context, previewView, lifecycleOwner, detector, cameraExecutor)

    }
}

fun startCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    detector: Detector,
    cameraExecutor: ExecutorService,
    isFrontCamera: Boolean = false
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display.rotation)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(previewView.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

            imageProxy.use {
                bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
            }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
            )

            detector.detect(rotatedBitmap)
            imageProxy.close()
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e("Camera", "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(context))
}
