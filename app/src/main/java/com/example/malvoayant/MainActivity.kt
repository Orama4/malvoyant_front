package com.example.malvoayant


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.NavigationLogic.Algorithm.SafePathFinder
import com.example.malvoayant.data.viewmodels.FloorPlanViewModel
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.data.viewmodels.StepCounterViewModel

class MainActivity : ComponentActivity() {
    private lateinit var stepCounterViewModel: StepCounterViewModel
    private val pathFinder = SafePathFinder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val floorPlanViewModel: FloorPlanViewModel = viewModel()
            stepCounterViewModel =StepCounterViewModel(application,floorPlanViewModel.floorPlanState)
            stepCounterViewModel.startListening()
            NavigationController(stepCounterViewModel)        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop listening when the app is closed
        stepCounterViewModel.stopListening()
    }
}
