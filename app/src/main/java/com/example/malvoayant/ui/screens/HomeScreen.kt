package com.yourpackagename.screens

import android.content.Context
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
import com.example.malvoayant.R
import com.example.malvoayant.ui.components.NavigationButton
import com.example.malvoayant.ui.components.HeaderBar
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
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
            modifier = Modifier.fillMaxSize()
                .background(AppColors.darkBlue)
                ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            HeaderBar(
                pageType = "home",
                onSpeakHelp = {
                    speechHelper.speak("Welcome to Irchad application. This page will help you navigate to the register or connection page.")
                }
            )
            Column(
                modifier = Modifier.fillMaxSize()
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
                        speechHelper.speak("Register button, navigating to registration page.")
                        // Add navigation logic here
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
                        speechHelper.speak("Login button, navigating to login page.")
                        // Add navigation logic here
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


@Preview(showBackground = true, name = "Home Screen Preview")
@Composable
fun HomeScreenPreview() {
    HomeScreen(context = LocalContext.current)
}
