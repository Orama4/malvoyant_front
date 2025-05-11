package com.example.malvoayant


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.malvoayant.NavigationLogic.Algorithm.SafePathFinder
import com.example.malvoayant.navigation.NavigationController
import com.example.malvoayant.data.viewmodels.StepCounterViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: StepCounterViewModel
    private val pathFinder = SafePathFinder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel = StepCounterViewModel(application)
            viewModel.startListening()
            NavigationController(viewModel)        }
    }
}
