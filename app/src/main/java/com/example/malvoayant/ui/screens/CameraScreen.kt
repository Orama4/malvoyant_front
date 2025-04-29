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

@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val frame by viewModel.currentFrame.collectAsState()
    val boundingBoxes by viewModel.boundingBoxes.collectAsState()
    val inferenceTime by viewModel.inferenceTime.collectAsState()

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        frame?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Camera Stream",
                modifier = Modifier.fillMaxSize()
            )
        }
       /* // 🟩 Draw the bounding boxes over the frame
        DetectionOverlay(boundingBoxes = boundingBoxes)

        // 🕑 Show inference time
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp)
        ) {
            androidx.compose.material3.Text(text = "Inference Time: ${inferenceTime}ms")
        }*/
    }
}
