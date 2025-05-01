package com.example.malvoayant.ui.screens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.data.viewmodels.CameraViewModel
import com.example.malvoayant.ui.components.DetectionOverlay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val frame by viewModel.currentFrame.collectAsState()
    val boundingBoxes by viewModel.boundingBoxes.collectAsState()
    val inferenceTime by viewModel.inferenceTime.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var frameCounter by remember { mutableStateOf(0) }

    // Increment the frame counter whenever we get a new frame
    LaunchedEffect(frame) {
        frame?.let { frameCounter++ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Display the camera frame
        frame?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Camera Stream",
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.Red),
                contentScale = ContentScale.Fit
            )

            // Add the detection overlay on top of the camera feed
            if (boundingBoxes.isNotEmpty()) {
                DetectionOverlay(
                    boundingBoxes = boundingBoxes,
                    boxColor = Color.Green,
                    textColor = Color.White,
                    textBackgroundColor = Color.Black.copy(alpha = 0.7f)
                )
            }

            // Debug overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Text(
                    text = "Frame #$frameCounter: ${it.width}x${it.height}",
                    color = Color.Green
                )

                Text(
                    text = "Status: $connectionStatus",
                    color = Color.Green
                )

                if (inferenceTime > 0) {
                    Text(
                        text = "Inference: ${inferenceTime}ms",
                        color = Color.Green
                    )
                }

                Text(
                    text = "Objects: ${boundingBoxes.size}",
                    color = if (boundingBoxes.isNotEmpty()) Color.Yellow else Color.Green
                )
            }
        } ?: run {
            // Show placeholder when no frame is available
            Text(
                text = "Waiting for camera stream...",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )

            Text(
                text = "Status: $connectionStatus",
                color = Color.Yellow,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
    }
}