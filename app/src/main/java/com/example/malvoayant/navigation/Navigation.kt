package com.example.malvoayant.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.malvoayant.ui.screens.*
import com.yourpackagename.screens.HomeScreen

// Define the navigation routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Helper : Screen("helper")
    object PhoneNumbers : Screen("phone_numbers")
    object Repair : Screen("repair")
    object Search : Screen("search")
}

@Composable
fun NavigationController() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Search.route
    ) {


        // Helper Screen
        composable(Screen.Helper.route) {
            HelperScreen(
                context = context,
                navController = navController
            )
        }

        // Phone Numbers Screen
        composable(Screen.PhoneNumbers.route) {
            PhoneNumbersScreen(
                context = context,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // Repair Screen
        composable(
            route = "${Screen.Repair.route}/{deviceStatus}?hasMeeting={hasMeeting}&date={date}&time={time}",
            arguments = listOf(
                navArgument("deviceStatus") { type = NavType.StringType },
                navArgument("hasMeeting") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("time") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val statusString = backStackEntry.arguments?.getString("deviceStatus") ?: "DISCONNECTED"
            val deviceStatus = DeviceStatus.valueOf(statusString)
            val hasMeeting = backStackEntry.arguments?.getBoolean("hasMeeting") ?: false
            val date = backStackEntry.arguments?.getString("date")
            val time = backStackEntry.arguments?.getString("time")

            val meetings = if (hasMeeting && date != null && time != null) {
                listOf(Meeting(date, time))
            } else {
                emptyList()
            }

            StatusMeetingScreen(
                context = context,
                onNavigateBack = { navController.navigateUp() },
                deviceStatus = deviceStatus,
                meetings = meetings
            )
        }

        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                context = context,
                navController = navController
//                onNavigateBack = { navController.navigateUp() },
//                onNavigateToHelper = { navController.navigate(Screen.Helper.route) },
//                onNavigateToSOS = { navController.navigate(Screen.PhoneNumbers.route) },
//                onNavigateToRepair = {
//                    // Navigate to repair with default values
//                    navController.navigate("${Screen.Repair.route}/DISCONNECTED")
//                }
            )
        }
    }
}