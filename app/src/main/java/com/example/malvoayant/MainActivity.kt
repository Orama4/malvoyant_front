package com.example.malvoayant


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.NavigationLogic.Algorithm.SafePathFinder
import com.example.malvoayant.data.viewmodels.FloorPlanViewModel
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
import com.example.malvoayant.ui.screens.SplashScreen

class MainActivity : ComponentActivity() {
    private lateinit var stepCounterViewModel: StepCounterViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        //installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val floorPlanViewModel: FloorPlanViewModel = viewModel()
            stepCounterViewModel =StepCounterViewModel(application,floorPlanViewModel.floorPlanState)
            stepCounterViewModel.startListening()
            if (showSplash) {
                SplashScreen(
                    onSplashFinished = { showSplash = false }
                )
            } else {
                // Your main app content
                NavigationController(stepCounterViewModel)
            }
                 }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop listening when the app is closed
        stepCounterViewModel.stopListening()
    }
}
