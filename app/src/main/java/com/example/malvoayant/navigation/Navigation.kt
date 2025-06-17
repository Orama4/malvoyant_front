package com.example.malvoayant.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.malvoayant.data.api.RetrofitClient
import com.example.malvoayant.data.viewmodels.AuthViewModel
import com.example.malvoayant.data.viewmodels.AuthViewModelFactory
import com.example.malvoayant.data.viewmodels.ContactViewModel
import com.example.malvoayant.data.viewmodels.ContactsViewModelFactory
import com.example.malvoayant.data.viewmodels.FloorPlanViewModel
import com.example.malvoayant.data.viewmodels.NavigationViewModel
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
import com.example.malvoayant.repositories.AuthRepository
import com.example.malvoayant.repositories.ContactsRepository
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
    object OD1 : Screen("od1")
    object OD2 : Screen("od2")
    object ResetPassword : Screen("ResetPassword")



}

@Composable
fun NavigationController(    stepCounterViewModel: StepCounterViewModel = viewModel(),authRepository: AuthRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current


    val floorPlanViewModel: FloorPlanViewModel = viewModel()
    val navigationViewModel: NavigationViewModel = remember { NavigationViewModel(floorPlanViewModel) }
    val contactApiService= RetrofitClient.contactService
    val contactViewModel: ContactViewModel = viewModel(
        factory = ContactsViewModelFactory(
            repository = ContactsRepository(
                contactApiService = contactApiService
            ), // Provide dependencies manually
            authRepository = authRepository, // Provide dependencies manually,
            context
        )
    )
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository,LocalContext.current)
    )
    LaunchedEffect(Unit) {
        floorPlanViewModel.loadGeoJSONFromAssets(context)


    }
    val isLoggedIn = remember {
        derivedStateOf {
            authViewModel.getToken() != null
        }
    }
    //val startDestination = if (isLoggedIn.value) Screen.Search.route else Screen.Home.route
    val startDestination =  Screen.Search.route
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(Screen.Home.route) { HomeScreen(context,navController) }
        composable(Screen.Registration.route) { RegistrationScreen1(context,authViewModel,navController) }
        composable(Screen.Login.route) { LoginScreen(context,authViewModel,navController) }
        composable(Screen.ResetPassword.route + "/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordScreen(navController, authViewModel, email = email)
        }

        // Helper Screen
        composable(Screen.Helper.route) {
            HelperScreen(
                context = context,
                navController = navController
            )
        }

        // Phone Numbers Screen
        // Phone Numbers Screen
        composable(Screen.PhoneNumbers.route) {
            PhoneNumbersScreen(
                context = context,
                navController = navController,
                viewModel = contactViewModel
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
        composable(Screen.OD2.route) {
            OD2Screen(navController)
        }
        composable(Screen.OD1.route) {
            OD1Screen(navController)
        }

        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                context = context,
                navController = navController,
                floorPlanViewModel = floorPlanViewModel,
                stepCounterViewModel = stepCounterViewModel,
                navigationViewModel = navigationViewModel

            )
        }
    }
}