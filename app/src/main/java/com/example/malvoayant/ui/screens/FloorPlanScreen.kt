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
    val steps by stepCounterViewModel.steps.observeAsState(0)
    val currentHeading by stepCounterViewModel.currentHeadingLive.observeAsState(0f)
    val navigationInstructions by stepCounterViewModel.navigationInstructions.observeAsState("")
    val hazardWarning by stepCounterViewModel.hazardWarning.observeAsState(null)
    val nearestLandmark by stepCounterViewModel.nearestLandmark.observeAsState(null)

    // Observe position data from Raspberry Pi
    val wifiPosition by stepCounterViewModel.wifiPositionLive.observeAsState(null)
    val lastWifiUpdateAgo by stepCounterViewModel.lastWifiUpdateAgo.observeAsState("Never")
    val wifiConfidence by stepCounterViewModel.wifiConfidence.observeAsState(0f)
    val fusedPosition by stepCounterViewModel.fusedPositionLive.observeAsState(Pair(0f, 0f))

    // Remember the launcher for file picking
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            floorPlanViewModel.importFromGeoJSONUri(context, it)
        }
    }

    LaunchedEffect(floorPlanState.navigationSettings) {
        floorPlanState.navigationSettings?.let {
            stepCounterViewModel.updateNavigationSettings(it)
        }
    }

    LaunchedEffect(Unit) {
        // Load default floor plan from assets when the screen is first composed
        floorPlanViewModel.loadGeoJSONFromAssets(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation information panel
        NavigationInfoPanel(
            steps = steps,
            currentHeading = currentHeading,
            navigationInstructions = navigationInstructions,
            hazardWarning = hazardWarning,
            nearestLandmark = nearestLandmark,
            modifier = Modifier.fillMaxWidth()
        )

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

                // Position data display
                Column {
                    CoordinatesDisplay(
                        title = "Fused Position",
                        x = fusedPosition.first,
                        y = fusedPosition.second,
                        lastUpdate = "Live"
                    )

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

            Button(onClick = { stepCounterViewModel.resetAll() }) {
                Text("Reset Path")
            }

            Button(onClick = {
                stepCounterViewModel.calibrateOrientation(0f) // Force 0° (east)
            }) { Text("Set East") }

            Button(onClick = {
                stepCounterViewModel.calibrateOrientation(90f) // Force 90° (south)
            }) { Text("Set South") }

        }

        // Calibration controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                stepCounterViewModel.calibrateWithWifi()
            }) {
                Text("Calibrate Position")
            }

            Button(onClick = {
                // Calibrate orientation to north (assuming wall 1 is north)
                floorPlanState.navigationSettings?.northReference?.direction?.let {
                    stepCounterViewModel.calibrateOrientation(it)
                }
            }) {
                Text("Calibrate North")
            }

        }
    }
}

@Composable
fun NavigationInfoPanel(
    steps: Int,
    currentHeading: Float,
    navigationInstructions: String,
    hazardWarning: String?,
    nearestLandmark: Pair<String, Float>?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            // Steps and heading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Steps: $steps",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Heading: ${"%.1f".format(currentHeading)}°",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation instructions
            if (navigationInstructions.isNotEmpty()) {
                Text(
                    text = navigationInstructions,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Hazard warning
            hazardWarning?.let { warning ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Nearest landmark
            nearestLandmark?.let { (name, distance) ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Near: $name (${"%.1f".format(distance)}m)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
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
                    text = "X: ${"%.2f".format(x)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Y: ${"%.2f".format(y)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                confidence?.let {
                    Text(
                        text = "Conf: ${"%.1f".format(it * 100)}%",
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