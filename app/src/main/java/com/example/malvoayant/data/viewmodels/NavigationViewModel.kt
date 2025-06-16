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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.malvoayant.utils.NavigationUtils
import kotlin.math.abs
import kotlin.math.atan2

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

    fun getDetectedObstacles(): List<Point> = NavigationUtils.getDetectedObstacles()
    // Ajoutez cette fonction
    private val _traversedPath = MutableStateFlow<List<Point>>(emptyList())
    val traversedPath: StateFlow<List<Point>> = _traversedPath

    fun updateTraversedPath(point: Point) {
        _traversedPath.update { it + point }
    }

    fun clearTraversedPath() {
        _traversedPath.update { emptyList() }
    }
    fun calculatePath(start: Any, destination: Any) {
        calculatePathInternal(start, destination, stopNavigationFirst = true)
    }

    // Fonction spéciale pour recalcul d'obstacle
    fun recalculatePathForObstacle(start: Any, destination: Any) {
        calculatePathInternal(start, destination, stopNavigationFirst = false)
    }

    private fun calculatePathInternal(start: Any, destination: Any, stopNavigationFirst: Boolean) {

        if (stopNavigationFirst) {
            NavigationUtils.stopNavigation(null)
            clearTraversedPath()
        }
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
                val path = gr.simplifyPath(patt)
                currentPath = path
                instructions = mutableListOf()

                for (i in 0 until path.size - 1) {
                    val p1 = path[i]
                    val p2 = path[i + 1]

                    // Ajouter une instruction de distance
                    /**TO DO: ici on ajoute une instruction en calcule la diff entre lorientation lancienne et lengle de cette instruction pour le demander dajuster son angle avant de commencer**/
                    val distance = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)) / 100
                    instructions += StaticInstruction("Go straight", distance)

                    // S’il y a un prochain segment, calculer l’angle du virage
                    if (i < path.size - 2) {
                        val p3 = path[i + 2]

                        // Vecteurs : direction1 = p2 - p1 ; direction2 = p3 - p2
                        val dx1 = p2.x - p1.x
                        val dy1 = p2.y - p1.y
                        val dx2 = p3.x - p2.x
                        val dy2 = p3.y - p2.y

                        val angle1 = atan2(dy1.toDouble(), dx1.toDouble())
                        val angle2 = atan2(dy2.toDouble(), dx2.toDouble())

                        // Calculer l'angle relatif entre les deux directions
                        var angleDiff = angle2 - angle1

                        // Normaliser entre -PI et PI
                        angleDiff = (angleDiff + Math.PI) % (2 * Math.PI) - Math.PI

                        val instruction = when {
                            abs(angleDiff) < Math.PI / 8 -> null // Trop petit → continuer
                            angleDiff in (Math.PI / 8)..(3 * Math.PI / 8) -> "Turn slightly right"
                            angleDiff in (3 * Math.PI / 8)..(5 * Math.PI / 8) -> "Turn right"
                            angleDiff > (5 * Math.PI / 8) -> "Sharp right"
                            angleDiff in (-3 * Math.PI / 8)..(-Math.PI / 8) -> "Turn slightly left"
                            angleDiff in (-5 * Math.PI / 8)..(-3 * Math.PI / 8) -> "Turn left"
                            angleDiff < (-5 * Math.PI / 8) -> "Sharp left"
                            else -> null
                        }

                        if (instruction != null) {
                            instructions += StaticInstruction(
                                instruction = instruction,
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