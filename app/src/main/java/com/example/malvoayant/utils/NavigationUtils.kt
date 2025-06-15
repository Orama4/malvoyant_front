package com.example.malvoayant.utils

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.data.models.POI
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.data.viewmodels.NavigationViewModel
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
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
    private const val TURNING_DISTANCE_THRESHOLD = 0.5 // meters

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
    private var onDynamicInstructionCallback: ((String) -> Unit)? = null
    private var onStopNavigationCallback: (() -> Unit)? = null
    private var onDynamicInstruction: ((String) -> Unit)? = null
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
        onDynamicInstruction: (String) -> Unit = {},
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
        this.onDynamicInstructionCallback = onDynamicInstruction
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
    fun updatePosition(position: Point,stepCounterViewModel: StepCounterViewModel?) {
        if (!isNavigating) return
        val path = navigationViewModel?.currentPath ?: return
        if (path.isEmpty()) return
        // Dans ton ViewModel ou ton observateur de navigation
        val userHeading = stepCounterViewModel?.currentHeadingLive?.value ?: return
        val segmentAngle = calculateAngle(path[currentPointIndex], path[currentPointIndex + 1])
        val diff = angleDifference(userHeading.toDouble(), segmentAngle)
        if (diff in 15.0..345.0){
            val dynamicInstruction = getTurnInstruction(diff)
            onDynamicInstructionCallback?.invoke(dynamicInstruction)
            return
        }

        currentPosition = position
        traversedPath.add(position)
        navigationViewModel?.updateTraversedPath(position)
        onPositionUpdatedCallback?.invoke(position)

        // Check if user started walking (for first instruction)
        val currentTime = System.currentTimeMillis()
        if (!instructionGiven && lastPosition != null) {
            val movedDistance = calculateDistance(lastPosition!!, position)
            Log.d("NavigationUtils", "la distance parcourue par l'utilisateur: $movedDistance")
            if (movedDistance > MOVEMENT_THRESHOLD) {
                onInstructionChangedCallback?.invoke(currentInstructionIndex) // OK
                instructionGiven = true
            }
        }

        lastPosition = position
        lastMovementTime = currentTime
        Log.d("NavigationUtils", "la position de l'utilisateur actuelle: $position")
        Log.d("NavigationUtils", "l'index de la position actuelle dans la liste des points du path: $currentPointIndex")
        Log.d("NavigationUtils", "l'index de l'instruction actuelle dans la liste des instructions du path: $currentInstructionIndex")
        Log.d("NavigationUtils", "le Path: ${path}")

        // 1) avancer currentPointIndex si on passe un point
        if (currentPointIndex < path.size - 1) {
            val next = path[currentPointIndex + 1]
            if (calculateDistance(position, next)/100  <= STRAIGHT_DISTANCE_THRESHOLD) {
                Log.d("NavigationUtils", "Advancing to next point: ${calculateDistance(position, next)/100} ")
                currentPointIndex++
            }
        }

// 2) recalculer l’instruction en “intelligent”
        updateInstructionIndex(position, path)

        // 3) détection d’arrivée
        if (currentPointIndex >= path.size - 1 ||
            calculateDistance(position, path.last())/100  <= STRAIGHT_DISTANCE_THRESHOLD) {
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
            calculateDistance(currentPosition!!, nextPoint) <= STRAIGHT_DISTANCE_THRESHOLD

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

    fun calculateDistanceToSegment(point: Point, segmentStart: Point, segmentEnd: Point): Double {
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

     fun calculateAngle(from: Point, to: Point): Double {
        val dx = (to.x - from.x).toDouble()
        val dy = (to.y - from.y).toDouble()
        return Math.toDegrees(kotlin.math.atan2(dy, dx))
    }

     fun angleDifference(a1: Double, a2: Double): Double {
        val diff = ((a2 - a1 + 180 + 360) % 360) - 180
        return kotlin.math.abs(diff)
    }
    fun getTurnInstruction(diff: Double): String {


        return when {
            diff < 15 || diff > 345 -> "Go straight"
            diff in 15.0..45.0 -> "Slightly right"
            diff in 45.0..100.0 -> "Turn right"
            diff in 100.0..150.0 -> "Sharp right"
            diff in 150.0..210.0 -> "Make a U-turn"
            diff in 210.0..260.0 -> "Sharp left"
            diff in 260.0..315.0 -> "Turn left"
            diff in 315.0..345.0 -> "Slightly left"
            else -> "Adjust your orientation"
        }
    }





}