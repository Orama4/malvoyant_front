package com.example.malvoayant.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.data.models.DoorWindow
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.POI
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.data.models.Room
import com.example.malvoayant.data.models.Wall
import com.example.malvoayant.data.models.Zone
import com.example.malvoayant.data.viewmodels.NavigationViewModel
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
import com.example.malvoayant.ui.theme.PlusJakartaSans
import kotlin.math.*
import kotlin.random.Random
import com.example.malvoayant.NavigationLogic.utils.calculateDistance
import com.example.malvoayant.utils.NavigationUtils
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FloorPlanCanvasView(
    floorPlanState: FloorPlanState,
    modifier: Modifier = Modifier,
    stepCounterViewModel: StepCounterViewModel = viewModel(),
    navigationViewModel: NavigationViewModel
) {

    val traversedPath by navigationViewModel.traversedPath.collectAsState()
    val pathPoints by stepCounterViewModel.pathPoints.observeAsState(listOf(Pair(0f, 0f)))
    val currentPosition by stepCounterViewModel.currentPositionLive.observeAsState(Pair(0f, 0f))
    val currentHeading by stepCounterViewModel.currentHeadingLive.observeAsState(0f)

    // 1️⃣ Mode global vs focus
    var globalView by remember { mutableStateOf(true) }

    // 2️⃣ États caméra - séparés pour chaque mode
    // Vue globale
    var globalScale by remember { mutableStateOf(floorPlanState.scale) }
    var globalOffset by remember { mutableStateOf(Offset(floorPlanState.offset.x, floorPlanState.offset.y)) }

    // Vue focus (zoom sur position)
    var focusScale by remember { mutableStateOf(6f) }
    var focusOffset by remember { mutableStateOf(Offset.Zero) }
    var focusRotation by remember { mutableStateOf(0f) }
    var isInitialFocus by remember { mutableStateOf(false) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Animation pour le chemin
    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 3000,
            easing = FastOutSlowInEasing
        )
    )

    // Palette de couleurs
    val wallColor = Color(0xFF2C3E50)
    val doorColor = Color(0xFFD68910)
    val windowColor = Color(0xFF5DADE2)
    val poiColor = Color(0xFFC0392B)
    val pathStartColor = Color(0xFF27AE60)
    val pathEndColor = Color(0xFFF1C40F)
    val pathGradientColors = listOf(pathStartColor, pathEndColor)
    val headingColor = Color(0xFFE74C3C)
    val roomColors = remember {
        mapOf(
            "living" to Color(0xFFF9F3E3),
            "bedroom" to Color(0xFFE8F3EC),
            "kitchen" to Color(0xFFF0E6EF),
            "bathroom" to Color(0xFFE1EEF5),
            "entrance" to Color(0xFFF2F2F2),
            "hallway" to Color(0xFFF4F0E6),
            "room" to Color(0xFFF1F8FB),
            "default" to Color(0xFFF5F5F5)
        )
    }

    LaunchedEffect(globalView, traversedPath) {
        if (!globalView && traversedPath.isNotEmpty()) {
            val lastPoint = traversedPath.last()
            val (cx, cy) = lastPoint

            val canvasCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

            // Offset pour centrer sur le dernier point
            focusOffset = Offset(
                x = canvasCenter.x - cx,
                y = canvasCenter.y - cy
            )

            isInitialFocus = true

            // Calculer une rotation si on a un point précédent (optionnel)
            if (traversedPath.size >= 2) {
                val prev = traversedPath[traversedPath.size - 2]
                val dx = lastPoint.x - prev.x
                val dy = lastPoint.y - prev.y
                focusRotation = -atan2(dx, dy) + PI.toFloat() / 2f
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFCFCFC),
                        Color(0xFFF5F7FA)
                    )
                )
            )
            .shadow(2.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    // Dans detectTransformGestures
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (globalView) {
                            // Vue globale
                            globalScale = (globalScale * zoom).coerceIn(0.1f, 5f)
                            globalOffset += pan
                        } else {
                            // Vue focus : marquer fin de l'initialisation dès le premier geste
                            isInitialFocus = false
                            focusScale = (focusScale * zoom).coerceIn(1f, 10f)
                            focusOffset += pan
                        }
                    }
                }
        ) {
            if (globalView) {
                // ===== VUE GLOBALE =====
                withTransform({
                    translate(globalOffset.x, globalOffset.y)
                    scale(globalScale, globalScale)
                }) {
                    drawContent(
                        floorPlanState = floorPlanState,
                        roomColors = roomColors,
                        wallColor = wallColor,
                        doorColor = doorColor,
                        windowColor = windowColor,
                        navigationViewModel = navigationViewModel,
                        progress = progress,
                        traversedPath = traversedPath,
                        pathPoints=pathPoints,
                        currentPosition=currentPosition,
                        currentHeading=currentHeading,
                        pathGradientColors=pathGradientColors,
                        headingColor=headingColor,
                    )
                }
            } else {
                // ===== VUE FOCUS =====
                val path = navigationViewModel.currentPath
                if (path != null && path.size >= 2) {
                    val (cx, cy) = path.first()
                    val canvasCenter = Offset(size.width/2f, size.height/2f)

                    withTransform({
                        scale(focusScale, focusScale)
                        translate(focusOffset.x, focusOffset.y)

                    })  {
                        drawContent(
                            floorPlanState = floorPlanState,
                            roomColors = roomColors,
                            wallColor = wallColor,
                            doorColor = doorColor,
                            windowColor = windowColor,
                            navigationViewModel = navigationViewModel,
                            progress = progress,
                            pathPoints=pathPoints,
                            traversedPath = traversedPath,
                        currentPosition=currentPosition,
                        currentHeading=currentHeading,
                        pathGradientColors=pathGradientColors,
                        headingColor=headingColor,
                        )
                    }
                }
            }
        }

        // Bouton toggle stylisé
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    globalView = !globalView
                    // Quand on revient à la vue globale, on recentre
                    if (globalView) {
                        globalScale = floorPlanState.scale
                        globalOffset = Offset(floorPlanState.offset.x, floorPlanState.offset.y)
                    }
                    else{
                        // Réinitialiser la vue focus
                        focusScale = 6f
                    }
                },
                containerColor = if (globalView) Color(0xFF0D1B2A) else Color(0xFFE76F00),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                AnimatedContent(
                    targetState = globalView,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with
                                fadeOut(animationSpec = tween(300))
                    }
                ) { isGlobal ->
                    if (isGlobal) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Focus view",
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Global view",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Indicateur de mode en haut
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = if (globalView) "Global view" else "Focus view",
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// Fonction helper pour dessiner tout le contenu
private fun DrawScope.drawContent(
    floorPlanState: FloorPlanState,
    roomColors: Map<String, Color>,
    wallColor: Color,
    doorColor: Color,
    windowColor: Color,
    navigationViewModel: NavigationViewModel,
    progress: Float,
    traversedPath: List<Point> = emptyList(),
    pathPoints: List<Pair<Float, Float>>,
    currentPosition: Pair<Float, Float>,
    currentHeading: Float,
    pathGradientColors: List<Color>,
    headingColor: Color
) {
    drawInfiniteGrid()

    // Draw rooms with subtle 3D effects and patterns
    drawElegantRooms(floorPlanState.rooms, roomColors)

    // Draw refined zones with better styling
    drawElegantZones(floorPlanState.zones)

    // Draw walls with more sophisticated 3D effect
    drawElegantWalls(floorPlanState.walls, wallColor)

    // Draw modern doors and windows with 3D details
    drawElegantDoorWindows(floorPlanState.doors, doorColor, isDoor = true)
    drawElegantDoorWindows(floorPlanState.windows, windowColor, isDoor = false)

    // Draw stylish POIs with category-specific icons
    drawElegantPOIs(floorPlanState.pois)
    drawElegantPath(floorPlanState.minPoint, pathPoints, currentPosition, currentHeading, pathGradientColors, headingColor)
    val obstacles = navigationViewModel.getDetectedObstacles()
    obstacles.forEach { obstacle ->
        // Dessiner l'obstacle comme un cercle rouge avec un X
        drawCircle(
            color = Color.Red,
            center = Offset(obstacle.x, obstacle.y),
            radius = 25f,
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = Color.Red.copy(alpha = 0.3f),
            center = Offset(obstacle.x, obstacle.y),
            radius = 25f
        )

        // Dessiner un X au centre
        val centerOffset = Offset(obstacle.x, obstacle.y)
        val size = 15f
        drawLine(
            color = Color.Red,
            start = Offset(centerOffset.x - size, centerOffset.y - size),
            end = Offset(centerOffset.x + size, centerOffset.y + size),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.Red,
            start = Offset(centerOffset.x + size, centerOffset.y - size),
            end = Offset(centerOffset.x - size, centerOffset.y + size),
            strokeWidth = 3f
        )
    }
    // Draw the current path
    navigationViewModel.currentPath?.let { path ->
        val angle = if (path.size > 1) {
            calculateAngleBetweenPoints(path[0], path[1])
        } else 0f
        drawAdvancedNavigationPath(
            path = path,
            currentPosition = Point(340.0f, 340.0f),
            progress = progress,
            angle = angle,
            density = this.density
        )
    }
    val remainingPath = navigationViewModel.currentPath?.let { path ->
        if (traversedPath.isNotEmpty()) {
            path.dropWhile { point ->
                traversedPath.any { calculateDistance(it, point) < 1.0 }
            }
        } else {
            path
        }
    }

    if (remainingPath != null && remainingPath.isNotEmpty()) {
        drawPath(
            points = remainingPath,
            color = Color.Blue,
            strokeWidth = 8f
        )
    }
    if (traversedPath.isNotEmpty()) {
        // Dessiner le chemin parcouru en gris
        drawPath(
            points = traversedPath,
            color = Color.Gray,
            strokeWidth = 8f
        )

        // Dessiner la position actuelle
        val currentPos = traversedPath.last()
        drawCircle(
            color = Color.Green,
            center = Offset(currentPos.x, currentPos.y),
            radius = 12f
        )
    }

}


private fun DrawScope.drawPath(
    points: List<Point>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )
}

private fun calculateDistance(a: Point, b: Point): Float {
    return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
}
// Extension pour calculer l'angle entre deux points
private fun calculateAngleBetweenPoints(p1: Point, p2: Point): Float {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return atan2(dy, dx)
}
private fun DrawScope.drawInfiniteGrid() {
    val gridSize = 50f // Size of each grid cell
    val gridColor = Color(0xFFE0E0E0) // Light gray color for the grid

    // Calculate the number of grid lines to draw based on the canvas size
    val width = size.width
    val height = size.height

    // Extend the grid beyond the canvas bounds to create an infinite effect
    val extendedWidth = width * 2
    val extendedHeight = height * 2

    // Calculate the starting and ending points for grid lines
    val startX = (-extendedWidth).toInt()
    val startY = (-extendedHeight).toInt()
    val endX = (extendedWidth).toInt()
    val endY = (extendedHeight).toInt()

    // Draw vertical grid lines
    for (x in startX..endX step gridSize.toInt()) {
        drawLine(
            color = gridColor,
            start = Offset(x.toFloat(), startY.toFloat()),
            end = Offset(x.toFloat(), endY.toFloat()),
            strokeWidth = 1f
        )
    }

    // Draw horizontal grid lines
    for (y in startY..endY step gridSize.toInt()) {
        drawLine(
            color = gridColor,
            start = Offset(startX.toFloat(), y.toFloat()),
            end = Offset(endX.toFloat(), y.toFloat()),
            strokeWidth = 1f
        )
    }
}


private fun DrawScope.drawElegantPath(
    minPoint: Point,
    pathPoints: List<Pair<Float, Float>>,
    currentPosition: Pair<Float, Float>,
    currentHeading: Float,
    gradientColors: List<Color>,
    headingColor: Color
) {
    // Always draw the current position and heading indicator, even if no steps taken yet
    val posX = currentPosition.first * 100 + minPoint.x
    val posY = currentPosition.second * 100 + minPoint.y

    // Draw current position with elegant styling
    // Pulsating outer glow effect
    for (i in 3 downTo 1) {
        drawCircle(
            color = headingColor.copy(alpha = 0.05f * i),
            radius = 24f + (i * 3),
            center = Offset(posX, posY)
        )
    }

    // Main position indicator
    drawCircle(
        color = Color.White,
        radius = 12f,
        center = Offset(posX, posY)
    )

    drawCircle(
        color = headingColor,
        radius = 8f,
        center = Offset(posX, posY)
    )

    // Draw elegant heading arrow - always show this regardless of movement
    val headingRad = Math.toRadians(currentHeading.toDouble())
    val arrowLength = 30.dp.toPx()  // Made longer for better visibility
    val arrowEndX = posX + arrowLength * cos(headingRad).toFloat()
    val arrowEndY = posY + arrowLength * sin(headingRad).toFloat()

    // Arrow shaft with gradient and increased width for visibility
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(headingColor, headingColor.copy(alpha = 0.7f)),
            start = Offset(posX, posY),
            end = Offset(arrowEndX, arrowEndY)
        ),
        start = Offset(posX, posY),
        end = Offset(arrowEndX, arrowEndY),
        strokeWidth = 3.5f  // Slightly thicker for better visibility
    )

    // Arrow head with enhanced size and visibility
    val arrowHeadLength = 10.dp.toPx()  // Larger arrow head
    val angle1 = headingRad + Math.PI * 3/4
    val angle2 = headingRad - Math.PI * 3/4

    val arrowHead1X = arrowEndX + arrowHeadLength * cos(angle1).toFloat()
    val arrowHead1Y = arrowEndY + arrowHeadLength * sin(angle1).toFloat()

    val arrowHead2X = arrowEndX + arrowHeadLength * cos(angle2).toFloat()
    val arrowHead2Y = arrowEndY + arrowHeadLength * sin(angle2).toFloat()

    // Draw arrow head with thicker stroke
    drawLine(
        color = headingColor,
        start = Offset(arrowEndX, arrowEndY),
        end = Offset(arrowHead1X, arrowHead1Y),
        strokeWidth = 3.5f
    )

    drawLine(
        color = headingColor,
        start = Offset(arrowEndX, arrowEndY),
        end = Offset(arrowHead2X, arrowHead2Y),
        strokeWidth = 3.5f
    )

    // Add a small indicator showing heading angle as text
    drawContext.canvas.nativeCanvas.drawText(
        String.format("%.0f°", currentHeading),
        arrowEndX + 5f,
        arrowEndY + 5f,
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#333333")
            textSize = 14f
            isFakeBoldText = true
            setShadowLayer(1.5f, 0.5f, 0.5f, android.graphics.Color.WHITE)
        }
    )

    // Only draw path if we have movement
    if (pathPoints.size > 1) {
        val path = Path()
        path.moveTo(pathPoints[0].first * 100 + minPoint.x, pathPoints[0].second * 100 + minPoint.y)

        // Create smooth path with bezier curves
        for (i in 1 until pathPoints.size) {
            val start = pathPoints[i - 1]
            val end = pathPoints[i]
            val controlX = (start.first + end.first) / 2
            val controlY = (start.second + end.second) / 2

            path.quadraticBezierTo(
                controlX * 100 + minPoint.x, controlY * 100 + minPoint.y,
                end.first * 100 + minPoint.x, end.second * 100 + minPoint.y
            )
        }

        // Draw path with elegant glow effect
        for (i in 5 downTo 1) {
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    start = Offset(pathPoints.first().first * 100 + minPoint.x, pathPoints.first().second * 100 + minPoint.y),
                    end = Offset(pathPoints.last().first * 100 + minPoint.x, pathPoints.last().second * 100 + minPoint.y),
                    colors = gradientColors
                ),
                style = Stroke(width = 4f + i * 1.2f, pathEffect = PathEffect.cornerPathEffect(12f)),
                alpha = 0.03f * i
            )
        }

        // Draw actual path with refined gradient
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                start = Offset(pathPoints.first().first * 100 + minPoint.x, pathPoints.first().second * 100 + minPoint.y),
                end = Offset(pathPoints.last().first * 100 + minPoint.x, pathPoints.last().second * 100 + minPoint.y),
                colors = gradientColors
            ),
            style = Stroke(width = 4f, pathEffect = PathEffect.cornerPathEffect(12f))
        )

        // Draw elegant waypoints
        pathPoints.forEachIndexed { index, point ->
            val centerX = point.first * 100 + minPoint.x
            val centerY = point.second * 100 + minPoint.y
            val isEndPoint = index == 0 || index == pathPoints.size - 1

            // Outer glow
            drawCircle(
                color = if (isEndPoint) gradientColors[if (index == 0) 0 else 1].copy(alpha = 0.2f) else Color(0x20000000),
                radius = if (isEndPoint) 16f else 12f,
                center = Offset(centerX, centerY)
            )

            // White circle
            drawCircle(
                color = Color.White,
                radius = if (isEndPoint) 8f else 6f,
                center = Offset(centerX, centerY),
                style = Fill
            )

            // Inner colored circle
            drawCircle(
                color = if (isEndPoint) gradientColors[if (index == 0) 0 else 1] else Color(0xFF78909C),
                radius = if (isEndPoint) 5f else 3f,
                center = Offset(centerX, centerY),
                style = Fill
            )
        }
    }
}

private fun DrawScope.drawElegantWalls(walls: List<Wall>, wallColor: Color) {
    walls.forEach { wall ->
        val shadowOffset = 2f

        // Wall shadow for 3D effect
        drawLine(
            color = Color(0x20000000),
            start = Offset(wall.start.x + shadowOffset, wall.start.y + shadowOffset),
            end = Offset(wall.end.x + shadowOffset, wall.end.y + shadowOffset),
            strokeWidth = wall.thickness + 2f,
            cap = StrokeCap.Round
        )

        // Main wall with gradient for 3D effect
        val wallAngle = atan2(wall.end.y - wall.start.y, wall.end.x - wall.start.x)
        val perpAngle = wallAngle + (Math.PI / 2).toFloat()
        val offsetX = cos(perpAngle) * 2
        val offsetY = sin(perpAngle) * 2

        // Wall gradient
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    wallColor.copy(alpha = 0.95f),
                    wallColor.copy(alpha = 0.8f)
                ),
                start = Offset(wall.start.x + offsetX.toFloat(), wall.start.y + offsetY.toFloat()),
                end = Offset(wall.start.x - offsetX.toFloat(), wall.start.y - offsetY.toFloat())
            ),
            start = Offset(wall.start.x, wall.start.y),
            end = Offset(wall.end.x, wall.end.y),
            strokeWidth = wall.thickness,
            cap = StrokeCap.Round
        )

        // Wall highlight
        val highlightWidth = wall.thickness * 0.25f
        val highlightOffset = wall.thickness * 0.25f
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(
                wall.start.x - highlightOffset * cos(wallAngle).toFloat(),
                wall.start.y - highlightOffset * sin(wallAngle).toFloat()
            ),
            end = Offset(
                wall.end.x - highlightOffset * cos(wallAngle).toFloat(),
                wall.end.y - highlightOffset * sin(wallAngle).toFloat()
            ),
            strokeWidth = highlightWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawElegantZones(zones: List<Zone>) {
    zones.forEach { zone ->
        if (zone.coords.isNotEmpty()) {
            val path = Path()
            path.moveTo(zone.coords[0].x, zone.coords[0].y)

            for (i in 1 until zone.coords.size) {
                path.lineTo(zone.coords[i].x, zone.coords[i].y)
            }

            path.close()

            // Parse fill color with fallback
            val fillColor = try {
                Color(android.graphics.Color.parseColor(zone.fill))
            } catch (e: IllegalArgumentException) {
                if (zone.zone_type == "danger") {
                    Color(0x20FF3B30) // Light red for danger zones
                } else {
                    Color(0x10007AFF) // Light blue for other zones
                }
            }

            // Parse stroke color with fallback
            val strokeColor = try {
                Color(android.graphics.Color.parseColor(zone.stroke))
            } catch (e: IllegalArgumentException) {
                if (zone.zone_type == "danger") {
                    Color(0xFFFF3B30) // Red for danger zones
                } else {
                    Color(0xFF007AFF) // Blue for other zones
                }
            }

            // Draw subtle shadow
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.05f),
                style = Fill,
                blendMode = BlendMode.SrcOver
            )

            // Draw fill with pattern overlay for texture
            drawPath(
                path = path,
                color = fillColor,
                style = Fill
            )

            // Add specific pattern for danger zones
            if (zone.zone_type == "danger") {
                clipPath(path) {
                    val stripePath = Path()
                    val stripeWidth = 1.5f
                    val stripeSpacing = 15f

                    // Create elegant diagonal pattern
                    for (i in -100..100) {
                        stripePath.moveTo(-1000f + i * stripeSpacing, -1000f)
                        stripePath.lineTo(1000f + i * stripeSpacing, 1000f)
                    }

                    // Draw stripes
                    drawPath(
                        path = stripePath,
                        color = Color(0xFFFF3B30).copy(alpha = 0.15f),
                        style = Stroke(width = stripeWidth),
                        blendMode = BlendMode.SrcAtop
                    )

                    // Add cross-hatch for emphasis
                    val crossStripePath = Path()
                    for (i in -100..100) {
                        crossStripePath.moveTo(1000f + i * stripeSpacing, -1000f)
                        crossStripePath.lineTo(-1000f + i * stripeSpacing, 1000f)
                    }

                    drawPath(
                        path = crossStripePath,
                        color = Color(0xFFFF3B30).copy(alpha = 0.15f),
                        style = Stroke(width = stripeWidth),
                        blendMode = BlendMode.SrcAtop
                    )
                }
            }

            // Draw elegant border
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = zone.strokeWidth,
                    pathEffect = PathEffect.cornerPathEffect(8f)
                )
            )

            // Draw zone name with elegant style
            if (zone.name.isNotEmpty() && zone.center.x != 0f && zone.center.y != 0f) {
                drawContext.canvas.nativeCanvas.drawText(
                    zone.name,
                    zone.center.x,
                    zone.center.y,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#333333")
                        textSize = 14f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(1.5f, 0.5f, 0.5f, android.graphics.Color.WHITE)
                    }
                )
            }
        }
    }
}


private fun DrawScope.drawElegantRooms(room: Room, roomColors: Map<String, Color>) {
    room.polygons.forEach { polygon ->
        if (polygon.coords.size >= 3) {
            val path = Path()
            path.moveTo(polygon.coords[0].x, polygon.coords[0].y)

            for (i in 1 until polygon.coords.size) {
                path.lineTo(polygon.coords[i].x, polygon.coords[i].y)
            }

            path.close()

            val roomType = polygon.type.lowercase()
            val baseColor = roomColors[roomType] ?: roomColors["default"]!!

            // Draw subtle shadow for 3D effect
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.05f),
                style = Fill
            )

            // Create elegant gradient for room fill
            val gradient = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    baseColor.copy(alpha = 0.9f)
                ),
                start = Offset(polygon.center.x - 120, polygon.center.y - 120),
                end = Offset(polygon.center.x + 120, polygon.center.y + 120)
            )

            // Draw room fill with gradient
            drawPath(
                path = path,
                brush = gradient,
                style = Fill
            )

            // Add subtle texture pattern based on room type
            clipPath(path) {
                when (roomType) {
                    "bedroom" -> drawElegantBedroomPattern(polygon.center)
                    "kitchen" -> drawElegantKitchenPattern(polygon.center)
                    "bathroom" -> drawElegantBathroomPattern(polygon.center)
                    "living" -> drawElegantLivingRoomPattern(polygon.center)
                }
            }

            // Draw room border with subtle effect
            drawPath(
                path = path,
                color = baseColor.darker(0.1f),
                style = Stroke(width = 1.5f, pathEffect = PathEffect.cornerPathEffect(6f))
            )

            // Draw room name with elegant style
            if (polygon.name.isNotEmpty() && polygon.center.x != 0f && polygon.center.y != 0f) {
                drawContext.canvas.nativeCanvas.drawText(
                    polygon.name,
                    polygon.center.x,
                    polygon.center.y,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#333333")
                        textSize = 14f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(1.5f, 0.5f, 0.5f, android.graphics.Color.WHITE)
                    }
                )
            }
        }
    }
}

private fun DrawScope.drawElegantBedroomPattern(center: Point) {
    // Draw subtle bed icon
    val pattern = Path()
    val bedWidth = 40f
    val bedHeight = 60f
    val bedX = center.x - bedWidth / 2
    val bedY = center.y - bedHeight / 2

    // Bed frame
    pattern.addRoundRect(
        RoundRect(
            left = bedX,
            top = bedY,
            right = bedX + bedWidth,
            bottom = bedY + bedHeight,
            cornerRadius = CornerRadius(5f, 5f)
        )
    )

    // Pillows
    pattern.addRoundRect(
        RoundRect(
            left = bedX + 5f,
            top = bedY + 5f,
            right = bedX + bedWidth / 2 - 2.5f,
            bottom = bedY + 20f,
            cornerRadius = CornerRadius(3f, 3f)
        )
    )

    pattern.addRoundRect(
        RoundRect(
            left = bedX + bedWidth / 2 + 2.5f,
            top = bedY + 5f,
            right = bedX + bedWidth - 5f,
            bottom = bedY + 20f,
            cornerRadius = CornerRadius(3f, 3f)
        )
    )

    // Draw pattern with very subtle color
    drawPath(
        path = pattern,
        color = Color(0x05000000),
        style = Fill
    )

    drawPath(
        path = pattern,
        color = Color(0x08000000),
        style = Stroke(width = 0.5f)
    )
}

private fun DrawScope.drawElegantKitchenPattern(center: Point) {
    // Draw subtle kitchen island and counters
    val pattern = Path()
    val islandWidth = 36f
    val islandHeight = 24f
    val islandX = center.x - islandWidth / 2
    val islandY = center.y - islandHeight / 2

    // Kitchen island
    pattern.addRoundRect(
        RoundRect(
            left = islandX,
            top = islandY,
            right = islandX + islandWidth,
            bottom = islandY + islandHeight,
            cornerRadius = CornerRadius(2f, 2f)
        )
    )

    // Sink circle
    pattern.addOval(
        Rect(
            left = center.x - 6f,
            top = islandY + 6f,
            right = center.x + 6f,
            bottom = islandY + 18f
        )
    )

    // Draw pattern with very subtle color
    drawPath(
        path = pattern,
        color = Color(0x05000000),
        style = Fill
    )

    drawPath(
        path = pattern,
        color = Color(0x08000000),
        style = Stroke(width = 0.5f)
    )
}

private fun DrawScope.drawElegantBathroomPattern(center: Point) {
    // Draw subtle bathroom fixtures
    val pattern = Path()
    val fixtureSize = 20f

    // Bathtub/shower
    pattern.addRoundRect(
        RoundRect(
            left = center.x - fixtureSize,
            top = center.y - fixtureSize,
            right = center.x + fixtureSize,
            bottom = center.y + fixtureSize / 2,
            cornerRadius = CornerRadius(8f, 8f)
        )
    )

    // Sink
    pattern.addOval(
        Rect(
            left = center.x - 8f,
            top = center.y + fixtureSize / 2 + 5f,
            right = center.x + 8f,
            bottom = center.y + fixtureSize / 2 + 20f
        )
    )

    // Draw pattern with very subtle color
    drawPath(
        path = pattern,
        color = Color(0x05000000),
        style = Fill
    )

    drawPath(
        path = pattern,
        color = Color(0x08000000),
        style = Stroke(width = 0.5f)
    )
}

private fun DrawScope.drawElegantLivingRoomPattern(center: Point) {
    // Draw subtle living room furniture
    val pattern = Path()

    // Sofa
    val sofaWidth = 50f
    val sofaHeight = 25f
    val sofaX = center.x - sofaWidth / 2
    val sofaY = center.y - 15f

    pattern.addRoundRect(
        RoundRect(
            left = sofaX,
            top = sofaY,
            right = sofaX + sofaWidth,
            bottom = sofaY + sofaHeight,
            cornerRadius = CornerRadius(8f, 8f)
        )
    )

    // Coffee table
    pattern.addOval(
        Rect(
            left = center.x - 15f,
            top = center.y + 20f,
            right = center.x + 15f,
            bottom = center.y + 40f
        )
    )

    // Draw pattern with very subtle color
    drawPath(
        path = pattern,
        color = Color(0x05000000),
        style = Fill
    )

    drawPath(
        path = pattern,
        color = Color(0x08000000),
        style = Stroke(width = 0.5f)
    )
}

private fun DrawScope.drawElegantDoorWindows(items: List<DoorWindow>, color: Color, isDoor: Boolean) {
    items.forEach { item ->
        rotate(degrees = item.angle, pivot = Offset(item.x, item.y)) {
            if (isDoor) {
                drawElegantDoor(item, color)
            } else {
                drawElegantWindow(item, color)
            }
        }
    }
}

private fun DrawScope.drawElegantDoor(item: DoorWindow, color: Color) {
    val doorWidth = item.size
    val doorThickness = item.thick

    // Draw door frame shadow
    drawRoundRect(
        color = Color(0x30000000),
        topLeft = Offset(item.x - doorWidth / 2 + 1.5f, item.y - doorThickness / 2 + 1.5f),
        size = Size(doorWidth, doorThickness),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 2f)
    )

    // Draw door frame with gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                color,
                color.darker(0.2f)
            ),
            start = Offset(item.x - doorWidth / 2, item.y - doorThickness / 2),
            end = Offset(item.x + doorWidth / 2, item.y + doorThickness / 2)
        ),
        topLeft = Offset(item.x - doorWidth / 2, item.y - doorThickness / 2),
        size = Size(doorWidth, doorThickness),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 2f)
    )

    // Draw door panel with subtle gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = 0.08f),
                color.copy(alpha = 0.15f)
            ),
            start = Offset(item.x - doorWidth / 2, item.y - doorThickness / 2),
            end = Offset(item.x + doorWidth / 2, item.y + doorThickness / 2)
        ),
        topLeft = Offset(item.x - doorWidth / 2, item.y - doorThickness / 2),
        size = Size(doorWidth, doorThickness),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Draw elegant door swing arc
    val arcPath = Path()
    val angleSign = item.angleSign
    val startAngle = if (angleSign > 0) 270f else 0f
    val sweepAngle = if (angleSign > 0) 90f else -90f

    arcPath.addArc(
        oval = Rect(
            left = item.x - doorWidth / 2,
            top = item.y - doorWidth / 2,
            right = item.x + doorWidth / 2,
            bottom = item.y + doorWidth / 2
        ),
        startAngleDegrees = startAngle,
        sweepAngleDegrees = sweepAngle
    )

    // Draw arc shadow
    drawPath(
        path = arcPath,
        color = Color(0x20000000),
        style = Stroke(width = 1.2f),
        alpha = 0.5f
    )

    // Draw arc with subtle gradient
    drawPath(
        path = arcPath,
        brush = Brush.linearGradient(
            colors = listOf(color, color.copy(alpha = 0.7f)),
            start = Offset(item.x, item.y),
            end = Offset(
                item.x + (doorWidth / 2) * (if (angleSign > 0) 1 else -1),
                item.y + (doorWidth / 2) * (if (startAngle == 270f) -1 else 1)
            )
        ),
        style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
    )

    // Draw elegant doorknob
    val knobX = item.x + (doorWidth / 5) * (if (angleSign > 0) 1 else -1)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.lighter(0.3f),
                color.darker(0.3f)
            ),
            center = Offset(knobX, item.y),
            radius = 4f
        ),
        radius = 2.5f,
        center = Offset(knobX, item.y)
    )

    // Draw highlight on doorknob
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = 1f,
        center = Offset(knobX - 0.5f, item.y - 0.5f)
    )
}

private fun DrawScope.drawElegantWindow(item: DoorWindow, color: Color) {
    val windowWidth = item.size
    val windowThickness = item.thick

    // Draw window frame shadow
    drawRoundRect(
        color = Color(0x30000000),
        topLeft = Offset(item.x - windowWidth / 2 + 1.5f, item.y - windowThickness / 2 + 1.5f),
        size = Size(windowWidth, windowThickness),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 2f)
    )

    // Draw window frame with gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                color,
                color.darker(0.2f)
            ),
            start = Offset(item.x - windowWidth / 2, item.y - windowThickness / 2),
            end = Offset(item.x + windowWidth / 2, item.y + windowThickness / 2)
        ),
        topLeft = Offset(item.x - windowWidth / 2, item.y - windowThickness / 2),
        size = Size(windowWidth, windowThickness),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 2f)
    )

    // Draw window glass with subtle reflection gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xD0F5F9FC), // Light blue-white with transparency for glass effect
                Color(0xD0E1F5FE)
            ),
            start = Offset(item.x - windowWidth / 2, item.y - windowThickness / 2),
            end = Offset(item.x + windowWidth / 2, item.y + windowThickness / 2)
        ),
        topLeft = Offset(item.x - windowWidth / 2, item.y - windowThickness / 2),
        size = Size(windowWidth, windowThickness),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Draw window dividers
    val dividerPath = Path()

    // Draw vertical divider
    dividerPath.moveTo(item.x, item.y - windowThickness / 2)
    dividerPath.lineTo(item.x, item.y + windowThickness / 2)

    // Draw horizontal divider for larger windows
    if (windowWidth > 30) {
        dividerPath.moveTo(item.x - windowWidth / 2, item.y)
        dividerPath.lineTo(item.x + windowWidth / 2, item.y)
    }

    // Draw divider shadows
    drawPath(
        path = dividerPath,
        color = Color(0x30000000),
        style = Stroke(width = 1.5f)
    )

    // Draw dividers
    drawPath(
        path = dividerPath,
        color = color,
        style = Stroke(width = 1.2f)
    )

    // Draw reflection highlight
    val reflectionPath = Path()

    // Top-left to bottom-right reflection
    reflectionPath.moveTo(item.x - windowWidth / 2 + 2, item.y - windowThickness / 2 + 2)
    reflectionPath.lineTo(
        item.x - windowWidth / 4,
        item.y - windowThickness / 4
    )

    drawPath(
        path = reflectionPath,
        color = Color.White.copy(alpha = 0.5f),
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawElegantPOIs(pois: List<POI>) {
    pois.forEach { poi ->
        val poiX = poi.x
        val poiY = poi.y
        val poiSize = poi.width / 2

        // Draw shadow for 3D effect
        drawCircle(
            color = Color(0x30000000),
            radius = poiSize + 2,
            center = Offset(poiX + 1.5f, poiY + 1.5f)
        )

        // Draw POI based on category with elegant styling
        when (poi.category.lowercase()) {
            "furniture" -> drawElegantFurniturePOI(poiX, poiY, poiSize)
            "electronics" -> drawElegantElectronicsPOI(poiX, poiY, poiSize)
            "plant" -> drawElegantPlantPOI(poiX, poiY, poiSize)
            else -> drawElegantDefaultPOI(poiX, poiY, poiSize, Color(0xFFC0392B))
        }

        // Draw POI name with elegant styling
        if (poi.name.isNotEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                poi.name,
                poiX,
                poiY + poiSize + 15f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#333333")
                    textSize = 12f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    setShadowLayer(1.5f, 0.5f, 0.5f, android.graphics.Color.WHITE)
                }
            )
        }
    }
}

private fun DrawScope.drawElegantFurniturePOI(x: Float, y: Float, size: Float) {
    // Elegant furniture icon (sophisticated table)
    val tableColor = Color(0xFF8D6E63)

    // Table top with 3D effect
    val tablePath = Path()
    tablePath.addRoundRect(
        RoundRect(
            left = x - size,
            top = y - size / 2,
            right = x + size,
            bottom = y,
            cornerRadius = CornerRadius(3f, 3f)
        )
    )

    // Table legs
    // Left front leg
    tablePath.addRect(
        Rect(
            left = x - size + size / 5,
            top = y,
            right = x - size + size / 2,
            bottom = y + size / 2
        )
    )

    // Right front leg
    tablePath.addRect(
        Rect(
            left = x + size / 2,
            top = y,
            right = x + size - size / 5,
            bottom = y + size / 2
        )
    )

    // Draw table shadow
    drawPath(
        path = tablePath,
        color = Color(0x40000000),
        style = Fill,
        alpha = 0.5f
    )

    // Draw table with subtle gradient
    drawPath(
        path = tablePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                tableColor.lighter(0.1f),
                tableColor.darker(0.1f)
            )
        ),
        style = Fill
    )

    // Draw outline for definition
    drawPath(
        path = tablePath,
        color = tableColor.darker(0.2f),
        style = Stroke(width = 0.8f)
    )

    // Draw highlight on top edge of table
    drawLine(
        color = Color.White.copy(alpha = 0.4f),
        start = Offset(x - size + 3, y - size / 2 + 1),
        end = Offset(x + size - 3, y - size / 2 + 1),
        strokeWidth = 0.8f
    )
}

private fun DrawScope.drawElegantElectronicsPOI(x: Float, y: Float, size: Float) {
    // Modern device/computer icon
    val deviceColor = Color(0xFF546E7A)

    // Draw device body with 3D effect
    val devicePath = Path()

    // Main body
    devicePath.addRoundRect(
        RoundRect(
            left = x - size,
            top = y - size,
            right = x + size,
            bottom = y + size / 3,
            cornerRadius = CornerRadius(3f, 3f)
        )
    )

    // Base/stand
    devicePath.addRoundRect(
        RoundRect(
            left = x - size / 3,
            top = y + size / 3,
            right = x + size / 3,
            bottom = y + size / 1.5f,
            cornerRadius = CornerRadius(2f, 2f)
        )
    )

    // Draw device shadow
    drawPath(
        path = devicePath,
        color = Color(0x40000000),
        style = Fill,
        alpha = 0.5f
    )

    // Draw device with gradient
    drawPath(
        path = devicePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                deviceColor.lighter(0.1f),
                deviceColor.darker(0.2f)
            )
        ),
        style = Fill
    )

    // Draw outline for definition
    drawPath(
        path = devicePath,
        color = deviceColor.darker(0.3f),
        style = Stroke(width = 0.8f)
    )

    // Draw screen
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF263238),
                Color(0xFF37474F)
            ),
            start = Offset(x - size * 0.8f, y - size * 0.8f),
            end = Offset(x + size * 0.8f, y)
        ),
        topLeft = Offset(x - size * 0.8f, y - size * 0.8f),
        size = Size(size * 1.6f, size),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Screen highlights/reflections
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(x - size * 0.8f + 2, y - size * 0.8f + 2),
        end = Offset(x - size * 0.3f, y - size * 0.5f),
        strokeWidth = 0.8f
    )
}

private fun DrawScope.drawElegantPlantPOI(x: Float, y: Float, size: Float) {
    // Elegant plant/pot icon
    val potColor = Color(0xFFBF8757)
    val leafColor = Color(0xFF2E7D32)

    // Draw pot with 3D effect
    val potPath = Path()

    // Pot body (tapered)
    potPath.moveTo(x - size * 0.7f, y)
    potPath.lineTo(x - size * 0.8f, y + size)
    potPath.lineTo(x + size * 0.8f, y + size)
    potPath.lineTo(x + size * 0.7f, y)
    potPath.close()

    // Draw pot shadow
    drawPath(
        path = potPath,
        color = Color(0x40000000),
        style = Fill,
        alpha = 0.5f
    )

    // Draw pot with gradient
    drawPath(
        path = potPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                potColor.lighter(0.1f),
                potColor.darker(0.2f)
            )
        ),
        style = Fill
    )

    // Draw pot rim
    drawLine(
        color = potColor.darker(0.3f),
        start = Offset(x - size * 0.7f, y),
        end = Offset(x + size * 0.7f, y),
        strokeWidth = 1.2f
    )

    // Draw elegant plant leaves
    val leavesPath = Path()

    // Center stem
    leavesPath.moveTo(x, y)
    leavesPath.cubicTo(
        x, y - size / 2,
        x, y - size,
        x, y - size * 1.2f
    )

    // Left leaf
    leavesPath.moveTo(x, y - size / 2)
    leavesPath.cubicTo(
        x - size / 3, y - size / 2 - size / 4,
        x - size / 2, y - size,
        x - size / 4, y - size * 1.1f
    )

    // Right leaf
    leavesPath.moveTo(x, y - size / 2)
    leavesPath.cubicTo(
        x + size / 3, y - size / 2 - size / 4,
        x + size / 2, y - size,
        x + size / 4, y - size * 1.1f
    )

    // Additional small left leaf
    leavesPath.moveTo(x, y - size / 4)
    leavesPath.cubicTo(
        x - size / 4, y - size / 4,
        x - size / 2, y - size / 2,
        x - size / 3, y - size / 1.5f
    )

    // Additional small right leaf
    leavesPath.moveTo(x, y - size / 4)
    leavesPath.cubicTo(
        x + size / 4, y - size / 4,
        x + size / 2, y - size / 2,
        x + size / 3, y - size / 1.5f
    )

    // Draw leaves shadow
    drawPath(
        path = leavesPath,
        color = Color(0x30000000),
        style = Stroke(width = size / 4, cap = StrokeCap.Round),
        alpha = 0.3f
    )

    // Draw leaves with gradient
    drawPath(
        path = leavesPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                leafColor.lighter(0.2f),
                leafColor.darker(0.1f)
            )
        ),
        style = Stroke(width = size / 4, cap = StrokeCap.Round)
    )

    // Draw leaf highlights
    val highlightPath = Path()
    highlightPath.moveTo(x - size / 8, y - size / 2)
    highlightPath.cubicTo(
        x - size / 4, y - size / 2 - size / 8,
        x - size / 3, y - size / 1.2f,
        x - size / 6, y - size
    )

    drawPath(
        path = highlightPath,
        color = Color.White.copy(alpha = 0.3f),
        style = Stroke(width = size / 12, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawElegantDefaultPOI(x: Float, y: Float, size: Float, color: Color) {
    // Draw POI with 3D effect
    val poiPath = Path()

    // Create circle path
    poiPath.addOval(
        Rect(
            left = x - size,
            top = y - size,
            right = x + size,
            bottom = y + size
        )
    )

    // Draw shadow
    drawPath(
        path = poiPath,
        color = Color(0x40000000),
        style = Fill,
        alpha = 0.5f
    )

    // Draw POI with gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.lighter(0.2f),
                color
            ),
            center = Offset(x - size / 4, y - size / 4),
            radius = size * 1.4f
        ),
        radius = size,
        center = Offset(x, y)
    )

    // Draw highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = size / 3,
        center = Offset(x - size / 4, y - size / 4),
        style = Fill
    )

    // Draw subtle rim
    drawCircle(
        color = color.darker(0.2f),
        radius = size,
        center = Offset(x, y),
        style = Stroke(width = 1f)
    )
}

// Extension function to make a color darker
private fun Color.darker(factor: Float): Color {
    return Color(
        red = red * (1 - factor),
        green = green * (1 - factor),
        blue = blue * (1 - factor),
        alpha = alpha
    )
}

// Extension function to make a color lighter
private fun Color.lighter(factor: Float): Color {
    return Color(
        red = red + (1 - red) * factor,
        green = green + (1 - green) * factor,
        blue = blue + (1 - blue) * factor,
        alpha = alpha
    )
}


private fun DrawScope.drawAdvancedNavigationPath(
    path: List<Point>,
    currentPosition: Point,
    progress: Float,
    angle: Float,
    density: Float
) {
    if (path.isEmpty()) return

    // Configuration des couleurs
    val pathColor = Color(0xFF4A6179) // Bleu Google
    val shadowColor = Color(0x412C3E50)

    val pathToDraw = Path().apply {
        moveTo(path[0].x, path[0].y)
        path.drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
    }
    // 2. Ombre portée
    drawPath(
        path = pathToDraw,
        color = shadowColor,
        style = Stroke(
            width = 20f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        alpha = 0.2f
    )

    // 3. Tracé principal
    drawPath(
        path = pathToDraw,
        color = pathColor,
        style = Stroke(
            width = 10f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )


    // 5. Points de repère
    path.forEachIndexed { index, point ->
        when {
            index == 0 -> drawCircle( // Point de départ
                color = Color(0xFF203142), // Vert Google
                radius = 12f,
                center = Offset(point.x, point.y)
            )

            index ==path.size - 1 -> drawGooglePin( // Point d'arrivée
                position = Offset(point.x, point.y)
            )
        }
    }

}



// Icône de marqueur Google
private fun DrawScope.drawGooglePin(position: Offset) {
    // Base du pin (cercle rouge)
    drawCircle(
        color = Color(0xFFEA4335), // Rouge Google
        radius = 16f,
        center = position.copy(y = position.y - 8f) // Décalé vers le haut
    )

    // Pointe du pin (triangle vers le bas)
    drawPath(
        path = Path().apply {
            moveTo(position.x - 12f, position.y - 8f) // Point gauche
            lineTo(position.x + 12f, position.y - 8f) // Point droit
            lineTo(position.x, position.y + 16f)      // Pointe vers le bas
            close()
        },
        color = Color(0xFFEA4335)
    )

    // Point central (cercle blanc)
    drawCircle(
        color = Color.White,
        radius = 6f,
        center = position.copy(y = position.y - 8f) // Aligné avec la base
    )
}

