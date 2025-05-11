package com.example.malvoayant


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.malvoayant.NavigationLogic.Algorithm.SafePathFinder
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.data.viewmodels.StepCounterViewModel

class MainActivity : ComponentActivity() {
    private lateinit var stepCounterViewModel: StepCounterViewModel
    private val pathFinder = SafePathFinder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            stepCounterViewModel = ViewModelProvider(this)[StepCounterViewModel::class.java]
            stepCounterViewModel.startListening()
            NavigationController(stepCounterViewModel)        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop listening when the app is closed
        stepCounterViewModel.stopListening()
    }
}
