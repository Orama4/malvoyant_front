package com.example.malvoayant.ui.screens

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
import android.util.Log


@Composable
fun LoginScreen(context: Context) {
    val textStates = remember { mutableStateListOf("", "") }



    var step by remember { mutableStateOf(0) }
    val speechHelper = remember { SpeechHelper(context) }

    val labels = listOf("Email", "Password")
    val placeholders = listOf("Enter your email", "Enter your password")
    val icons = listOf(
        painterResource(id = R.drawable.ic_email),
        painterResource(id = R.drawable.ic_password),

    )



    LaunchedEffect(Unit) {
        Log.d("LoginScreen", "Starting TTS initialization")
        speechHelper.initializeSpeech("This is the login page. Enter your ${labels[step]}")
    }
    LaunchedEffect(step) {
        speechHelper.speak("Enter your ${labels[step]}")
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
                        dragAmount > 50 && step > 0 -> { // Swipe right
                            step--
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
                        speechHelper.speak("Micro button, click two times to activate voice function.")
                        // Add navigation logic here
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
