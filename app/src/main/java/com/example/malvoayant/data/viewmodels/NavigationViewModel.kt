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
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.exceptions.PathfindingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

class NavigationViewModel(
    private val floorPlanViewModel: FloorPlanViewModel,
    ) : ViewModel() {
    private val pathFinder = PathFinder()

    var currentPath by mutableStateOf<List<Point>?>(null)
        private set
    var instructions by mutableStateOf<List<StaticInstruction>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        errorMessage = null
    }
    var showInstructions by mutableStateOf(false)
        private set



    fun calculatePath(start: Any, destination: Any) {
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
}