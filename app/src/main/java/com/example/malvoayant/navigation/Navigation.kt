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

// Define the navigation routes
sealed class Screen(val route: String) {
    object Helper : Screen("helper")
    object PhoneNumbers : Screen("phone_numbers")
    object Repair : Screen("repair")
    object Search : Screen("search")
    object Home : Screen("home")
    object Registration : Screen("registration")
    object Login : Screen("login")

}

@Composable
fun NavigationController() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Search.route
    ) {

        composable(Screen.Home.route) { HomeScreen(context,navController) }
        composable(Screen.Registration.route) { RegistrationScreen1(context) }
        composable(Screen.Login.route) { LoginScreen(context,navController) }

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
                navController = navController
            )
        }

        // Repair Screen
        composable(
            route = "${Screen.Repair.route}/{deviceStatus}?date={date}&time={time}",
            arguments = listOf(
                navArgument("deviceStatus") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType; nullable = true },
                navArgument("time") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val statusString = backStackEntry.arguments?.getString("deviceStatus") ?: "DISCONNECTED"
            val deviceStatus = DeviceStatus.valueOf(statusString)
            val date = backStackEntry.arguments?.getString("date")
            val time = backStackEntry.arguments?.getString("time")

            val meeting = if (date != null && time != null) {
                Meeting(date, time)
            } else {
                null
            }

            StatusMeetingScreen(
                context = context,
                navController = navController,
                deviceStatus = deviceStatus,
                meeting = meeting
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