package com.example.malvoayant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.ui.screens.StepCounterViewModel

class MainActivity : ComponentActivity() {
    private lateinit var stepCounterViewModel: StepCounterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            stepCounterViewModel = ViewModelProvider(this)[StepCounterViewModel::class.java]

            // Start listening to sensors and WebSocket
            stepCounterViewModel.startListening()

            // Set up the navigation with the StepCounterViewModel
            NavigationController(stepCounterViewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop listening when the app is closed
        stepCounterViewModel.stopListening()
    }
}