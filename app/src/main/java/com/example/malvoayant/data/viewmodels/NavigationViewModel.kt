package com.example.malvoayant.data.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.NavigationLogic.Algorithm.PathFinder
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.exceptions.PathfindingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NavigationViewModel(
    private val floorPlanViewModel: FloorPlanViewModel
) : ViewModel() {
    private val pathFinder = PathFinder()

    var currentPath by mutableStateOf<List<Point>?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun calculatePath(start: Any, destination: Any) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            currentPath = null

            try {
                val floorPlan = floorPlanViewModel.floorPlanState
                currentPath = pathFinder.findPath(start, destination, floorPlan)
                Log.d("NavigationVM", "Path calculated with ${currentPath?.size ?: 0} points")
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