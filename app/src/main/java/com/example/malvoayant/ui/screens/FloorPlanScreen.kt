package com.example.malvoayant.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FloorPlanScreen(
    stepCounterViewModel: StepCounterViewModel = viewModel(),
    floorPlanViewModel: FloorPlanViewModel = viewModel()
) {
    val context = LocalContext.current
    val floorPlanState = floorPlanViewModel.floorPlanState
    val isConnected by stepCounterViewModel.isConnected.observeAsState(false)

    // Observe position data from Raspberry Pi
    val wifiPosition by stepCounterViewModel.wifiPositionLive.observeAsState(null)
    val lastWifiUpdateAgo by stepCounterViewModel.lastWifiUpdateAgo.observeAsState("Never")
    val wifiConfidence by stepCounterViewModel.wifiConfidence.observeAsState(0f)

    // Remember the launcher for file picking
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            floorPlanViewModel.importFromGeoJSONUri(context, it)
        }
    }

    LaunchedEffect(Unit) {
        // Load default floor plan from assets when the screen is first composed
        floorPlanViewModel.loadGeoJSONFromAssets(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Connection status indicator and WiFi position display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Connection status and control button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isConnected) "WebSocket: Connected" else "WebSocket: Disconnected",
                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = {
                            if (isConnected) {
                                stepCounterViewModel.stopListening()
                            } else {
                                stepCounterViewModel.startListening()
                            }
                        }
                    ) {
                        Text(if (isConnected) "Disconnect" else "Connect")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // WiFi Position data display
                wifiPosition?.let { position ->
                    CoordinatesDisplay(
                        title = "WiFi Position",
                        x = position.first,
                        y = position.second,
                        confidence = wifiConfidence,
                        lastUpdate = lastWifiUpdateAgo
                    )
                } ?: run {
                    Text(
                        text = "Waiting for WiFi position data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Floor plan canvas
        FloorPlanCanvasView(
            floorPlanState = floorPlanState,
            stepCounterViewModel = stepCounterViewModel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { launcher.launch("application/json") }) {
                Text("Load Floor Plan")
            }

            Button(onClick = { stepCounterViewModel.resetAll() }) {
                Text("Reset Path")
            }

            Button(onClick = { stepCounterViewModel.requestSingleWifiUpdate() }) {
                Text("Get Position")
            }
        }
    }
}

@Composable
fun CoordinatesDisplay(
    title: String,
    x: Float,
    y: Float,
    confidence: Float? = null,
    lastUpdate: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "X: ${String.format("%.2f", x)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Y: ${String.format("%.2f", y)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                confidence?.let {
                    Text(
                        text = "Conf: ${String.format("%.1f", it * 100)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            lastUpdate?.let {
                Text(
                    text = "Updated: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}