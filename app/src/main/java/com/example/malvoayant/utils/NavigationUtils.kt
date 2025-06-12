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
    private const val STEP_SIMULATION_INTERVAL = 1000L // ms
    private const val STRAIGHT_DISTANCE_THRESHOLD = 1.0 // meters
    private const val TURNING_DISTANCE_THRESHOLD = 1.0 // meters

    // Navigation state
    private var isNavigating = false
    private var currentPosition: Point? = null
    private var currentInstructionIndex = 0
    private var currentPointIndex=0
    private var traversedPath = mutableListOf<Point>()
    private var simulationJob: Job? = null
    private var obstacleDetected = false
    private var lastPositionUpdateTime = 0L


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
        obstacleDetected = false

        // Start step simulation
        simulationJob?.cancel()
        simulationJob = navigationViewModel.viewModelScope.launch {
            while (isNavigating) {
                delay(STEP_SIMULATION_INTERVAL)
                simulateNextStep(navigationViewModel, onPositionUpdated, onInstructionChanged, onStopNavigation )
            }
        }
    }

    fun stopNavigation(path: List<Point>?) {
        isNavigating = false
        if (path != null) {
            currentInstructionIndex=path.size - 1
        }
        simulationJob?.cancel()
    }

    fun detectObstacle(position: Point) {
        obstacleDetected = true
    }

    private suspend fun simulateNextStep(
        navigationViewModel: NavigationViewModel,
        onPositionUpdated: (Point) -> Unit,
        onInstructionChanged: (Int) -> Unit,
        onStopNavigation: () -> Unit
    ) {
        val path = navigationViewModel.currentPath ?: return
        if (path.isEmpty()) return

        // Sécurité pour l'indice
        if (currentPointIndex >= path.size - 1) {
            stopNavigation(path)
            onStopNavigation()
            return
        }

        val currentPoint = path[currentPointIndex]
        val nextPoint = path[currentPointIndex + 1]

        // Avance vers le prochain point
        currentPosition = moveTowards(currentPosition!!, nextPoint, 4f)



        traversedPath.add(currentPosition!!)
        navigationViewModel.updateTraversedPath(currentPosition!!)
        onPositionUpdated(currentPosition!!)

        // Avancer dans les points
        if (calculateDistance(currentPosition!!, nextPoint) < 0.5) {
            currentPointIndex++
        }

        // Détection d'instruction atteinte
        if (shouldAdvanceInstruction(currentPoint, nextPoint)) {
            currentInstructionIndex++
            onInstructionChanged(currentInstructionIndex)
        }

        if (isDeviatingFromPath()) {
            handleDeviation(navigationViewModel)
        }

        if (obstacleDetected) {
            handleObstacle(navigationViewModel)
        }
    }


    private fun shouldAdvanceInstruction(currentPoint: Point, nextPoint: Point): Boolean {
        val isStraightInstruction = currentInstructionIndex % 2 == 0

        return if (isStraightInstruction) {
            // For straight instructions (even index), advance when close to next point
            calculateDistance(currentPosition!!, nextPoint) < STRAIGHT_DISTANCE_THRESHOLD
        } else {
            // For turning instructions (odd index), advance when leaving current point
            calculateDistance(currentPosition!!, currentPoint) > TURNING_DISTANCE_THRESHOLD
        }
    }

    private fun moveTowards(current: Point, target: Point, speed: Float): Point {
        val dx = target.x - current.x
        val dy = target.y - current.y
        val distance = sqrt(dx.pow(2) + dy.pow(2))

        return if (distance <= speed) {
            target
        } else {
            val ratio = speed / distance
            Point(
                x = current.x + dx * ratio,
                y = current.y + dy * ratio
            )
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

    private suspend fun handleDeviation(navigationViewModel: NavigationViewModel) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPositionUpdateTime < RECALCULATION_DELAY) return

        lastPositionUpdateTime = currentTime
        currentPosition?.let { currentPos ->
            // Recalculate path from current position
            navigationViewModel.calculatePath(
                start = currentPos,
                destination = navigationViewModel.currentPath?.last() ?: return@let
            )

            // Reset navigation state
            traversedPath.clear()
            traversedPath.add(currentPos)
            currentInstructionIndex = 0
        }
    }

    private suspend fun handleObstacle(navigationViewModel: NavigationViewModel) {
        obstacleDetected = false
        currentPosition?.let { currentPos ->
            // Recalculate path avoiding obstacle
            navigationViewModel.calculatePath(
                start = currentPos,
                destination = navigationViewModel.currentPath?.last() ?: return@let
            )

            // Reset navigation state
            traversedPath.clear()
            traversedPath.add(currentPos)
            currentInstructionIndex = 0
        }
    }

    fun getTraversedPath(): List<Point> = traversedPath.toList()
    fun getCurrentInstructionIndex(): Int = currentInstructionIndex
    fun isNavigationActive(): Boolean = isNavigating
}