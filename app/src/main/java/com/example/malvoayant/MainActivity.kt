package com.example.malvoayant


import android.content.Intent
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.ui.screens.FloorPlanViewModel
import com.example.malvoayant.ui.screens.StepCounterViewModel


import com.example.malvoayant.ui.theme.MalvoayantTheme

class MainActivity : ComponentActivity() {
    private lateinit var stepviewModel: StepCounterViewModel
    private lateinit var floorPlanViewModel: FloorPlanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            floorPlanViewModel = viewModel()
            stepviewModel = StepCounterViewModel(application,floorPlanViewModel.floorPlanState)
            stepviewModel.startListening()
            NavigationController(stepviewModel)
        }
    }
}