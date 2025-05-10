package com.example.malvoayant.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.malvoayant.ui.screens.FloorPlanScreen
import com.example.malvoayant.ui.screens.StepCounterViewModel

@Composable
fun NavigationController(stepCounterViewModel: StepCounterViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "floorplan"
    ) {
        composable("floorplan") {
            FloorPlanScreen(stepCounterViewModel)
        }
    }
}