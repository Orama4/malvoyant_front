package com.example.malvoayant.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.malvoayant.api.RetrofitClient
import com.example.malvoayant.repositories.AuthRepository
import com.example.malvoayant.repositories.ContactsRepository
import com.example.malvoayant.ui.screens.*
import com.example.malvoayant.viewmodels.AuthViewModel
import com.example.malvoayant.viewmodels.AuthViewModelFactory
import com.example.malvoayant.viewmodels.ContactViewModel
import com.example.malvoayant.viewmodels.ContactsViewModelFactory
import com.example.malvoayant.viewmodels.FloorPlanViewModel

// Define the navigation routes
sealed class Screen(val route: String) {
    object Helper : Screen("helper")
    object PhoneNumbers : Screen("phone_numbers")
    object Repair : Screen("repair")
    object Search : Screen("search")
    object Home : Screen("home")
    object Registration : Screen("registration")
    object Login : Screen("login")
    object Verification : Screen("verification")
    object VerificationOtp : Screen("VerificationOtp")
    object ResetPassword : Screen("ResetPassword")

    fun createRoute(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }

}



@Composable
fun NavigationController(authRepository: AuthRepository) {
    val context = LocalContext.current

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
    val navController = rememberNavController()
    val viewModel: FloorPlanViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository,LocalContext.current)
    )
    LaunchedEffect(Unit) {
        viewModel.loadGeoJSONFromAssets(context)
    }

    val isLoggedIn = remember {
        derivedStateOf {
            authViewModel.getToken() != null
        }
    }
    Log.d("LOGGED__IN",isLoggedIn.value.toString())
    val startDestination = if (isLoggedIn.value) Screen.Search.route else Screen.Home.route
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

        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                context = context,
                navController = navController,
                viewModel=viewModel,
                authViewModel=authViewModel
            )
        }
    }
}