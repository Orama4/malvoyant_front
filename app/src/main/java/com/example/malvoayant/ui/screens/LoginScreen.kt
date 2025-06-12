package com.example.malvoayant.ui.screens

import android.Manifest
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malvoayant.R
import com.example.malvoayant.ui.components.*
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.malvoayant.data.api.LoginRequest
import com.example.malvoayant.navigation.Screen
import com.example.malvoayant.ui.utils.fixSpokenEmail
import com.example.malvoayant.ui.utils.startListening
import com.example.malvoayant.data.viewmodels.AuthViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
fun LoginScreen(context: Context, viewModel: AuthViewModel, navController: NavController) {
    val textStates = remember { mutableStateListOf("", "") }
    var step by remember { mutableStateOf(0) }
    var savestep by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startListening(context) { recognizedText ->
                textStates[step] = recognizedText // Update the text field with speech input
            }
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    val onspeakHelp = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening(context) { recognizedText ->
                val processedEmail = fixSpokenEmail(recognizedText)
                textStates[step] = processedEmail
            }
        } else {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // For click handling
    var lastClickTime = remember { mutableStateOf(0L) }
    val doubleClickTimeWindow = 300L

    // Create a coroutine scope tied to this composable
    val scope = rememberCoroutineScope()

    // Reference to the job for pending speech
    val pendingSpeechJob = remember { mutableStateOf<Job?>(null) }

    val labels = listOf("Email", "Password","submit")
    val placeholders = listOf("Enter your email", "Enter your password","")
    val icons = listOf(
        painterResource(id = R.drawable.ic_email),
        painterResource(id = R.drawable.ic_password),
        painterResource(id = R.drawable.ic_register)
    )

    val speechHelper = remember { SpeechHelper(context) }

    // Initialize speech when the screen is launched
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            speechHelper.speak("This is the login page. Enter your ${labels[step]}")
        }
    }

    LaunchedEffect(step) {
        when (step) {
            0, 1 -> speechHelper.speak("Enter your ${labels[step]}")
            2 -> speechHelper.speak("Click the button in the middle of the screen to login")
        }
    }


    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val isLoading by viewModel.loading.collectAsState()


    LaunchedEffect(loginSuccess) {
        if (loginSuccess != null) {
            navController.navigate(Screen.Search.route){
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    when {

                        dragAmount < -50 && step < labels.size - 1 -> { // Swipe left
                            step++

                        }
                        dragAmount < -50 && savestep < labels.size  -> { // Swipe left
                            savestep++


                        }
                        step==1 &&savestep==2->{
                            navController.navigate(Screen.Search.route) // Navigate to Search.route

                        }

                        dragAmount > 50 && step > 0 -> { // Swipe right
                            step--
                            savestep--
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(AppColors.darkBlue),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            HeaderBar(
                pageType = "login",
                onSpeakHelp = {
                    speechHelper.speak("This is the login page. Enter your ${labels[step]}")
                }
            )


            Column(
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Dynamic Input Field
                if (step < labels.lastIndex) {
                    MyTextField(
                        text = labels[step],
                        content = labels[step],
                        placeHolder = placeholders[step],
                        icon = icons[step],
                        isPassword = step == 1,
                        value = textStates[step],
                        onValueChange = { textStates[step] = it },
                        onDone = { step++ }
                    )
                }

                if (step == labels.lastIndex) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = AppColors.darkBlue)
                        } else {
                            NavigationButton(
                                text = "LOGIN",
                                icon = painterResource(id = R.drawable.ic_register),
                                onClick = {
                                    viewModel.login(LoginRequest(textStates[0], textStates[1]))
                                }
                            )
                        }
                    }
                }



                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Divider(
                        color = AppColors.darkBlue,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(1.dp)
                    )
                    Text(
                        text = "  OR  ",
                        fontSize = 24.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            speechHelper.speak("Or")
                        },
                        color = AppColors.writingBlue
                    )
                    Divider(
                        color = AppColors.darkBlue,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                // Login Button
                NavigationButton(
                    text = "CLICK TO SPELL",
                    icon = painterResource(id = R.drawable.ic_mic),
                    onClick = {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                            // Double click detected
                            // Cancel any pending speech from single click
                            pendingSpeechJob.value?.cancel()

                            // Perform double-click action immediately
                            onspeakHelp()
                        } else {
                            // Single click - delay the speech
                            pendingSpeechJob.value = scope.launch {
                                // Wait to see if this becomes a double click
                                delay(doubleClickTimeWindow)

                                // If we reach here, no double-click happened
                                speechHelper.speak("Micro button, click two times to activate voice function.")
                            }
                        }

                        lastClickTime.value = currentTime
                    }
                )
                // Page Indicator
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 30.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    PageIndicator(totalDots = labels.size, selectedIndex = step)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
        }
    }
}
