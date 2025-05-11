package com.example.malvoayant.data.viewmodels

// NavigationViewModel.kt

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.NavigationLogic.Algorithm.PathFinder
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class NavigationViewModel(
    private val floorPlanViewModel: FloorPlanViewModel
) : ViewModel() {
    private val pathFinder = PathFinder()

    // Déclaration correcte avec delegate property
    var currentPath by mutableStateOf<List<Point>?>(null)
        private set

    // Déclaration correcte pour le loading state
    var isLoading by mutableStateOf(false)
        private set

    fun calculatePath(start: Any, destination: Any) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val floorPlan = floorPlanViewModel.floorPlanState
                currentPath = pathFinder.findPath(start, destination, floorPlan)
                Log.d("PathDebug", "Path calculated: ${currentPath?.size} points")
            } catch (e: Exception) {
                currentPath = null
                Log.e("PathError", "Calculation failed", e)
            } finally {
                isLoading = false
            }
        }
    }
}