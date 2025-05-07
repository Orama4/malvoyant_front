package com.example.malvoayant.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FloorPlanCanvasView(
    floorPlanState: FloorPlanState,
    modifier: Modifier = Modifier,
    viewModel: StepCounterViewModel = viewModel()
) {
    val pathPoints by viewModel.pathPoints.observeAsState(listOf(Pair(0f, 0f)))
    val currentPosition by viewModel.currentPositionLive.observeAsState(Pair(0f, 0f))
    val currentHeading by viewModel.currentHeadingLive.observeAsState(0f)

    var scale by remember { mutableStateOf(floorPlanState.scale) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(floorPlanState.offset.x, floorPlanState.offset.y)) }

    LaunchedEffect(floorPlanState.scale, floorPlanState.offset) {
        scale = floorPlanState.scale
        offset = Offset(floorPlanState.offset.x, floorPlanState.offset.y)
    }

    val wallColor = Color(0xFF333333)
    val doorColor = Color(0xFFAA7722)
    val windowColor = Color(0xFF6699CC)
    val poiColor = Color(0xFFCC3344)
    val pathColor = Color(0xFF4CAF50)

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
// Use the smaller scale factor to fit the floor plan within the canvas

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
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                        offset += pan
                    }
                }
        ) {
            translate(offset.x, offset.y) {
                scale(scale, scale) {
                    drawRooms(floorPlanState.rooms, roomColors)
                    drawWalls(floorPlanState.walls, wallColor)
                    drawDoorWindows(floorPlanState.doors, doorColor, isDoor = true)
                    drawDoorWindows(floorPlanState.windows, windowColor, isDoor = false)
                    drawPOIs(floorPlanState.pois, poiColor)
                    drawPath(floorPlanState.minPoint, pathPoints, currentPosition, currentHeading)
                }
            }
        }
    }
}
private fun DrawScope.drawPath(minPoint: Point, pathPoints: List<Pair<Float, Float>>, currentPosition: Pair<Float, Float>, currentHeading: Float) {
    val path = Path()

    if (pathPoints.size > 1) {
        path.moveTo(pathPoints[0].first * 50 + minPoint.x, pathPoints[0].second * 50 + minPoint.y)

        for (i in 1 until pathPoints.size) {
            val start = pathPoints[i - 1]
            val end = pathPoints[i]
            val control = Offset((start.first + end.first) / 2, (start.second + end.second) / 2)

            path.quadraticBezierTo(
                control.x * 50 + minPoint.x, control.y * 50 + minPoint.y,
                end.first * 50 + minPoint.x, end.second * 50 + minPoint.y
            )
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                start = Offset(pathPoints.first().first * 50 + minPoint.x, pathPoints.first().second * 50 + minPoint.y),
                end = Offset(pathPoints.last().first * 50 + minPoint.x, pathPoints.last().second * 50 + minPoint.y),
                colors = listOf(Color(0xFF4CAF50), Color(0xFFFFEB3B))
            ),
            style = Stroke(width = 5f)
        )

        pathPoints.forEach { point ->
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 8f,
                center = Offset(point.first * 50 + minPoint.x, point.second * 50 + minPoint.y)
            )
        }

        if (pathPoints.isNotEmpty()) {
            drawCircle(
                color = Color.Red,
                radius = 12f,
                center = Offset(currentPosition.first * 50 + minPoint.x, currentPosition.second * 50 + minPoint.y)
            )
        }

        val headingRad = Math.toRadians(currentHeading.toDouble())
        val arrowLength = 20.dp.toPx()
        drawLine(
            color = Color.Red,
            start = Offset(currentPosition.first * 50 + minPoint.x, currentPosition.second * 50 + minPoint.y),
            end = Offset(
                (currentPosition.first * 50 + minPoint.x) + arrowLength * cos(headingRad).toFloat(),
                (currentPosition.second * 50 + minPoint.y) + arrowLength * sin(headingRad).toFloat()
            ),
            strokeWidth = 3.dp.toPx()
        )
    }
}


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

private fun DrawScope.drawRooms(room: Room, roomColors: Map<String, Color>) {
    room.polygons.forEach { polygon ->
        if (polygon.coords.size >= 3) {
            val path = Path()
            path.moveTo(polygon.coords[0].x, polygon.coords[0].y)

            for (i in 1 until polygon.coords.size) {
                path.lineTo(polygon.coords[i].x, polygon.coords[i].y)
            }

            path.close()

            val roomType = polygon.type.lowercase()
            val fillColor = roomColors[roomType] ?: roomColors["default"]!!

            drawPath(
                path = path,
                color = fillColor.copy(alpha = 0.6f),
                style = Fill
            )

            drawPath(
                path = path,
                color = fillColor.copy(alpha = 0.8f),
                style = Stroke(width = 2f)
            )

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

private fun DrawScope.drawDoorWindows(items: List<DoorWindow>, color: Color, isDoor: Boolean) {
    items.forEach { item ->
        rotate(degrees = item.angle, pivot = Offset(item.x, item.y)) {
            if (isDoor) {
                val doorWidth = item.size
                val doorThickness = item.thick

                drawRect(
                    color = color,
                    topLeft = Offset(item.x - doorWidth / 2, item.y - doorThickness / 2),
                    size = Size(doorWidth, doorThickness),
                    style = Stroke(width = 2f)
                )

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
                val windowWidth = item.size
                val windowThickness = item.thick

                drawRect(
                    color = color,
                    topLeft = Offset(item.x - windowWidth / 2, item.y - windowThickness / 2),
                    size = Size(windowWidth, windowThickness),
                    style = Stroke(width = 2f)
                )

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

private fun DrawScope.drawPOIs(pois: List<POI>, color: Color) {
    pois.forEach { poi ->
        drawCircle(
            color = color,
            radius = poi.width / 2,
            center = Offset(poi.x, poi.y)
        )

        if (poi.name.isNotEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                poi.name,
                poi.x,
                poi.y + poi.height / 2 + 15f,
                android.graphics.Paint().apply {
                    this.color = android.graphics.Color.BLACK
                    textSize = 10f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
