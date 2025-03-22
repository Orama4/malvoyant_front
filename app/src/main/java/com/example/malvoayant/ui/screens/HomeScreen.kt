package  com.example.malvoayant.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.navigation.Destination
import com.example.malvoayant.ui.components.NavigationButton
import com.example.malvoayant.ui.components.HeaderBar
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import com.example.malvoayant.ui.utils.startListening

@Composable
fun HomeScreen(context: Context,navController: NavHostController) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startListening(
                context,
                onResult = TODO()
            ) // Start listening only if permission is granted
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    val onspeakHelp = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening(
                    context,
                    onResult = TODO()
                ) // Start listening
            } else {
                launcher.launch(Manifest.permission.RECORD_AUDIO) // Request permission
            }
    }
    // For click handling
    var lastClickTime = remember { mutableStateOf(0L) }
    val doubleClickTimeWindow = 300L

    // Create a coroutine scope tied to this composable
    val scope = rememberCoroutineScope()

    // Reference to the job for pending speech
    val pendingSpeechJob = remember { mutableStateOf<Job?>(null) }
    // Create speech helper using application context for lifecycle safety
    val speechHelper = remember { SpeechHelper(context.applicationContext) }

    // State to track initialization
    var ttsInitialized by remember { mutableStateOf(false) }

    // Initialize TTS when component is first composed
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Starting TTS initialization")
        speechHelper.initializeSpeech("Welcome to Eershad application. This page will help you navigate to the register or connection page.")
    }

    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            Log.d("HomeScreen", "Disposing com.example.malvoayant.ui.utils.SpeechHelper")
            speechHelper.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable {
                speechHelper.speak(
                    "If you encounter difficulties, press anywhere to activate TalkBack. " +
                            "Press the top left corner to hear the page description."
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.darkBlue)
                ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            HeaderBar(
                pageType = "home",
                onSpeakHelp = { speechHelper.initializeSpeech("Welcome to Eershad application. This page will help you navigate to the register or connection page.") }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp)) // Apply rounded corners to the whole Column
                    .background(Color.White), // Background color with rounded corners
                horizontalAlignment = Alignment.CenterHorizontally
            )
            { Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Please choose to sign up or log in to your existing account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontFamily = PlusJakartaSans,
                    color = AppColors.writingBlue,
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .clickable {
                            speechHelper.speak("Please choose to sign up or log in to your existing account")
                        }
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Register Button
                NavigationButton(
                    text = "REGISTER",
                    icon = painterResource(id = R.drawable.ic_register),
                    onClick = {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                            // Double click detected
                            // Cancel any pending speech from single click
                            pendingSpeechJob.value?.cancel()

                            // Perform double-click action immediately
                            navController.navigate(Destination.Registration.route)
                        } else {
                            // Single click - delay the speech
                            pendingSpeechJob.value = scope.launch {
                                // Wait to see if this becomes a double click
                                delay(doubleClickTimeWindow)

                                // If we reach here, no double-click happened
                                speechHelper.speak("Register button, navigating to registration page.")
                            }
                        }

                        lastClickTime.value = currentTime
                    }


                )

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
                    text = "LOGIN",
                    icon = painterResource(id = R.drawable.ic_login),
                    onClick = {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                            // Double click detected
                            // Cancel any pending speech from single click
                            pendingSpeechJob.value?.cancel()

                            // Perform double-click action immediately
                            navController.navigate(Destination.Login.route)
                        } else {
                            // Single click - delay the speech
                            pendingSpeechJob.value = scope.launch {
                                // Wait to see if this becomes a double click
                                delay(doubleClickTimeWindow)

                                // If we reach here, no double-click happened
                                speechHelper.speak("Login button, navigating to login page.")
                            }
                        }

                        lastClickTime.value = currentTime
                    }
                ) }

        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
        }
    }
}

