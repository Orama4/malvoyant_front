package com.example.malvoayant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.ui.screens.FloorPlanViewModel
import com.example.malvoayant.ui.screens.StepCounterViewModel
import com.example.malvoayant.ui.theme.MalvoayantTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val stepCounterViewModel: StepCounterViewModel by viewModels()
    private val floorPlanViewModel: FloorPlanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MalvoayantTheme {
                // Set up the navigation with both ViewModels
                NavigationController(
                    stepCounterViewModel = stepCounterViewModel,
                    floorPlanViewModel = floorPlanViewModel
                )
            }
        }

        lifecycleScope.launch {
            // Start listening to sensors and WebSocket when the activity is created
            stepCounterViewModel.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        // Optionally stop sensors when app goes to background to save battery
        // stepCounterViewModel.stopListening()
    }

    override fun onResume() {
        super.onResume()
        // Restart listening if it was stopped in onPause
        if (!stepCounterViewModel.isConnected.value!!) {
            stepCounterViewModel.startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up when the app is closed
        stepCounterViewModel.stopListening()
    }
}