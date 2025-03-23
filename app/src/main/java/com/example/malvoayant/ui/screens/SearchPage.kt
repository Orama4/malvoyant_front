package com.example.malvoayant.ui.screens
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.navigation.Screen
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import com.example.voicerecorder.VoiceRecorderButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    context: Context,
    navController: NavHostController
) {
    val searchText = remember { mutableStateOf("") }
    val helperClickCount = remember { mutableStateOf(0) }
    val sosClickCount = remember { mutableStateOf(0) }
    val repairClickCount = remember { mutableStateOf(0) }

    var helperClickTimer by remember { mutableStateOf<java.util.Timer?>(null) }
    var sosClickTimer by remember { mutableStateOf<java.util.Timer?>(null) }
    var repairClickTimer by remember { mutableStateOf<java.util.Timer?>(null) }

    var lastClickTime = remember { mutableStateOf(0L) }
    val doubleClickTimeWindow = 300L

    // Create a coroutine scope tied to this composable
    val scope = rememberCoroutineScope()

    // Reference to the job for pending speech
    val pendingSpeechJob = remember { mutableStateOf<Job?>(null) }

    val speechHelper = remember { SpeechHelper(context) }
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            speechHelper.speak("Search page. You can search for points of interest. Use the buttons at the bottom for help, SOS, or repair services.")
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(50.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back_icon),
                    contentDescription = "Back",
                    modifier = Modifier.size(35.dp)
                )
            }

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
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "Description"
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(100.dp)
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

                TextField(
                    value = searchText.value,

                    onValueChange = { searchText.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    placeholder = {
                        Text(
                            text = "Find your POI...",
                            color = AppColors.darkBlue,
                            fontSize = 24.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = AppColors.darkBlue,
                        unfocusedTextColor = AppColors.darkBlue,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = AppColors.darkBlue,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            VoiceRecorderButton(
                onVoiceInput = { recognizedText ->
                    searchText.value = recognizedText
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.primary)
                    .clickable {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                            // Double click detected
                            // Cancel any pending speech from single click
                            pendingSpeechJob.value?.cancel()

                            // Perform double-click action immediately
                            navController.navigate(Screen.Helper.route)
                        } else {
                            // Single click - delay the speech
                            pendingSpeechJob.value = scope.launch {
                                // Wait to see if this becomes a double click
                                delay(doubleClickTimeWindow)

                                // If we reach here, no double-click happened
                                speechHelper.speak("Repair button, click to naviguate to repair page .")
                            }
                        }

                        lastClickTime.value = currentTime
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HELPER",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.primary)
                    .clickable {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                            // Double click detected
                            // Cancel any pending speech from single click
                            pendingSpeechJob.value?.cancel()

                            // Perform double-click action immediately
                            navController.navigate(Screen.Helper.route)
                        } else {
                            // Single click - delay the speech
                            pendingSpeechJob.value = scope.launch {
                                // Wait to see if this becomes a double click
                                delay(doubleClickTimeWindow)

                                // If we reach here, no double-click happened
                                speechHelper.speak("SOS button")
                            }
                        }

                        lastClickTime.value = currentTime
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.primary)
                    .clickable {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastClickTime.value < doubleClickTimeWindow) {
                            // Double click detected
                            // Cancel any pending speech from single click
                            pendingSpeechJob.value?.cancel()

                            // Perform double-click action immediately
                            navController.navigate("repair/CONNECTED?date=12.03.2025&time=17:00")
                        } else {
                            // Single click - delay the speech
                            pendingSpeechJob.value = scope.launch {
                                // Wait to see if this becomes a double click
                                delay(doubleClickTimeWindow)

                                // If we reach here, no double-click happened
                                speechHelper.speak("Repair button")
                            }
                        }

                        lastClickTime.value = currentTime
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "REPAIR",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
