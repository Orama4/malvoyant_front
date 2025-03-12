package com.example.malvoayant.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper

// Dummy data for helper contact
object DummyData {
    const val HELPER_PHONE_NUMBER = "1234567890"
}

@Composable
fun HelperScreen(
    context: Context,
    navController: NavHostController
) {
    // State to track if position is being shared
    var isPositionShared by remember { mutableStateOf(false) }

    // State to track button clicks for double-tap functionality
    val shareButtonClickCount = remember { mutableStateOf(0) }
    var shareButtonClickTimer by remember { mutableStateOf<java.util.Timer?>(null) }

    // Initialize speech helper
    val speechHelper = remember { SpeechHelper(context) }

    // Initialize speech when the screen is launched
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            val positionStatus = if (isPositionShared)
                "You are currently sharing your position with your helper."
            else
                "You are not sharing your position with your helper."

            speechHelper.speak(
                "Helper page. You can share your location with your helper or call them for assistance. $positionStatus"
            )
        }
    }

    // Clean up when the screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
            shareButtonClickTimer?.cancel()
        }
    }

    // Function to call helper
    val callHelper = {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${DummyData.HELPER_PHONE_NUMBER}")
        }
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable {
                speechHelper.speak(
                    "Helper page. Double tap on buttons to activate them."
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with back button and mic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {



            // Back button
            IconButton(
                onClick = { navController.navigateUp()},
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(35.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.back_icon), // Replace with your actual drawable name
                    contentDescription = "Back",
                    modifier = Modifier.size(40.dp)
                )
            }
            // Voice assistance button
            IconButton(
                onClick = {
                    val positionStatus = if (isPositionShared)
                        "You are currently sharing your position with your helper."
                    else
                        "You are not sharing your position with your helper."

                    speechHelper.speak(
                        "Helper page. You can share your location with your helper or call them for assistance. $positionStatus"
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "Voice Guide",
                    modifier = Modifier.size(40.dp)

                )
            }
        }

        // Spacer to push content to the center

        // Location sharing button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,

            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Spacer(modifier = Modifier.weight(0.05f))
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F7FA))
                    .border(
                        width = 2.dp,
                        color = AppColors.darkBlue,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        shareButtonClickCount.value++

                        if (shareButtonClickCount.value == 1) {
                            // First click - announce action
                            val buttonText = if (isPositionShared)
                                "Click to stop sharing your position"
                            else
                                "Share your position with your helper"

                            speechHelper.speak(buttonText)

                            // Reset click count after 3 seconds
                            shareButtonClickTimer?.cancel()
                            shareButtonClickTimer = java.util.Timer().apply {
                                schedule(object : java.util.TimerTask() {
                                    override fun run() {
                                        shareButtonClickCount.value = 0
                                    }
                                }, 3000)
                            }
                        } else if (shareButtonClickCount.value >= 2) {
                            // Second click - execute action
                            isPositionShared = !isPositionShared

                            val confirmationMessage = if (isPositionShared)
                                "You are now sharing your position with your helper."
                            else
                                "You have stopped sharing your position with your helper."

                            speechHelper.speak(confirmationMessage)

                            // Reset click count
                            shareButtonClickCount.value = 0
                            shareButtonClickTimer?.cancel()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPositionShared)
                            "Click to stop sharing your position"
                        else
                            "Share Your Position With Your Helper",
                        textAlign = TextAlign.Center,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = AppColors.darkBlue,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_location), // Use your location icon resource
                        contentDescription = null,
                        tint = AppColors.darkBlue,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(top = 8.dp)
                    )
                }
            }

            // Call helper button
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.primary)
                    .clickable {
                        speechHelper.speak("Calling your helper")
                        callHelper()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Call Your Helper",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.ic_phoen), // Use your phone icon resource
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(0.05f))
        }

    }
}

