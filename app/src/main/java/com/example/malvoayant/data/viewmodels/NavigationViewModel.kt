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
import com.example.malvoayant.utils.NavigationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class NavigationViewModel(
    private val floorPlanViewModel: FloorPlanViewModel
) : ViewModel() {
    private val pathFinder = PathFinder()

    private var deviationThreshold = 1.0f
    private val minorDeviationThreshold = 0.5f // 50cm
    private val majorDeviationThreshold = 2.0f // 2m
    var isOffPath by mutableStateOf(false)
        private set

    var currentPath by mutableStateOf<List<Point>?>(null)
        private set
    var instructions by mutableStateOf<List<StaticInstruction>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Enhanced test mode variables
    private var testCoroutineJob: Job? = null
    private var isAutoTestRunning by mutableStateOf(false)
    private var testSpeed = 2000L // 2 seconds between positions
    // Test mode status
    var testModeStatus by mutableStateOf("Test Mode: Off")
        private set

    // Function to start automatic deviation testing
    fun startDeviationTest() {
        val path = currentPath ?: return
        if (path.isEmpty()) return

        testCoroutineJob?.cancel()
        isAutoTestRunning = true
        testModeStatus = "Test Mode: Running"

        testCoroutineJob = viewModelScope.launch {
            try {
                // Test scenario 1: Normal path following
                testModeStatus = "Testing: Normal Path Following"
                for (i in path.indices) {
                    if (!isAutoTestRunning) break
                    delay(testSpeed)
                    checkForDeviation(path[i])
                    Log.d("DeviationTest", "Position ${i + 1}/${path.size}: On path")
                }

                delay(1000)

                // Test scenario 2: Minor deviations
                testModeStatus = "Testing: Minor Deviations"
                for (i in 0 until minOf(5, path.size)) {
                    if (!isAutoTestRunning) break
                    val deviatedPoint = Point(
                        x = path[i].x + (0.2f + i * 0.1f), // 0.2m to 0.6m deviation
                        y = path[i].y + (0.2f + i * 0.1f)
                    )
                    delay(testSpeed)
                    checkForDeviation(deviatedPoint)
                    Log.d("DeviationTest", "Minor deviation test ${i + 1}: ${0.2f + i * 0.1f}m off path")
                }

                delay(1000)

                // Test scenario 3: Moderate deviations
                testModeStatus = "Testing: Moderate Deviations"
                for (i in 0 until minOf(3, path.size)) {
                    if (!isAutoTestRunning) break
                    val deviatedPoint = Point(
                        x = path[i].x + (0.8f + i * 0.4f), // 0.8m to 1.6m deviation
                        y = path[i].y + (0.8f + i * 0.4f)
                    )
                    delay(testSpeed)
                    checkForDeviation(deviatedPoint)
                    Log.d("DeviationTest", "Moderate deviation test ${i + 1}: ${0.8f + i * 0.4f}m off path")
                }

                delay(1000)

                // Test scenario 4: Major deviations (triggers recalculation)
                testModeStatus = "Testing: Major Deviations"
                for (i in 0 until minOf(2, path.size)) {
                    if (!isAutoTestRunning) break
                    val deviatedPoint = Point(
                        x = path[i].x + (2.5f + i * 0.5f), // 2.5m to 3m deviation
                        y = path[i].y + (2.5f + i * 0.5f)
                    )
                    delay(testSpeed)
                    checkForDeviation(deviatedPoint)
                    Log.d("DeviationTest", "Major deviation test ${i + 1}: ${2.5f + i * 0.5f}m off path")
                    delay(3000) // Wait for recalculation
                }

                // Test complete
                testModeStatus = "Test Mode: Completed Successfully"
                isAutoTestRunning = false

            } catch (e: Exception) {
                Log.e("DeviationTest", "Test failed: ${e.message}")
                testModeStatus = "Test Mode: Failed - ${e.message}"
                isAutoTestRunning = false
            }
        }
    }

    // Function to stop deviation testing
    fun stopDeviationTest() {
        testCoroutineJob?.cancel()
        isAutoTestRunning = false
        testModeStatus = "Test Mode: Stopped"
    }

    // Function to test specific deviation distance
    fun testSpecificDeviation(distance: Float) {
        val path = currentPath ?: return
        if (path.isEmpty()) return

        viewModelScope.launch {
            val testPoint = Point(
                x = path[0].x + distance,
                y = path[0].y + distance
            )
            testModeStatus = "Testing: ${distance}m deviation"
            checkForDeviation(testPoint)
            Log.d("DeviationTest", "Testing specific deviation: ${distance}m")
        }
    }

    // Function to simulate real-time position updates with controlled deviations
    fun simulateRealtimeWalk() {
        val path = currentPath ?: return
        if (path.isEmpty()) return

        testCoroutineJob?.cancel()
        isAutoTestRunning = true
        testModeStatus = "Simulating: Real-time Walk"

        testCoroutineJob = viewModelScope.launch {
            try {
                for (i in path.indices) {
                    if (!isAutoTestRunning) break

                    // Simulate some natural deviation (0.1m to 0.3m)
                    val naturalDeviation = 0.2f
                    val angle = (0..360).random() * Math.PI / 180

                    val simulatedPosition = Point(
                        x = path[i].x + (naturalDeviation * cos(angle)).toFloat(),
                        y = path[i].y + (naturalDeviation * sin(angle)).toFloat()
                    )

                    checkForDeviation(simulatedPosition)
                    delay(1000) // 1 second between positions

                    // Occasionally simulate larger deviation
                    if (i % 5 == 0 && i > 0) {
                        val largerDeviation = 1.7f
                        val deviatedPosition = Point(
                            x = (path[i].x + largerDeviation).toFloat(),
                            y = (path[i].y + largerDeviation).toFloat()
                        )
                        delay(500)
                        checkForDeviation(deviatedPosition)
                        delay(2000) // Wait for guidance

                        // Return to path
                        delay(500)
                        checkForDeviation(path[i])
                    }
                }

                testModeStatus = "Simulation: Completed"
                isAutoTestRunning = false

            } catch (e: Exception) {
                Log.e("DeviationTest", "Simulation failed: ${e.message}")
                testModeStatus = "Simulation: Failed"
                isAutoTestRunning = false
            }
        }
    }
    // Helper function to get current test status
    fun isTestRunning(): Boolean = isAutoTestRunning

    fun clearError() {
        errorMessage = null
    }
    var showInstructions by mutableStateOf(false)
        private set

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

            isOffPath = false

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
                                (currentPath!![i+1].y - currentPath!![i].y).pow(2) )/50,
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

            // Find nearest point on path
            val nearestPoint = path.minByOrNull { calculateDistance(it, currentPosition) }
                ?: return@launch

            val distanceToPath = calculateDistance(nearestPoint, currentPosition)

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
                distanceToPath <= majorDeviationThreshold -> {
                    withContext(Dispatchers.Main) {
                        isOffPath = true
                        errorMessage = "You've slightly deviated from the path"
                        // Generate guidance instructions
                        generateGuidanceInstructions(currentPosition, nearestPoint)
                    }
                }

                // Major deviation - recalculate path
                else -> {
                    withContext(Dispatchers.Main) {
                        isOffPath = true
                        errorMessage = "Recalculating path due to large deviation"
                        recalculatePath(currentPosition)
                    }
                }
            }

            // Always update traversed path
            updateTraversedPath(currentPosition)
        }
    }

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
                // Add this as a temporary instruction
                instructions = listOf(StaticInstruction(instruction, null, "Guidance")) + instructions
            }
        }
    }

    // method to recalculate path
    private fun recalculatePath(currentPosition: Point) {
        val destination = currentPath?.lastOrNull() ?: return
        calculatePath(currentPosition, destination)
    }


    // Test variables for simulation
    private var isTestMode by mutableStateOf(false)
    private var testPositions = mutableListOf<Point>()
    private var currentTestIndex = 0

    // Function to enable test mode
    fun enableTestMode(testPositions: List<Point>) {
        this.isTestMode = true
        this.testPositions = testPositions.toMutableList()
        this.currentTestIndex = 0
    }

    // Function to simulate next position
    fun simulateNextPosition() {
        if (isTestMode && currentTestIndex < testPositions.size) {
            val testPosition = testPositions[currentTestIndex]
            Log.d("NavigationVM", "Simulating position: $testPosition")
            checkForDeviation(testPosition)
            currentTestIndex++
        }
    }

    // Function to test specific deviation scenarios
    fun testDeviationScenarios() {
        val path = currentPath ?: return
        if (path.isEmpty()) return

        // Test minor deviation (within 0.5m)
        val minorDeviationPoint = Point(
            x = path[0].x + 0.3f, // 30cm off path
            y = path[0].y + 0.3f
        )

        // Test moderate deviation (between 0.5m and 2m)
        val moderateDeviationPoint = Point(
            x = path[0].x + 1.5f, // 1.5m off path
            y = path[0].y + 1.5f
        )

        // Test major deviation (over 2m)
        val majorDeviationPoint = Point(
            x = path[0].x + 3.0f, // 3m off path
            y = path[0].y + 3.0f
        )

        enableTestMode(listOf(
            path[0], // On path
            minorDeviationPoint,
            moderateDeviationPoint,
            majorDeviationPoint
        ))
    }

    // Function to create test positions around a path
    fun createTestPositionsAroundPath(): List<Point> {
        val path = currentPath ?: return emptyList()
        val testPositions = mutableListOf<Point>()

        path.forEachIndexed { index, pathPoint ->
            // On path
            testPositions.add(pathPoint)

            // Minor deviation (0.3m)
            testPositions.add(Point(pathPoint.x + 0.3f, pathPoint.y))

            // Moderate deviation (1.2m)
            testPositions.add(Point(pathPoint.x + 1.2f, pathPoint.y))

            // Major deviation (2.5m)
            testPositions.add(Point(pathPoint.x + 2.5f, pathPoint.y))
        }

        return testPositions
    }
}

