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
    private val detectedObstacles = mutableListOf<Point>()
    fun getDetectedObstacles(): List<Point> = detectedObstacles.toList()
    // Configurations
    private const val DEVIATION_THRESHOLD = 2.0 // meters
    private const val OBSTACLE_DETECTION_RANGE = 3.0 // meters
    private const val RECALCULATION_DELAY = 5000L // ms
    private const val STRAIGHT_DISTANCE_THRESHOLD = 0.3 // meters
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





    fun handleObstacle(position: Point) : String{
        val obstaclePosition = Point(
            x = position.x + 200f,
            y = position.y
        )
        if (!detectedObstacles.any { calculateDistance(it, obstaclePosition) < 50f }) {
            detectedObstacles.add(obstaclePosition)

            currentPosition?.let { currentPos ->
                val currentPath = navigationViewModel?.currentPath
                if (currentPath == null || currentPath.isEmpty()) {
                    return "Obstacle détecté mais aucun itinéraire actif."
                }

                val isNewObstacleOnPath = isSpecificObstacleOnCalculatedPath(
                    obstacle = obstaclePosition,
                    path = currentPath
                )

                if (!isNewObstacleOnPath) {
                    return "Obstacle détecté à 2 mètres sur le côté. Continuez votre chemin."
                } else {
                    Log.d("OBSTACLE", "Nouvel obstacle sur le chemin - recalcul nécessaire")

                    navigationViewModel?.recalculatePathForObstacle(
                        start = currentPos,
                        destination = currentPath.last()
                    )

                    traversedPath.clear()
                    traversedPath.add(currentPos)
                    currentInstructionIndex = 0
                    currentPointIndex = 0
                    instructionGiven = false

                    return "Obstacle détecté sur votre chemin à 2 mètres. Recalcul de l'itinéraire en cours."
                }
            }
        }
        return "Obstacle déjà détecté dans cette zone."
    }

    private fun isSpecificObstacleOnCalculatedPath(
        obstacle: Point,
        path: List<Point>
    ): Boolean {
        val maxDistanceToPath = 10f // 1 mètre en unités du système

        for (i in 0 until path.size - 1) {
            val segmentStart = path[i]
            val segmentEnd = path[i + 1]

            val distanceToSegment = calculateDistanceToSegment(
                obstacle,
                segmentStart,
                segmentEnd
            )

            if (distanceToSegment <= maxDistanceToPath) {
                Log.d("OBSTACLE", "Obstacle détecté sur le segment $i du chemin (distance: $distanceToSegment)")
                return true
            }
        }

        Log.d("OBSTACLE", "Obstacle pas sur le chemin calculé")
        return false
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

        // 1) avancer currentPointIndex si on passe un point
        if (currentPointIndex < path.size - 1) {
            val next = path[currentPointIndex + 1]
            if (calculateDistance(position, next) / 100 < STRAIGHT_DISTANCE_THRESHOLD) {
                currentPointIndex++
            }
        }

// 2) recalculer l’instruction en “intelligent”
        updateInstructionIndex(position, path)

        // 3) détection d’arrivée
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


    fun getTraversedPath(): List<Point> = traversedPath.toList()
    fun getCurrentInstructionIndex(): Int = currentInstructionIndex
    fun isNavigationActive(): Boolean = isNavigating

    private fun updateInstructionIndex(position: Point, path: List<Point>) {
        val eps = TURNING_DISTANCE_THRESHOLD
        var bestInstr = currentInstructionIndex
        var minDist = Double.MAX_VALUE

        // Pour chaque segment [path[i], path[i+1]] on calcule
        //   - t_i (paramètre de projection)
        //   - distance perpendiculaire dist_i
        // puis on garde l’indice i du segment qui minimise dist_i
        for (i in 0 until path.size - 1) {
            val A = path[i]
            val B = path[i + 1]
            val vx = B.x - A.x
            val vy = B.y - A.y
            val segLen = calculateDistance(A, B)
            if (segLen == 0.0) continue

            // projection scalaire t_i
            val t = ((position.x - A.x) * vx + (position.y - A.y) * vy) / (segLen * segLen)
            // point projeté
            val projX = A.x + (t * vx).toFloat()
            val projY = A.y + (t * vy).toFloat()
            val dist = calculateDistance(position, Point(projX, projY))

            if (dist < minDist) {
                minDist = dist
                // calcul de l’instruction
                val alpha = (eps / segLen).toDouble()
                bestInstr = when {
                    t < alpha -> if (i > 0) 2*i - 1 else 2*i         // turning début
                    t > 1 - alpha -> 2*(i + 1) - 1                     // turning fin
                    else -> 2*i                                       // straight
                }
            }
        }

        // bornes
        if (bestInstr < 0) bestInstr = 0

        // on débloque seulement si changement
        if (bestInstr != currentInstructionIndex) {
            currentInstructionIndex = bestInstr
            onInstructionChangedCallback?.invoke(bestInstr)
        }
    }



}