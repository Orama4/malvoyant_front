package com.example.malvoayant.ui.screens


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.malvoayant.websocket.WebSocketClient

@Composable
fun PathTracingCanvas(
    modifier: Modifier = Modifier,
    serverUrl: String = "ws://10.0.2.2:3000" // Default localhost for Android emulator
) {
    // Create WebSocket client
    val webSocketClient = remember { WebSocketClient(serverUrl) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Connect/disconnect based on lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    webSocketClient.connect()
                }
                Lifecycle.Event.ON_STOP -> {
                    webSocketClient.disconnect()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup when the composable is disposed
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webSocketClient.disconnect()
        }
    }

    // Connect on first composition
    LaunchedEffect(Unit) {
        webSocketClient.connect()
    }

    Box(
        modifier = modifier
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Draw the path
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val points = webSocketClient.pathPoints

            if (points.isNotEmpty()) {
                // Start path at the first point
                path.moveTo(points[0].x, points[0].y)

                // Connect all other points
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }

                // Draw the path
                drawPath(
                    path = path,
                    color = Color(0xFF4CAF50), // Green color
                    style = Stroke(width = 5f)
                )

                // Draw points (optional)
                points.forEach { point ->
                    drawCircle(
                        color = Color(0xFF2196F3), // Blue color
                        radius = 8f,
                        center = point
                    )
                }

                // Draw current position (most recent point)
                if (points.isNotEmpty()) {
                    drawCircle(
                        color = Color.Red,
                        radius = 12f,
                        center = points.last()
                    )
                }
            }
        }
    }
}