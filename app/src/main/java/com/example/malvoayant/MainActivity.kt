package com.example.malvoayant


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.NavigationLogic.Algorithm.SafePathFinder
import com.example.malvoayant.data.viewmodels.FloorPlanViewModel
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.ui.screens.StepCounterViewModel

class MainActivity : ComponentActivity() {
    private lateinit var stepviewModel: StepCounterViewModel
    private lateinit var floorPlanViewModel: FloorPlanViewModel
    private val pathFinder = SafePathFinder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            floorPlanViewModel = viewModel()
            stepviewModel = StepCounterViewModel(application,floorPlanViewModel.floorPlanState)
            stepviewModel.startListening()
            NavigationController(stepviewModel)        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop listening when the app is closed
        stepviewModel.stopListening()
    }
}
