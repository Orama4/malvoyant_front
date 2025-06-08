package com.example.malvoayant.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.items
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.data.models.POI
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.data.viewmodels.FloorPlanViewModel
import com.example.malvoayant.data.viewmodels.NavigationViewModel
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
import com.example.malvoayant.navigation.Screen
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    context: Context,
    navController: NavHostController,
    floorPlanViewModel: FloorPlanViewModel,
    stepCounterViewModel: StepCounterViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navigationViewModel: NavigationViewModel
) {
    val context = LocalContext.current
    val isConnected by stepCounterViewModel.isConnected.observeAsState(false)
    val floorPlan = floorPlanViewModel.floorPlanState
    val searchText = remember { mutableStateOf("") }
    val lastClickTime = remember { mutableStateOf(0L) }
    val doubleClickTimeWindow = 300L
    // Observe position data from Raspberry Pi
    val wifiPosition by stepCounterViewModel.wifiPositionLive.observeAsState(null)
    val lastWifiUpdateAgo by stepCounterViewModel.lastWifiUpdateAgo.observeAsState("Never")
    val wifiConfidence by stepCounterViewModel.wifiConfidence.observeAsState(0f)
    val errorMessage = navigationViewModel.errorMessage
    // Nouveaux états pour la sélection
    var startPoint by remember { mutableStateOf<POI?>(null) }
    var endPoint by remember { mutableStateOf<POI?>(null) }
    var showStartSelection by remember { mutableStateOf(false) }
    var showEndSelection by remember { mutableStateOf(false) }
    // Position actuelle (à remplacer par votre logique réelle)
    val currentPosition = remember { POI(x=310f, y=310f, name = "current") }



    // Remember the launcher for file picking
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            floorPlanViewModel.importFromGeoJSONUri(context, it)
        }
    }
    // Afficher l'erreur
    if (!errorMessage.isNullOrEmpty()) {
        AlertDialog(
            onDismissRequest = { navigationViewModel.clearError() },
            title = {
                Text(
                    text = "Erreur de navigation",
                    fontFamily = PlusJakartaSans,
                )
            },
            text = {
                Text(
                    text = errorMessage ?: "Erreur inconnue", // Le "?:" fournit une valeur par défaut
                    fontFamily = PlusJakartaSans,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { navigationViewModel.clearError() }
                ) {
                    Text("OK")
                }
            }
        )
    }
    val scope = rememberCoroutineScope()
    val pendingSpeechJob = remember { mutableStateOf<Job?>(null) }
    val speechHelper = remember { SpeechHelper(context) }
    // Accès correct avec .value quand pas de delegate
    val currentPath = navigationViewModel.currentPath
    val isLoading = navigationViewModel.isLoading

    // Animation states
    val pulsateAnimation = rememberInfiniteTransition(label = "pulsate")
    val scale = pulsateAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
// Observer les changements de chemin
    LaunchedEffect(navigationViewModel.currentPath) {
        Log.d("SearchScreen", "Chemin actuel: $currentPath")
    }
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            speechHelper.speak("Search page. You can search for points of interest. Use the buttons at the bottom for help, SOS, or repair services.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
        }
    }

    val filteredPois = remember(searchText.value, floorPlanViewModel.floorPlanState.pois) {
        floorPlanViewModel.floorPlanState.pois.filter { poi ->
            poi.name.contains(searchText.value, ignoreCase = true)
        }
    }

    LaunchedEffect(filteredPois) {
        floorPlanViewModel.setPois(filteredPois)

    }
    /*LaunchedEffect(floorPlan.pois) {
        if (floorPlan.pois.size >= 3 && !navigationViewModel.isLoading) {
            navigationViewModel.calculatePath(
                start = Point(310.0f, 310.0f),
                destination = floorPlan.pois[2]
                    ?: return@LaunchedEffect
            )
        }
    }*/
    // Calculer le chemin quand les points changent
    LaunchedEffect(startPoint, endPoint) {
        if (startPoint != null && endPoint != null) {
            if (startPoint!!.name == "current") {
                navigationViewModel.calculatePath(
                    start =Point(x= startPoint!!.x, y= startPoint!!.y),
                    destination = endPoint!!
                )
            }else{
            navigationViewModel.calculatePath(
                start = startPoint!!,
                destination = endPoint!!
            )
            }
        }
    }
    /*if (isLoading) {
        CircularProgressIndicator()
    } else {
        currentPath?.let { path ->
            PathVisualizer(path)
        }
    }*/

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFE9ECEF)
                    )
                )
            )
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            // Top app bar with back button and mic
            TopAppBar(
                navController = navController,
                speechHelper = speechHelper
            )

            // Nouveau: Sélecteurs de points
            PointSelectors(
                startPoint = startPoint,
                endPoint = endPoint,
                onStartClick = { showStartSelection = true },
                onEndClick = { showEndSelection = true },
                speechHelper = speechHelper
            )

            // Floor plan view
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(elevation = 8.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                FloorPlanCanvasView(
                    floorPlanState = floorPlanViewModel.floorPlanState,
                    modifier = Modifier.fillMaxSize(),
                    stepCounterViewModel = stepCounterViewModel,
                    navigationViewModel = navigationViewModel
                )
            }
            // Connection status and WiFi position display
            ConnectionStatusAndWiFiInfo(isConnected, wifiPosition.toString(), lastWifiUpdateAgo, wifiConfidence)

            // Bottom navigation buttons
            BottomNavigationButtons(
                navController = navController,
                lastClickTime = lastClickTime,
                doubleClickTimeWindow = doubleClickTimeWindow,
                pendingSpeechJob = pendingSpeechJob,
                speechHelper = speechHelper,
                scope = scope
            )
        }
        // Liste de sélection pour le départ
        if (showStartSelection) {
            PointSelectionDialog(
                title = "Choose starting point",
                points = listOf(currentPosition) + filteredPois,
                onSelect = {
                    startPoint = it
                    showStartSelection = false
                },
                onDismiss = { showStartSelection = false },
                speechHelper = speechHelper
            )
        }

        // Liste de sélection pour l'arrivée
        if (showEndSelection) {
            PointSelectionDialog(
                title = "Choose destination point",
                points = filteredPois,
                onSelect = {
                    endPoint = it
                    showEndSelection = false
                },
                onDismiss = { showEndSelection = false },
                speechHelper = speechHelper
            )
        }
    }
}


@Composable
private fun ConnectionStatusAndWiFiInfo(
    isConnected: Boolean,
    wifiPosition: String?,
    lastWifiUpdateAgo: String?,
    wifiConfidence: Float
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Display connection status
            Text(
                text = if (isConnected) "WebSocket: Connected" else "WebSocket: Disconnected",
                fontWeight = FontWeight.Bold
            )

            // WiFi position and confidence
            Text(
                text = "WiFi Position: $wifiPosition"
            )
            Text(
                text = "Last Update: $lastWifiUpdateAgo"
            )
            Text(
                text = "Confidence: ${"%.2f".format(wifiConfidence)}"
            )
        }
    }
}

@Composable
private fun TopAppBar(
    navController: NavHostController,
    speechHelper: SpeechHelper
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppColors.darkBlue,
                            AppColors.darkBlue.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowLeft,
                contentDescription = "Back",
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }

        Text(
            text = "Floor Map Explorer",
            color = AppColors.darkBlue,
            fontSize = 20.sp,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            onClick = {
                speechHelper.speak("This is the search page. You can search for points of interest. Use the search bar to find locations. The buttons at the bottom provide different services.")
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppColors.primary,
                            AppColors.primary.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.mic),
                contentDescription = "Voice Assistant",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SearchBox(
    searchText: MutableState<String>,
    speechHelper: SpeechHelper
) {
    Box(
        modifier = Modifier

            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(70.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AppColors.darkBlue.copy(alpha = 0.95f),
                        AppColors.darkBlue.copy(alpha = 0.9f)
                    ),
                    start = androidx. compose. ui. geometry. Offset(0f, 0f),
                    end = androidx. compose. ui. geometry. Offset(1000f, 0f)
                )
            )
            .clickable {
                speechHelper.speak("Search field. Click to search for points of interest.")
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            TextField(
                value = searchText.value,
                onValueChange = { searchText.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                placeholder = {
                    Text(
                        text = "Find your Point of Interest",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = AppColors.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BottomNavigationButtons(
    navController: NavHostController,
    lastClickTime: MutableState<Long>,
    doubleClickTimeWindow: Long,
    pendingSpeechJob: MutableState<Job?>,
    speechHelper: SpeechHelper,
    scope: CoroutineScope
) {
    // Animation states
    val buttonHoverStates = remember { mutableStateListOf(false, false, false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Helper Button
        AnimatedActionButton(
            text = "HELPER",
            icon = R.drawable.ic_phoen,
            description = "Helper",
            isHovered = buttonHoverStates[0],
            onHoverChange = { buttonHoverStates[0] = it },
            onClick = {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                    pendingSpeechJob.value?.cancel()
                    navController.navigate(Screen.Helper.route)
                } else {
                    pendingSpeechJob.value = scope.launch {
                        delay(doubleClickTimeWindow)
                        speechHelper.speak("Helper button, click to navigate to helper page.")
                    }
                }

                lastClickTime.value = currentTime
            }
        )

        // SOS Button
        AnimatedActionButton(
            text = "SOS",
            icon = R.drawable.mic,
            description = "SOS",
            isHovered = buttonHoverStates[1],
            onHoverChange = { buttonHoverStates[1] = it },
            isEmergency = true,
            onClick = {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                    pendingSpeechJob.value?.cancel()
                    navController.navigate(Screen.Helper.route)
                } else {
                    pendingSpeechJob.value = scope.launch {
                        delay(doubleClickTimeWindow)
                        speechHelper.speak("SOS button")
                    }
                }

                lastClickTime.value = currentTime
            }
        )

        // Repair Button
        AnimatedActionButton(
            text = "REPAIR",
            icon = R.drawable.ic_down,
            description = "Repair",
            isHovered = buttonHoverStates[2],
            onHoverChange = { buttonHoverStates[2] = it },
            onClick = {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                    pendingSpeechJob.value?.cancel()
                    navController.navigate("repair/CONNECTED?date=12.03.2025&time=17:00")
                } else {
                    pendingSpeechJob.value = scope.launch {
                        delay(doubleClickTimeWindow)
                        speechHelper.speak("Repair button")
                    }
                }

                lastClickTime.value = currentTime
            }
        )
    }
}

@Composable
private fun AnimatedActionButton(
    text: String,
    icon: Int,
    description: String,
    isHovered: Boolean,
    onHoverChange: (Boolean) -> Unit,
    isEmergency: Boolean = false,
    onClick: () -> Unit
) {
    // Animation
    val infiniteTransition = rememberInfiniteTransition(label = "buttonAnimation")
    val scale = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEmergency) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnimation"
    )

    val backgroundColor = if (isEmergency) {
        infiniteTransition.animateColor(
            initialValue = AppColors.primary,
            targetValue = Color(0xFFE74C3C),
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "colorAnimation"
        )
    } else {
        rememberUpdatedState(AppColors.primary)
    }

    Box(
        modifier = Modifier

            .fillMaxHeight()
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = if (isHovered) 10.dp else 6.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isEmergency) backgroundColor.value else AppColors.primary,
                        if (isEmergency) backgroundColor.value.copy(alpha = 0.8f) else AppColors.primary.copy(alpha = 0.8f)
                    ),
                    start = androidx. compose. ui. geometry. Offset(0f, 0f),
                    end = androidx. compose. ui. geometry. Offset(0f, 1000f)
                )
            )
            .clickable { onClick() }
            .graphicsLayer {
                if (isEmergency) {
                    scaleX = scale.value
                    scaleY = scale.value
                }
            }
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Add pulsating effect for emergency button
        if (isEmergency) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2

                drawCircle(
                    color = Color.White.copy(alpha = (1 - scale.value) * 0.3f),
                    radius = radius * scale.value,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun PointSelectors(
    startPoint: POI?,
    endPoint: POI?,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    speechHelper: SpeechHelper
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Sélecteur de départ
        PointSelector(
            label = "Start",
            selectedPoint = startPoint?.name ?: "Select...",
            onClick = {
                speechHelper.speak("Choosing starting point")
                onStartClick()
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Sélecteur d'arrivée
        PointSelector(
            label = "Destination",
            selectedPoint = endPoint?.name ?: "Select...",
            onClick = {
                speechHelper.speak("Choosing destination point")
                onEndClick()
            }
        )
    }
}

@Composable
fun PointSelector(
    label: String,
    selectedPoint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.darkBlue.copy(alpha = 0.9f))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans
                )
                Text(
                    text = selectedPoint,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = "Select",
                tint = Color.White,
                modifier = Modifier.size(16.dp) // Taille "small"
            )

        }
    }
}

@Composable
fun PointSelectionDialog(
    title: String,
    points: List<POI>,
    onSelect: (POI) -> Unit,
    onDismiss: () -> Unit,
    speechHelper: SpeechHelper
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column {
                // En-tête
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.darkBlue)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Liste des points
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(points) { point ->
                        PointListItem(
                            point = point,
                            onClick = {
                                speechHelper.speak("selected: ${point.name}")
                                onSelect(point)
                            }
                        )
                    }
                }

                // Bouton fermer
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary
                    )
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}

@Composable
fun PointListItem(
    point: POI,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                if (point.name == "current") R.drawable.ic_location
                else R.drawable.ic_location
            ),
            contentDescription = null,
            tint = if (point.name == "current") AppColors.primary else Color.Gray
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = point.name,
            fontSize = 16.sp,
            fontFamily = PlusJakartaSans,
            fontWeight = if (point.name == "current") FontWeight.Bold else FontWeight.Normal
        )
    }
}