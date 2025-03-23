package com.example.malvoayant.ui.screens
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.navigation.Screen
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.utils.SpeechHelper

@Composable
fun SearchScreen(
    context: Context,
    navController: NavHostController
) {
    // Initialize SpeechHelper for text-to-speech functionality

    val searchText = remember { mutableStateOf("") }

    // Counter to track clicks on buttons
    val helperClickCount = remember { mutableStateOf(0) }
    val sosClickCount = remember { mutableStateOf(0) }
    val repairClickCount = remember { mutableStateOf(0) }

    // Timers to reset click counts
    var helperClickTimer by remember { mutableStateOf<java.util.Timer?>(null) }
    var sosClickTimer by remember { mutableStateOf<java.util.Timer?>(null) }
    var repairClickTimer by remember { mutableStateOf<java.util.Timer?>(null) }

    // Initialize speech when the screen is launched
    val speechHelper = remember { SpeechHelper(context) }
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            speechHelper.speak("Search page. You can search for points of interest. Use the buttons at the bottom for help, SOS, or repair services.")
        }
    }

    // Clean up when the screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
            helperClickTimer?.cancel()
            sosClickTimer?.cancel()
            repairClickTimer?.cancel()
        }
    }

    Column(
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
        // Top navigation bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)

        ) {
            // Back button in top left
            IconButton(
                onClick = { navController.navigateUp()},
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(50.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.back_icon), // Replace with your actual drawable name
                    contentDescription = "Back" ,
                            modifier = Modifier.size(35.dp)
                )
            }

            // Description button in top right
            IconButton(
                onClick = {
                    speechHelper.speak("This is the search page. You can search for points of interest. Use the search bar to find locations. The buttons at the bottom provide different services.")
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(Color.White)
                    .size(40.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.mic), // Replace with your actual drawable name
                    contentDescription = "Back",

                )
            }



        }

        // Search field
        var searchText by remember { mutableStateOf("") } // State to hold the text input

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(100.dp) // Increased height
                .border(2.dp, AppColors.darkBlue, RoundedCornerShape(8.dp))
                .background(AppColors.lightBlue, RoundedCornerShape(8.dp))
                .clickable {
                    speechHelper.speak("Search field. Click to search for points of interest.")
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = AppColors.darkBlue,
                            modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // TextField for text input
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it }, // Update the state when text changes
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    placeholder = {
                        Text(
                            text = "Find your POI...",
                            color = AppColors.darkBlue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,

                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = AppColors.darkBlue, // Text color when focused
                        unfocusedTextColor = AppColors.darkBlue, // Text color when not focused
                        focusedContainerColor = Color.Transparent, // Background color when focused
                        unfocusedContainerColor = Color.Transparent, // Background color when not focused
                        cursorColor = AppColors.darkBlue, // Cursor color
                        focusedIndicatorColor = Color.Transparent, // Hide the underline when focused
                        unfocusedIndicatorColor = Color.Transparent // Hide the underline when not focused
                    ),
                    singleLine = true, // Ensure the text field is single line
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Spacer to push buttons to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Bottom navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Helper button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.primary)
                    .clickable {
                        helperClickCount.value++

                        if (helperClickCount.value == 1) {
                            speechHelper.speak("Helper button. Press again to navigate to the helper page.")

                            // Reset click count after 3 seconds if not clicked again
                            helperClickTimer?.cancel()
                            helperClickTimer = java.util.Timer().apply {
                                schedule(object : java.util.TimerTask() {
                                    override fun run() {
                                        helperClickCount.value = 0
                                    }
                                }, 3000)
                            }
                        } else if (helperClickCount.value >= 2) {
                            helperClickCount.value = 0
                            helperClickTimer?.cancel()
                            navController.navigate(Screen.Helper.route)

                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HELPER",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // SOS button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.primary)
                    .clickable {
                        sosClickCount.value++

                        if (sosClickCount.value == 1) {
                            speechHelper.speak("SOS button. Press again to navigate to the SOS page.")

                            // Reset click count after 3 seconds if not clicked again
                            sosClickTimer?.cancel()
                            sosClickTimer = java.util.Timer().apply {
                                schedule(object : java.util.TimerTask() {
                                    override fun run() {
                                        sosClickCount.value = 0
                                    }
                                }, 3000)
                            }
                        } else if (sosClickCount.value >= 2) {
                            sosClickCount.value = 0
                            sosClickTimer?.cancel()
                            navController.navigate(Screen.PhoneNumbers.route)

                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Repair button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.primary)
                    .clickable {
                        repairClickCount.value++

                        if (repairClickCount.value == 1) {
                            speechHelper.speak("Repair button. Press again to navigate to the repair page.")

                            // Reset click count after 3 seconds if not clicked again
                            repairClickTimer?.cancel()
                            repairClickTimer = java.util.Timer().apply {
                                schedule(object : java.util.TimerTask() {
                                    override fun run() {
                                        repairClickCount.value = 0
                                    }
                                }, 3000)
                            }
                        } else if (repairClickCount.value >= 2) {
                            repairClickCount.value = 0
                            repairClickTimer?.cancel()
                            navController.navigate("repair/CONNECTED?date=12.03.2025&time=17:00")

                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "REPAIR",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }


    }
}

