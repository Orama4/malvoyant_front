package com.example.malvoayant.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.example.malvoayant.ui.components.HeaderBar
import com.example.malvoayant.ui.components.MyTextField
import com.example.malvoayant.ui.components.NavigationButton
import com.example.malvoayant.ui.components.PageIndicator
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper

@Composable
fun RegistrationScreen1(context: Context) {
    val speechHelper = remember { SpeechHelper(context) }

    LaunchedEffect(context) {
        speechHelper.initializeSpeech {
            speechHelper.speak("This page is the registration page, you can here write or spell your email address, tap one time in order to activate talk back , else tap 2 times to select the target elmeent, you can activate voice function byt taping on the top right corner.")
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
                pageType = "register",
                onSpeakHelp = {
                    speechHelper.speak("This page is the registration page, you can here write or spell your email address, tap one time in order to activate talk back , else tap 2 times to select the target elmeent, you can activate voice function byt taping on the top right corner.")
                }
            )
            Column(
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(20.dp)) // Apply rounded corners to the whole Column

                    .background(Color.White), // Background color with rounded corners
                horizontalAlignment = Alignment.CenterHorizontally
            )
            { Spacer(modifier = Modifier.height(20.dp))

                // Email text field
                MyTextField(
                    text = "Email",
                    content="email",
                    placeHolder="Enter your email",
                    icon = painterResource(id = R.drawable.ic_email),
                    onClick = {
                        speechHelper.speak("Email text field, tap to enter your email ")
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
                    text = "CLICK TO SPELL",
                    icon = painterResource(id = R.drawable.ic_mic),
                    onClick = {
                        speechHelper.speak("Micro button, click two times to activate voice function.")
                        // Add navigation logic here
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 30.dp), // Add some space from the bottom
                    contentAlignment = Alignment.BottomCenter // Stick it to the bottom
                ) {
                    PageIndicator(totalDots = 5, selectedIndex = 0)
                }}

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
