package com.example.malvoayant


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.malvoayant.navigation.Destination
import com.example.malvoayant.ui.screens.HomeScreen
import com.example.malvoayant.ui.screens.LoginScreen
import com.example.malvoayant.ui.screens.RegistrationScreen1
import com.example.malvoayant.ui.theme.MalvoayantTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavigationScreen(navController)
        }
    }
}

@Composable
fun NavigationScreen(navController: NavHostController) {
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = Destination.Home.route) {
        composable(Destination.Home.route) { HomeScreen(context,navController) }
        composable(Destination.Registration.route) { RegistrationScreen1(context) }
        composable(Destination.Login.route) { LoginScreen(context) }
    }
}

