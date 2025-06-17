package com.example.malvoayant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.data.api.RetrofitClient
import com.example.malvoayant.data.viewmodels.FloorPlanViewModel
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.repositories.AuthRepository
import com.example.malvoayant.ui.screens.SplashScreen

class MainActivity : ComponentActivity() {

    private lateinit var stepCounterViewModel: StepCounterViewModel

    // 👇 Permission launcher
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Camera permission is required for navigation", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiService = RetrofitClient.authApiService
        val authRepository = AuthRepository(apiService)
        // ✅ Demande la permission caméra si pas déjà accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val floorPlanViewModel: FloorPlanViewModel = viewModel()
            stepCounterViewModel = StepCounterViewModel(application, floorPlanViewModel.floorPlanState)
            stepCounterViewModel.startListening()

            if (showSplash) {
                SplashScreen(
                    onSplashFinished = { showSplash = false }
                )
            } else {
                NavigationController(stepCounterViewModel,authRepository)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stepCounterViewModel.stopListening()
    }
}
