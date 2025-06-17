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
import com.example.malvoayant.data.api.RegisterRequest
import com.example.malvoayant.navigation.Screen
import com.example.malvoayant.ui.utils.fixSpokenEmail
import com.example.malvoayant.ui.utils.startListening
import com.example.malvoayant.data.viewmodels.AuthViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun RegistrationScreen1(context: Context, viewModel: AuthViewModel, navController: NavController) {
    var step by remember { mutableStateOf(0) }
    val textStates = remember { mutableStateListOf("", "", "") }

    var textState by remember { mutableStateOf("") }

    var emailState by remember { mutableStateOf("") }
    var passwordState by remember { mutableStateOf("") }
    var phoneState by remember { mutableStateOf("") }
    var firstNameState by remember { mutableStateOf("") }
    var lastNameState by remember { mutableStateOf("") }
    var addressState by remember { mutableStateOf("") }





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
                when (step) {
                    0 -> emailState = fixSpokenEmail(recognizedText)
                    1 -> passwordState = recognizedText
                    2 -> phoneState = recognizedText
                    3 -> firstNameState = recognizedText
                    4 -> lastNameState = recognizedText
                    5 -> addressState = recognizedText
                }
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

    val labels = listOf("Email", "Password", "Phone Number", "First Name", "Last Name", "Address", "Submit")
    val placeholders = listOf("Enter your email", "Enter your password", "Enter your phone number", "Enter your first name", "Enter your last name", "Enter your address", "")
    val icons = listOf(
        painterResource(id = R.drawable.ic_email),
        painterResource(id = R.drawable.ic_password),
        painterResource(id = R.drawable.ic_phone)
    )

    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            speechHelper.speak("This is the registration page. Enter your ${labels[step]}")

        }
    }

    LaunchedEffect(step) {
        if (step == 6) {
            speechHelper.speak("Click on the button in the middle of the screen to register.")
        } else {
            speechHelper.speak("Enter your ${labels[step]}")
        }
    }

    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val registerSuccess by viewModel.registerSuccess.collectAsState()

    LaunchedEffect(registerSuccess) {
        if (registerSuccess) {
            navController.navigate(Screen.Login.route)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                when (step) {
                    0 -> MyTextField(
                        text = "Email",
                        content = "Email",
                        placeHolder = "Enter your email",
                        icon = painterResource(id = R.drawable.ic_email),
                        value = emailState,
                        onValueChange = { emailState = it },
                        onDone = { step++ }
                    )
                    1 -> MyTextField(
                        text = "Password",
                        content = "Password",
                        placeHolder = "Enter your password",
                        icon = painterResource(id = R.drawable.ic_password),
                        isPassword = true,
                        value = passwordState,
                        onValueChange = { passwordState = it },
                        onDone = { step++ }
                    )
                    2 -> MyTextField(
                        text = "Phone Number",
                        content = "Phone Number",
                        placeHolder = "Enter your phone number",
                        icon = painterResource(id = R.drawable.ic_phone),
                        value = phoneState,
                        onValueChange = { phoneState = it },
                        onDone = { step++ }
                    )
                    3 -> MyTextField(
                        text = "First Name",
                        content = "First Name",
                        placeHolder = "Enter your first name",
                        icon = painterResource(id = R.drawable.ic_register),
                        value = firstNameState,
                        onValueChange = { firstNameState = it },
                        onDone = { step++ }
                    )
                    4 -> MyTextField(
                        text = "Last Name",
                        content = "Last Name",
                        placeHolder = "Enter your last name",
                        icon = painterResource(id = R.drawable.ic_register),
                        value = lastNameState,
                        onValueChange = { lastNameState = it },
                        onDone = { step++ }
                    )
                    5 -> MyTextField(
                        text = "Address",
                        content = "Address",
                        placeHolder = "Enter your address",
                        icon = painterResource(id = R.drawable.ic_location),
                        value = addressState,
                        onValueChange = { addressState = it },
                        onDone = { step++ }
                    )
                }

                if (step == 6) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading) {
                            CircularProgressIndicator(color = AppColors.darkBlue)
                        } else {
                            NavigationButton(
                                text = "REGISTER",
                                icon = painterResource(id = R.drawable.ic_register),
                                onClick = {
                                    val request = RegisterRequest(
                                        email = emailState,
                                        password = passwordState,
                                        firstname = firstNameState,
                                        lastname = lastNameState,
                                        phonenumber = phoneState,
                                        address = addressState
                                    )
                                    viewModel.register(request)
                                }
                            )
                        }
                    }
                }



                Spacer(modifier = Modifier.height(24.dp))
                if (step < 6) {
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
                }



                Spacer(modifier = Modifier.height(24.dp))
                // Login Button
                if (step < 6) {
                    NavigationButton(
                        text = "CLICK TO SPELL",
                        icon = painterResource(id = R.drawable.ic_mic),
                        onClick = {
                            val currentTime = System.currentTimeMillis()

                            if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                                pendingSpeechJob.value?.cancel()
                                onspeakHelp()
                            } else {
                                pendingSpeechJob.value = scope.launch {
                                    delay(doubleClickTimeWindow)
                                    speechHelper.speak("Micro button, click two times to activate voice function.")
                                }
                            }

                            lastClickTime.value = currentTime
                        }
                    )
                }


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