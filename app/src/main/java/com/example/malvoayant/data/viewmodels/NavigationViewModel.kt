package com.example.malvoayant.data.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.NavigationLogic.Algorithm.PathFinder
import com.example.malvoayant.NavigationLogic.Models.StaticInstruction
import com.example.malvoayant.NavigationLogic.graph.GridNavigationGraph
import com.example.malvoayant.NavigationLogic.utils.calculateDistance
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.exceptions.PathfindingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.malvoayant.utils.NavigationUtils
import kotlinx.coroutines.withContext
import kotlin.math.atan2

class NavigationViewModel(
    private val floorPlanViewModel: FloorPlanViewModel,
) : ViewModel() {
    private val pathFinder = PathFinder()

    private var deviationThreshold = 1.0f
    private val minorDeviationThreshold = 25.0f // 50cm
    private val majorDeviationThreshold = 100.0f // 2m
    private val segmentCompletionThreshold = 40.0f
    var isOffPath by mutableStateOf(false)

    var currentPath by mutableStateOf<List<Point>?>(null)
        private set
    var instructions by mutableStateOf<List<StaticInstruction>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var currentPathIndex by mutableStateOf(0)
        private set

    // Add this variable to track current segment
    private var currentSegmentIndex by mutableStateOf(0)

    fun clearError() {
        errorMessage = null
    }
    var showInstructions by mutableStateOf(false)
        private set

    // Reset path tracking when starting new navigation
    fun resetPathTracking() {
        currentSegmentIndex = 0
        currentPathIndex = 0
        isOffPath = false
        errorMessage = null
    }

    // Ajoutez cette fonction
    fun handleObstacleDetected(position: Point) {
        NavigationUtils.detectObstacle(position)
    }
    private val _traversedPath = MutableStateFlow<List<Point>>(emptyList())
    val traversedPath: StateFlow<List<Point>> = _traversedPath

    fun updateTraversedPath(point: Point) {
        _traversedPath.update { it + point }
    }

    fun calculatePath(start: Any, destination: Any) {
        NavigationUtils.stopNavigation(null)
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            currentPath = null

            try {
                val floorPlan = floorPlanViewModel.floorPlanState
                Log.d("NavigationVM", "brooo")
                val gr=GridNavigationGraph(
                    floorPlan = floorPlan,
                    bounds = listOf(
                        floorPlan.minPoint.x,
                        floorPlan.maxPoint.x,
                        floorPlan.minPoint.y,
                        floorPlan.maxPoint.y
                    )
                )
                Log.d("NavigationVM", "brooo")
                gr.buildGrid()
                val patt= gr.findPath(start, destination )
                Log.d("NavigationVMG", "Path found: $patt")
                val patt_simp= gr.simplifyPath(patt)
                Log.d("NavigationVMG", "Simplified path: $patt_simp")
                //currentPath = pathFinder.findPath(start, destination, floorPlan)
                currentPath = patt_simp
                instructions = emptyList()
                //crate instructions based on the path

                for (i in 0 until currentPath!!.size - 1) {
                    instructions=instructions+ StaticInstruction(
                        instruction = "Go straight",
                        distance = sqrt((currentPath!![i+1].x - currentPath!![i].x).pow(2) +
                                (currentPath!![i+1].y - currentPath!![i].y).pow(2) )/100,
                    )
                    // checking if i+1 is on the right or left of i to add turning instruction

                    if (i < currentPath!!.size - 2) {
                        val nextPoint = currentPath!![i + 2]
                        val currentPoint = currentPath!![i + 1]
                        val startPoint = currentPath!![i]

                        // Calculate the direction of the turn
                        val dx1 = currentPoint.x - startPoint.x
                        val dy1 = currentPoint.y - startPoint.y
                        val dx2 = nextPoint.x - currentPoint.x
                        val dy2 = nextPoint.y - currentPoint.y

                        // Calculate the angle between the two segments
                        val angle = Math.toDegrees(
                            Math.atan2(dy2.toDouble(), dx2.toDouble()) - Math.atan2(dy1.toDouble(),
                                dx1.toDouble()
                            )
                        ).toFloat()
                        if(startPoint.x==currentPoint.x || startPoint.y==currentPoint.y) {

                            if (startPoint.x > currentPoint.x) {
                                if (currentPoint.y > nextPoint.y) {
                                    instructions += StaticInstruction(
                                        instruction = "Turn right",
                                        distance = null,
                                        type = "Turning"
                                    )
                                } else {
                                    instructions += StaticInstruction(
                                        instruction = "Turn left",
                                        distance = null,
                                        type = "Turning"
                                    )
                                }
                            } else if (startPoint.x < currentPoint.x) {
                                if (currentPoint.y > nextPoint.y) {
                                    instructions += StaticInstruction(
                                        instruction = "Turn left",
                                        distance = null,
                                        type = "Turning"
                                    )
                                } else {
                                    instructions += StaticInstruction(
                                        instruction = "Turn right",
                                        distance = null,
                                        type = "Turning"
                                    )
                                }
                            } else if (startPoint.y < currentPoint.y) {
                                if (currentPoint.x > nextPoint.x) {
                                    instructions += StaticInstruction(
                                        instruction = "Turn right",
                                        distance = null,
                                        type = "Turning"
                                    )
                                } else {
                                    instructions += StaticInstruction(
                                        instruction = "Turn left",
                                        distance = null,
                                        type = "Turning"
                                    )
                                }
                            } else if (startPoint.y > currentPoint.y) {
                                if (currentPoint.x < nextPoint.x) {
                                    instructions += StaticInstruction(
                                        instruction = "Turn right",
                                        distance = null,
                                        type = "Turning"
                                    )
                                } else {
                                    instructions += StaticInstruction(
                                        instruction = "Turn left",
                                        distance = null,
                                        type = "Turning"
                                    )
                                }
                            }
                        }
                        else if (angle > 0) {
                            instructions += StaticInstruction(
                                instruction = "Turn right",
                                distance = null,
                                type = "Turning"
                            )
                        } else if (angle < 0) {
                            instructions += StaticInstruction(
                                instruction = "Turn left",
                                distance = null,
                                type = "Turning"
                            )
                        }
                    }
                }
                Log.d("NavigationVM","Instructions created with ${instructions} steps")
                Log.d("NavigationVM", "Path calculated with ${currentPath?.size ?: 0} points")
                Log.d("NavigationVM", "Path calculated with ${currentPath}")
            } catch (e: PathfindingException) {
                errorMessage = "Navigation error: ${e.message}"
                Log.e("NavigationVM", "Pathfinding failed", e)
            } catch (e: Exception) {
                errorMessage = "Unexpected error: ${e.localizedMessage ?: "Please try again"}"
                Log.e("NavigationVM", "Path calculation failed", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun checkForDeviation(currentPosition: Point) {
        viewModelScope.launch(Dispatchers.Default) {
            val path = currentPath ?: return@launch
            if (path.isEmpty()) return@launch

            Log.d("Deviation", "Checking deviation for position: $currentPosition")
            Log.d("Deviation", "Current segment index: $currentSegmentIndex")

            // 1. First, update our current segment based on progress
            updateCurrentSegment(currentPosition, path)

            // 2. Find the nearest point on the current and next segments
            val (nearestPoint, distanceToPath) = findNearestPointOnPath(currentPosition, path)

            Log.d("Deviation", "Current segment: $currentSegmentIndex, Distance to path: $distanceToPath")
            when {
                // Minor deviation - no action needed
                distanceToPath <= minorDeviationThreshold -> {
                    if (isOffPath) {
                        withContext(Dispatchers.Main) {
                            isOffPath = false
                            errorMessage = null
                        }
                    }
                }

                // Moderate deviation - guide back to path
                distanceToPath <= majorDeviationThreshold && distanceToPath > minorDeviationThreshold -> {
                    withContext(Dispatchers.Main) {
                        isOffPath = true
                       // errorMessage = "You've slightly deviated from the path"
                        // Generate guidance instructions
                        generateGuidanceInstructions(currentPosition, nearestPoint)
                    }
                }

                // Major deviation - recalculate path
                else -> {
                    withContext(Dispatchers.Main) {
                        isOffPath = true
                       // errorMessage = "Recalculating path due to large deviation"
                        recalculatePath(currentPosition)
                    }
                }
            }

            // Always update traversed path
            updateTraversedPath(currentPosition)
        }
    }
    private fun updateCurrentSegment(currentPosition: Point, path: List<Point>) {
        // If we're at the last segment, no need to update
        if (currentSegmentIndex >= path.size - 1) return

        // Check distance to next waypoint
        val nextWaypoint = path[currentSegmentIndex + 1]
        val distanceToNext = calculateDistance(currentPosition, nextWaypoint)

        // If we're close enough to the next waypoint, advance our segment
        if (distanceToNext <= segmentCompletionThreshold) {
            currentSegmentIndex = minOf(currentSegmentIndex + 1, path.size - 2) // -2 because segments are between points
            Log.d("Deviation", "Advanced to segment: $currentSegmentIndex")
        }
    }


    private fun findNearestPointOnPath(
        currentPosition: Point,
        path: List<Point>
    ): Pair<Point, Float> {
        // We'll check the current segment and the next 2 segments ahead
        val segmentsToCheck = 2
        val startIndex = currentSegmentIndex
        val endIndex = minOf(currentSegmentIndex + segmentsToCheck, path.size - 2)

        var nearestPoint = path[currentSegmentIndex]
        var minDistance = calculateDistance(currentPosition, nearestPoint).toFloat()

        // Check all relevant segments
        for (i in startIndex..endIndex) {
            val segmentStart = path[i]
            val segmentEnd = path[i + 1]

            val distance = NavigationUtils.calculateDistanceToSegment(
                currentPosition,
                segmentStart,
                segmentEnd
            ).toFloat()

            if (distance < minDistance) {
                minDistance = distance
                // Calculate the actual nearest point if needed (optional)
                nearestPoint = getClosestPointOnLineSegment(currentPosition, segmentStart, segmentEnd)
            }
        }

        return Pair(nearestPoint, minDistance)
    }

    // You can keep the getClosestPointOnLineSegment function as is since it's still used above
    private fun getClosestPointOnLineSegment(
        point: Point,
        lineStart: Point,
        lineEnd: Point
    ): Point {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y

        if (dx == 0f && dy == 0f) {
            return lineStart // Degenerate segment
        }

        val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0f, 1f)

        return Point(
            x = lineStart.x + clampedT * dx,
            y = lineStart.y + clampedT * dy
        )
    }

    // Update the generateGuidanceInstructions function
    private fun generateGuidanceInstructions(currentPosition: Point, nearestPathPoint: Point) {
        viewModelScope.launch(Dispatchers.Default) {
            // Calculate direction to guide user back to path
            val dx = nearestPathPoint.x - currentPosition.x
            val dy = nearestPathPoint.y - currentPosition.y

            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

            val instruction = when {
                angle > -45 && angle <= 45 -> "Turn slightly right to return to path"
                angle > 45 && angle <= 135 -> "Turn around to return to path"
                angle > 135 || angle <= -135 -> "Turn around to return to path"
                else -> "Turn slightly left to return to path"
            }

            withContext(Dispatchers.Main) {
                // Only add guidance instruction if there isn't already one
                if (instructions.none { it.type == "Guidance" }) {
                    instructions = listOf(StaticInstruction(instruction, null, "Guidance")) + instructions
                }
            }
        }
    }

    // Update the recalculatePath function
    private fun recalculatePath(currentPosition: Point) {
        val destination = currentPath?.lastOrNull() ?: return
        viewModelScope.launch {
            // First clear old instructions
            instructions = emptyList()
            showInstructions = false
            resetPathTracking()

            Log.d("Deviation","Recalculating path. Please wait.")

            // Calculate new path
            calculatePath(currentPosition, destination)
            isOffPath = true
        }
    }
}
