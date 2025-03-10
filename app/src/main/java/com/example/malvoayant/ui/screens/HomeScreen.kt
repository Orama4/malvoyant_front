package com.yourpackagename.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malvoayant.ui.components.NavigationButton
import com.example.malvoayant.ui.components.HeaderBar
import com.example.malvoayant.ui.utils.SpeechHelper

@Composable
fun HomeScreen(context: Context) {
    val speechHelper = remember { SpeechHelper(context) }

    LaunchedEffect(context) {
        speechHelper.initializeSpeech {
            speechHelper.speak("Welcome to Irchad application. This page will help you navigate to the register or connection page. Press the minus button to go to the register page and the plus button to go to the connection page.")
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            HeaderBar(
                pageType = "home",
                onSpeakHelp = {
                    speechHelper.speak("Welcome to Irchad application. This page will help you navigate to the register or connection page.")
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Please choose to sign up or log in to your existing account",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable {
                        speechHelper.speak("Please choose to sign up or log in to your existing account")
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Register Button
            NavigationButton(
                text = "REGISTER 🧑",
                onClick = {
                    speechHelper.speak("Register button, navigating to registration page.")
                    // Add navigation logic here
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "OR",
                fontSize = 18.sp,
                modifier = Modifier.clickable {
                    speechHelper.speak("Or")
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Login Button
            NavigationButton(
                text = "LOGIN ➡️",
                onClick = {
                    speechHelper.speak("Login button, navigating to login page.")
                    // Add navigation logic here
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
        }
    }
}


@Preview(showBackground = true, name = "Home Screen Preview")
@Composable
fun HomeScreenPreview() {
    HomeScreen(context = LocalContext.current)
}
