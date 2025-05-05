package com.example.malvoayant.ui.screens
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.malvoayant.ui.utils.ImageAnalyzer
import com.example.malvoayant.ui.utils.SpeechHelper
import kotlinx.coroutines.delay
import java.io.File


@Composable
fun OD2Screen(
    navController: NavController,
    analyzer: ImageAnalyzer = com.example.malvoayant.ui.utils.ImageAnalyzer
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    var detectedText by remember { mutableStateOf("") }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var isCameraReady by remember { mutableStateOf(false) }

    // Initialiser la caméra (sans appeler de composable ici !)
    LaunchedEffect(Unit) {
        ImageAnalyzer.initSpeech(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture
            )

            // ✅ Déclencheur pour lancer la prise de photo plus tard
            isCameraReady = true
        }, ContextCompat.getMainExecutor(context))
    }

    // 📸 Prendre la photo automatiquement 2s après que la caméra soit prête
    LaunchedEffect(isCameraReady) {
        if (isCameraReady) {
            while (true) {
                Log.d("OD2Screen", "Waiting 4s before capture...")
                delay(4000)

                val photoFile = File(context.cacheDir, "captured.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d("OD2Screen", "Photo saved: ${photoFile.absolutePath}")
                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            analyzer.analyze(bitmap) { resultText ->
                                detectedText = resultText
                                Log.d("OD2Screen", "Detected text: $resultText")
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("OD2Screen", "Capture failed: ${exception.message}")
                        }
                    }
                )

                Log.d("OD2Screen", "Waiting 10s before next capture...")
                delay(10_000)
            }
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            ImageAnalyzer.shutdownSpeech()
        }
    }

    Column {
        AndroidView(
            factory = {
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Detected: $detectedText",
            modifier = Modifier.padding(16.dp)
        )
    }
}