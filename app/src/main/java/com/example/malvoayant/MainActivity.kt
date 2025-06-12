package com.example.malvoayant


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.NavigationLogic.Algorithm.SafePathFinder

import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.malvoayant.api.RetrofitClient
import com.example.malvoayant.data.viewmodels.FloorPlanViewModel
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
import com.example.malvoayant.repositories.AuthRepository

import com.example.malvoayant.ui.theme.MalvoayantTheme

class MainActivity : ComponentActivity() {
    private lateinit var stepCounterViewModel: StepCounterViewModel
    private val pathFinder = SafePathFinder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiService = RetrofitClient.authApiService
        val authRepository = AuthRepository(apiService)
        setContent {
            val floorPlanViewModel: FloorPlanViewModel = viewModel()
            stepCounterViewModel =StepCounterViewModel(application,floorPlanViewModel.floorPlanState)
            stepCounterViewModel.startListening()
            NavigationController(stepCounterViewModel,authRepository)        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop listening when the app is closed
        stepCounterViewModel.stopListening()
    }
}
