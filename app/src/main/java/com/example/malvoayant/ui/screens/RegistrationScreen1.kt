package com.example.malvoayant.ui.screens

import android.Manifest
import android.R.attr
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malvoayant.R
import com.example.malvoayant.ui.components.*
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import android.R.attr.value
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.malvoayant.ui.utils.fixSpokenEmail
import com.example.malvoayant.ui.utils.startListening
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun RegistrationScreen1(context: Context) {
    var step by remember { mutableStateOf(0) }
    val textStates = remember { mutableStateListOf("", "", "") }

    var textState by remember { mutableStateOf("") }

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
    // Create speech helper using application context for lifecycle safety


    // State to track initialization
    var ttsInitialized by remember { mutableStateOf(false) }


    var hasMoved by remember { mutableStateOf(false) }


// Cette ligne réinitialise la valeur à chaque fois que l’écran apparaît
    LaunchedEffect(Unit) {
        textState = ""
    }

    val speechHelper = remember { SpeechHelper(context) }

    val labels = listOf("Email", "Password", "Phone Number")
    val placeholders = listOf("Enter your email", "Enter your password", "Enter your phone number")
    val icons = listOf(
        painterResource(id = R.drawable.ic_email),
        painterResource(id = R.drawable.ic_password),
        painterResource(id = R.drawable.ic_phone)
    )

    LaunchedEffect(Unit) {
        Log.d("RegistrationScreen", "Starting TTS initialization")
        speechHelper.initializeSpeech("This is the registration page. Enter your ${labels[step]}")
    }

    LaunchedEffect(step) {
        speechHelper.speak("Enter your ${labels[step]}")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { hasMoved = false }, // Reset flag when drag ends
                    onHorizontalDrag = { _, dragAmount ->
                        if (!hasMoved) { // Only trigger once per gesture
                            when {
                                dragAmount < -50 && step < labels.size - 1 -> { // Swipe left
                                    step++
                                    hasMoved = true
                                }
                                dragAmount > 50 && step > 0 -> { // Swipe right
                                    step--
                                    hasMoved = true
                                }
                            }
                        }
                    }
                )
            }


    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(AppColors.darkBlue),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            HeaderBar(
                pageType = "register",
                onSpeakHelp = {
                    speechHelper.speak("This is the registration page. Enter your ${labels[step]}")
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
                MyTextField(
                    text = labels[step],
                    content = labels[step],
                    placeHolder = placeholders[step],
                    icon = icons[step],
                    isPassword = step == 1,
                    value = textStates[step], // Utiliser la liste
                    onValueChange = { textStates[step] = it }, // Stocker la valeur actuelle
                    onDone = {
                        if (step < labels.size - 1) {
                            step++ // Passer à l'étape suivante
                        }
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
