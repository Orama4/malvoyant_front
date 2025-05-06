package com.example.malvoayant.ui.screens


import android.provider.CalendarContract.Colors
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FloorPlanCanvasView(
    floorPlanState: FloorPlanState,
    modifier: Modifier = Modifier,
    serverUrl: String = "ws://192.168.105.132:3000/",
    onScaleChange: (Float) -> Unit = {},
    onOffsetChange: (com.example.malvoayant.ui.screens.Offset) -> Unit = {}
) {
    val webSocketClient = remember { WebSocketClient(serverUrl) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Tracking user interactions for pan and zoom
    var scale by remember { mutableStateOf(floorPlanState.scale) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(
        floorPlanState.offset.x,
        floorPlanState.offset.y
    )) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    webSocketClient.connect()
                    Log.d("connceted ","connected in the page of floor plan ")

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
    // Update values when floorPlanState changes
    LaunchedEffect(floorPlanState.scale, floorPlanState.offset) {
        scale = floorPlanState.scale
        offset = Offset(floorPlanState.offset.x, floorPlanState.offset.y)
    }

    // Predefined colors for different elements
    val wallColor = Color(0xFF333333)
    val doorColor = Color(0xFFAA7722)
    val windowColor = Color(0xFF6699CC)
    val poiColor = Color(0xFFCC3344)
    val pathColor = Color(0xFF4CAF50)
    // Room colors mapping
    val roomColors = remember {
        mapOf(
            "living" to Color(0xFFF0DAAF),
            "bedroom" to Color(0xFFD4E2C6),
            "kitchen" to Color(0xFFDCC0DD),
            "bathroom" to Color(0xFFC6D8E4),
            "entrance" to Color(0xFFE0E0E0),
            "default" to Color(0xFFF0DAAF)
        )
    }

    // Define paint styles for different elements
    val wallPaint = Paint().asFrameworkPaint().apply {
        color = wallColor.toArgb()
        strokeWidth = 4f
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        style = android.graphics.Paint.Style.STROKE
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .background(Color.White)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Update scale (zoom)
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                        onScaleChange(scale)

                        // Update offset (pan)
                        offset += pan
                        onOffsetChange(com.example.malvoayant.ui.screens.Offset(offset.x, offset.y))
                    }
                }
        ) {
            // Apply transformations (scale and translate)
            translate(offset.x, offset.y) {
                scale(scale, scale) {
                    // Draw rooms
                        drawRooms(floorPlanState.rooms, roomColors)


                    // Draw walls
                        drawWalls(floorPlanState.walls, wallColor)


                    // Draw doors
                        drawDoorWindows(floorPlanState.doors, doorColor, isDoor = true)


                    // Draw windows
                        drawDoorWindows(floorPlanState.windows, windowColor, isDoor = false)


                    // Draw POIs
                        drawPOIs(floorPlanState.pois, poiColor)
                    drawPath(floorPlanState.minPoint, webSocketClient.pathPoints)
                }
            }
        }

//        // Add controls for zoom
//        Row(
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            FloatingActionButton(
//                onClick = {
//                    scale = (scale * 1.2f).coerceIn(0.1f, 5f)
//                    onScaleChange(scale)
//                },
//                containerColor = MaterialTheme.colorScheme.primary,
//                contentColor = MaterialTheme.colorScheme.onPrimary,
//                modifier = Modifier.size(40.dp)
//            ) {
//                Text("+")
//            }
//
//            FloatingActionButton(
//                onClick = {
//                    scale = (scale * 0.8f).coerceIn(0.1f, 5f)
//                    onScaleChange(scale)
//                },
//                containerColor = MaterialTheme.colorScheme.primary,
//                contentColor = MaterialTheme.colorScheme.onPrimary,
//                modifier = Modifier.size(40.dp)
//            ) {
//                Text("-")
//            }
//        }
    }
}

private fun DrawScope.drawPath(minPoint: Point, points: List<Offset>) {
    val path = Path()

    if (points.isNotEmpty()) {
        // Start path at the minPoint
        path.moveTo(minPoint.x, minPoint.y)

        // Connect all points, adjusting each by minPoint
        points.forEach { point ->
            path.lineTo(point.x + minPoint.x, point.y + minPoint.y)
        }

        // Draw the path
        drawPath(
            path = path,
            color = Color(0xFF4CAF50), // Green color for the path
            style = Stroke(width = 5f)
        )

        // Draw points (optional)
        points.forEach { point ->
            drawCircle(
                color = Color(0xFF2196F3), // Blue color
                radius = 8f,
                center = Offset(point.x + minPoint.x, point.y + minPoint.y)
            )
        }

        // Draw current position (most recent point)
        if (points.isNotEmpty()) {
            drawCircle(
                color = Color.Red,
                radius = 12f,
                center = Offset(points.last().x + minPoint.x, points.last().y + minPoint.y)
            )
        }
    }
}


// Extension function to draw walls
private fun DrawScope.drawWalls(walls: List<Wall>, wallColor: Color) {
    walls.forEach { wall ->
        drawLine(
            color = wallColor,
            start = Offset(wall.start.x, wall.start.y),
            end = Offset(wall.end.x, wall.end.y),
            strokeWidth = wall.thickness
        )
    }
}

// Extension function to draw rooms (polygons)
private fun DrawScope.drawRooms(room: Room, roomColors: Map<String, Color>) {
    room.polygons.forEach { polygon ->
        if (polygon.coords.size >= 3) {
            val path = Path()
            path.moveTo(polygon.coords[0].x, polygon.coords[0].y)

            for (i in 1 until polygon.coords.size) {
                path.lineTo(polygon.coords[i].x, polygon.coords[i].y)
            }

            // Close the path
            path.close()

            // Get room type or use default color
            val roomType = polygon.type.lowercase()
            val fillColor = roomColors[roomType] ?: roomColors["default"]!!

            // Draw filled polygon with semi-transparency
            drawPath(
                path = path,
                color = fillColor.copy(alpha = 0.6f),
                style = Fill
            )

            // Draw stroke
            drawPath(
                path = path,
                color = fillColor.copy(alpha = 0.8f),
                style = Stroke(width = 2f)
            )

            // Draw room name if available
            if (polygon.name.isNotEmpty() && polygon.center.x != 0f && polygon.center.y != 0f) {
                drawContext.canvas.nativeCanvas.drawText(
                    polygon.name,
                    polygon.center.x,
                    polygon.center.y,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 12f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

// Extension function to draw doors and windows
private fun DrawScope.drawDoorWindows(items: List<DoorWindow>, color: Color, isDoor: Boolean) {
    items.forEach { item ->
        // Draw based on item type and angle
        rotate(degrees = item.angle, pivot = Offset(item.x, item.y)) {
            if (isDoor) {
                // Draw a door symbol
                val doorWidth = item.size
                val doorThickness = item.thick

                // Draw door frame
                drawRect(
                    color = color,
                    topLeft = Offset(item.x - doorWidth / 2, item.y - doorThickness / 2),
                    size = Size(doorWidth, doorThickness),
                    style = Stroke(width = 2f)
                )

                // Draw door swing arc based on hinge position and angleSign
                val arcPath = Path()
                val angleSign = item.angleSign
                val startAngle = if (angleSign > 0) 270f else 0f
                val sweepAngle = if (angleSign > 0) 90f else -90f

                arcPath.addArc(
                    oval = androidx.compose.ui.geometry.Rect(
                        left = item.x - doorWidth / 2,
                        top = item.y - doorWidth / 2,
                        right = item.x + doorWidth / 2,
                        bottom = item.y + doorWidth / 2
                    ),
                    startAngleDegrees = startAngle,
                    sweepAngleDegrees = sweepAngle
                )

                drawPath(
                    path = arcPath,
                    color = color,
                    style = Stroke(width = 1.5f)
                )
            } else {
                // Draw a window symbol
                val windowWidth = item.size
                val windowThickness = item.thick

                // Draw window frame
                drawRect(
                    color = color,
                    topLeft = Offset(item.x - windowWidth / 2, item.y - windowThickness / 2),
                    size = Size(windowWidth, windowThickness),
                    style = Stroke(width = 2f)
                )

                // Draw window panes (crosshairs)
                drawLine(
                    color = color,
                    start = Offset(item.x, item.y - windowThickness / 2),
                    end = Offset(item.x, item.y + windowThickness / 2),
                    strokeWidth = 1f
                )
            }
        }
    }
}

// Extension function to draw POIs
private fun DrawScope.drawPOIs(pois: List<POI>, color: Color) {
    pois.forEach { poi ->
        // Draw POI icon (simple circle)
        drawCircle(
            color = color,
            radius = poi.width / 2,
            center = Offset(poi.x, poi.y)
        )

        // Draw POI name
        if (poi.name.isNotEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                poi.name,
                poi.x,
                poi.y + poi.height / 2 + 15f, // Position text below the icon
                android.graphics.Paint().apply {
                    this.color = android.graphics.Color.BLACK
                    textSize = 10f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
