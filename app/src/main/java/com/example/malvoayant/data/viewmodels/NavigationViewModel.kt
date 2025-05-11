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
    var currentPath by mutableStateOf<List<Point>?>(null)
        private set

    fun calculatePath(
        start: Any,
        destination: Any
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val floorPlan = floorPlanViewModel.floorPlanState
                val path = pathFinder.findPath(start, destination, floorPlan)

                currentPath = path
                Log.d("PathFinder", "Chemin nettoyé: $currentPath")
            } catch (e: Exception) {
                currentPath = null
                Log.e("Navigation", "Erreur calcul chemin", e)
            }
        }
    }
}