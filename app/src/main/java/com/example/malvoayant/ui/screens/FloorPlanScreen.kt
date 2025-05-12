package com.example.malvoayant.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Navigation state
    val isConnected by stepCounterViewModel.isConnected.observeAsState(false)
    val steps by stepCounterViewModel.steps.observeAsState(0)
    val currentHeading by stepCounterViewModel.currentHeadingLive.observeAsState(0f)
    val navigationInstructions by stepCounterViewModel.navigationInstructions.observeAsState("")
    val hazardWarning by stepCounterViewModel.hazardWarning.observeAsState(null)
    val nearestLandmark by stepCounterViewModel.nearestLandmark.observeAsState(null)
    val calibrationState by stepCounterViewModel.calibrationState.observeAsState(StepCounterViewModel.CalibrationState.IDLE)

    // Position data
    val wifiPosition by stepCounterViewModel.wifiPositionLive.observeAsState(null)
    val lastWifiUpdateAgo by stepCounterViewModel.lastWifiUpdateAgo.observeAsState("Never")
    val wifiConfidence by stepCounterViewModel.wifiConfidence.observeAsState(0f)
    val fusedPosition by stepCounterViewModel.fusedPositionLive.observeAsState(Pair(0f, 0f))

    // File picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { floorPlanViewModel.importFromGeoJSONUri(context, it) }
    }

    // Effects
    LaunchedEffect(floorPlanState.navigationSettings) {
        floorPlanState.navigationSettings?.let {
            stepCounterViewModel.updateNavigationSettings(it)
        }
    }

    LaunchedEffect(Unit) {
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

        // Connection status panel
        ConnectionStatusPanel(
            isConnected = isConnected,
            onConnectToggle = {
                if (isConnected) stepCounterViewModel.stopListening()
                else stepCounterViewModel.startListening()
            },
            fusedPosition = fusedPosition,
            wifiPosition = wifiPosition,
            wifiConfidence = wifiConfidence,
            lastWifiUpdateAgo = lastWifiUpdateAgo
        )

        // Floor plan canvas
        FloorPlanCanvasView(
            floorPlanState = floorPlanState,
            stepCounterViewModel = stepCounterViewModel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(4.dp)
        )

        // Calibration controls
        CalibrationControls(
            calibrationState = calibrationState,
            onStartCalibration = { stepCounterViewModel.startXAxisCalibration() },
            onSetXAxis = { stepCounterViewModel.completeXAxisCalibration() },
            onSetYAxis = { stepCounterViewModel.completeYAxisCalibration() },
            onResetCalibration = { stepCounterViewModel.resetCalibration() },
            onResetPath = { stepCounterViewModel.resetAll() }
        )

        // Orientation quick-set buttons
        OrientationQuickSetButtons(
            onSetEast = { stepCounterViewModel.calibrateOrientation(0f) },
            onSetSouth = { stepCounterViewModel.calibrateOrientation(90f) }
        )
    }
}

@Composable
private fun ConnectionStatusPanel(
    isConnected: Boolean,
    onConnectToggle: () -> Unit,
    fusedPosition: Pair<Float, Float>,
    wifiPosition: Pair<Float, Float>?,
    wifiConfidence: Float,
    lastWifiUpdateAgo: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    color = if (isConnected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )

                Button(onClick = onConnectToggle) {
                    Text(if (isConnected) "Disconnect" else "Connect")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
}

@Composable
private fun CalibrationControls(
    calibrationState: StepCounterViewModel.CalibrationState,
    onStartCalibration: () -> Unit,
    onSetXAxis: () -> Unit,
    onSetYAxis: () -> Unit,
    onResetCalibration: () -> Unit,
    onResetPath: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calibration buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            when (calibrationState) {
                StepCounterViewModel.CalibrationState.IDLE -> {
                    Button(onClick = onStartCalibration) {
                        Text("Start Calibration")
                    }
                }
                StepCounterViewModel.CalibrationState.CALIBRATING_X -> {
                    Button(onClick = onSetXAxis) {
                        Text("Set X-Axis")
                    }
                }
                StepCounterViewModel.CalibrationState.CALIBRATING_Y -> {
                    Button(onClick = onSetYAxis) {
                        Text("Set Y-Axis")
                    }
                }
                StepCounterViewModel.CalibrationState.CALIBRATED -> {
                    Button(onClick = onResetCalibration) {
                        Text("Reset Calibration")
                    }
                }
            }

            Button(onClick = onResetPath) {
                Text("Reset Path")
            }
        }

        // Calibration instructions
        Text(
            text = when (calibrationState) {
                StepCounterViewModel.CalibrationState.CALIBRATING_X ->
                    "Walk forward in the direction you want as X-axis, then press 'Set X-Axis'"
                StepCounterViewModel.CalibrationState.CALIBRATING_Y ->
                    "Walk right to set Y-axis (perpendicular to X), then press 'Set Y-Axis'"
                StepCounterViewModel.CalibrationState.CALIBRATED ->
                    "Calibration complete. System ready for navigation."
                else -> "Press 'Start Calibration' to begin"
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun OrientationQuickSetButtons(
    onSetEast: () -> Unit,
    onSetSouth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onSetEast) {
            Text("Set East (0°)")
        }

        Button(onClick = onSetSouth) {
            Text("Set South (90°)")
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
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Steps: $steps", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Heading: ${"%.1f".format(currentHeading)}°",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (navigationInstructions.isNotEmpty()) {
                Text(
                    text = navigationInstructions,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            hazardWarning?.let { warning ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

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
                Text("X: ${"%.2f".format(x)}", style = MaterialTheme.typography.bodyMedium)
                Text("Y: ${"%.2f".format(y)}", style = MaterialTheme.typography.bodyMedium)
                confidence?.let {
                    Text("Conf: ${"%.1f".format(it * 100)}%", style = MaterialTheme.typography.bodyMedium)
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