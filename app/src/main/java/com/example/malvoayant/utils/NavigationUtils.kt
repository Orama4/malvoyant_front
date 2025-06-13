package com.example.malvoayant.utils

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.data.models.POI
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.data.viewmodels.NavigationViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

object NavigationUtils {

    // Configurations
    private const val DEVIATION_THRESHOLD = 2.0 // meters
    private const val OBSTACLE_DETECTION_RANGE = 3.0 // meters
    private const val RECALCULATION_DELAY = 5000L // ms
    private const val STRAIGHT_DISTANCE_THRESHOLD = 0.5 // meters
    private const val TURNING_DISTANCE_THRESHOLD = 0.2 // meters

    // Navigation state
    private var isNavigating = false
    private var currentPosition: Point? = null
    private var currentInstructionIndex = 0
    private var currentPointIndex = 0
    private var traversedPath = mutableListOf<Point>()
    private var obstacleDetected = false
    private var lastPositionUpdateTime = 0L
    private var onPositionUpdatedCallback: ((Point) -> Unit)? = null
    private var onInstructionChangedCallback: ((Int) -> Unit)? = null
    private var onStopNavigationCallback: (() -> Unit)? = null
    private var navigationViewModel: NavigationViewModel? = null
    private var instructionGiven = false // Track if first instruction has been given
    private var lastMovementTime = 0L
    private var lastPosition: Point? = null
    private const val MOVEMENT_THRESHOLD = 0.3 // meters - minimum movement to detect walking

    fun startNavigation(
        start: Any,
        destination: Point,
        navigationViewModel: NavigationViewModel,
        onPositionUpdated: (Point) -> Unit,
        onInstructionChanged: (Int) -> Unit,
        onStopNavigation: () -> Unit
    ) {
        if (isNavigating) return
        val startPoint = when (start) {
            is Point -> start
            is POI -> Point(start.x, start.y)
            else -> throw IllegalArgumentException("Invalid start point type")
        }

        isNavigating = true
        currentPosition = navigationViewModel.currentPath?.firstOrNull() ?: startPoint
        traversedPath.clear()
        traversedPath.add(startPoint)
        currentInstructionIndex = 0
        currentPointIndex = 0
        obstacleDetected = false
        instructionGiven = false
        lastMovementTime = System.currentTimeMillis()
        lastPosition = startPoint

        // Store callbacks and viewModel for use in updatePosition
        this.onPositionUpdatedCallback = onPositionUpdated
        this.onInstructionChangedCallback = onInstructionChanged
        this.onStopNavigationCallback = onStopNavigation
        this.navigationViewModel = navigationViewModel

        // Start with the first instruction
        onInstructionChanged(currentInstructionIndex)
    }

    fun stopNavigation(path: List<Point>?) {
        isNavigating = false
        if (path != null) {
            currentInstructionIndex = path.size - 1
        }

        // ✅ Appeler le callback AVANT de le mettre à null
        onStopNavigationCallback?.invoke()

        onPositionUpdatedCallback = null
        onInstructionChangedCallback = null
        onStopNavigationCallback = null
        navigationViewModel = null
    }


    fun detectObstacle(position: Point) {
        obstacleDetected = true
        handleObstacle()
    }

    // Method to update position from step detection
    fun updatePosition(position: Point) {
        if (!isNavigating) return

        val path = navigationViewModel?.currentPath ?: return
        if (path.isEmpty()) return

        currentPosition = position
        traversedPath.add(position)
        navigationViewModel?.updateTraversedPath(position)
        onPositionUpdatedCallback?.invoke(position)

        // Check if user started walking (for first instruction)
        val currentTime = System.currentTimeMillis()
        if (!instructionGiven && lastPosition != null) {
            val movedDistance = calculateDistance(lastPosition!!, position)
            if (movedDistance > MOVEMENT_THRESHOLD) {
                onInstructionChangedCallback?.invoke(currentInstructionIndex) // OK
                instructionGiven = true
            }
        }

        lastPosition = position
        lastMovementTime = currentTime
        Log.d("NavigationUtils", "Position updated: $position")
        Log.d("NavigationUtils", "Current point index: $currentPointIndex")
        Log.d("NavigationUtils", "Current instruction index: $currentInstructionIndex")
        Log.d("NavigationUtils", "Path: ${path}")

        // Check if we've reached the next point in the path
        if (currentPointIndex < path.size - 1) {
            val nextPoint = path[currentPointIndex + 1]
            Log.d("NavigationUtils", "Next point: $nextPoint")
            Log.d("NavigationUtils", "Current position: $position")
            Log.d("NavigationUtils", "Distance to next point: ${calculateDistance(position, nextPoint)}")

            if (calculateDistance(position, nextPoint)/100 < STRAIGHT_DISTANCE_THRESHOLD) {
                if (shouldAdvanceInstruction(path[currentPointIndex], nextPoint)) {
                    currentInstructionIndex++
                    onInstructionChangedCallback?.invoke(currentInstructionIndex)
                }
                currentPointIndex++
            }

        }

        // Check if we've reached the destination
        if (currentPointIndex >= path.size - 1 ||
            calculateDistance(position, path.last()) / 100 < STRAIGHT_DISTANCE_THRESHOLD) {

            stopNavigation(path)
            Log.d("NavigationUtils", "Reached destination")
            return
        }


        // Check for deviation
        if (isDeviatingFromPath()) {
            handleDeviation()
        }

        // Check for obstacle (implemented elsewhere)
        if (obstacleDetected) {
            handleObstacle()
        }
    }

    private fun shouldAdvanceInstruction(currentPoint: Point, nextPoint: Point): Boolean {
        val isStraightInstruction = currentInstructionIndex % 2 == 0

        return if (isStraightInstruction) {
            // For straight instructions (even index), advance when close to next point
            calculateDistance(currentPosition!!, nextPoint)/100 < STRAIGHT_DISTANCE_THRESHOLD
        } else {
            // For turning instructions (odd index), advance when leaving current point
            calculateDistance(currentPosition!!, currentPoint)/100 > TURNING_DISTANCE_THRESHOLD

        }
    }

    private fun isDeviatingFromPath(): Boolean {
        val path = traversedPath
        if (path.size < 2) return false

        // Simple deviation check: distance to last path segment
        val lastSegmentStart = path[path.size - 2]
        val lastSegmentEnd = path.last()
        val distanceToSegment = calculateDistanceToSegment(
            currentPosition!!,
            lastSegmentStart,
            lastSegmentEnd
        )

        return distanceToSegment > DEVIATION_THRESHOLD
    }

    private fun calculateDistance(a: Point, b: Point): Double {
        return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2).toDouble())
    }

    private fun calculateDistanceToSegment(point: Point, segmentStart: Point, segmentEnd: Point): Double {
        val segmentLength = calculateDistance(segmentStart, segmentEnd)
        if (segmentLength == 0.0) return calculateDistance(point, segmentStart)

        val t = ((point.x - segmentStart.x) * (segmentEnd.x - segmentStart.x) +
                (point.y - segmentStart.y) * (segmentEnd.y - segmentStart.y)) /
                (segmentLength * segmentLength)

        val clampedT = t.coerceIn(0.0, 1.0)
        val projection = Point(
            x = (segmentStart.x + clampedT * (segmentEnd.x - segmentStart.x)).toFloat(),
            y = (segmentStart.y + clampedT * (segmentEnd.y - segmentStart.y)).toFloat()
        )

        return calculateDistance(point, projection)
    }

    private fun handleDeviation() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPositionUpdateTime < RECALCULATION_DELAY) return

        lastPositionUpdateTime = currentTime
        currentPosition?.let { currentPos ->
            // Recalculate path from current position
            navigationViewModel?.calculatePath(
                start = currentPos,
                destination = navigationViewModel?.currentPath?.last() ?: return@let
            )

            // Reset navigation state
            traversedPath.clear()
            traversedPath.add(currentPos)
            currentInstructionIndex = 0
            currentPointIndex = 0
            instructionGiven = false
        }
    }

    private fun handleObstacle() {
        obstacleDetected = false
        currentPosition?.let { currentPos ->
            // Recalculate path avoiding obstacle
            navigationViewModel?.calculatePath(
                start = currentPos,
                destination = navigationViewModel?.currentPath?.last() ?: return@let
            )

            // Reset navigation state
            traversedPath.clear()
            traversedPath.add(currentPos)
            currentInstructionIndex = 0
            currentPointIndex = 0
            instructionGiven = false
        }
    }

    fun getTraversedPath(): List<Point> = traversedPath.toList()
    fun getCurrentInstructionIndex(): Int = currentInstructionIndex
    fun isNavigationActive(): Boolean = isNavigating
}