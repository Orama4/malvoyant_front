package com.example.malvoayant.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malvoayant.R
import com.example.malvoayant.ui.utils.SpeechHelper

enum class DeviceStatus {
    CONNECTED,
    DISCONNECTED,
    DOWN
}

// Data class for meeting information
data class Meeting(
    val date: String,
    val time: String
)

@Composable
fun StatusMeetingScreen(
    context: Context,
    onNavigateBack: () -> Unit,
    deviceStatus: DeviceStatus,
    meetings: List<Meeting>
) {
    // Initialize SpeechHelper for text-to-speech functionality
    val speechHelper = remember { SpeechHelper(context) }

    // Initialize speech when the screen is launched
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            // Different voice message based on status
            when (deviceStatus) {
                DeviceStatus.CONNECTED ->
                    speechHelper.speak("Your device is connected. ${getMeetingMessage(meetings)}")
                DeviceStatus.DISCONNECTED ->
                    speechHelper.speak("Your device is disconnected. ${getMeetingMessage(meetings)}")
                DeviceStatus.DOWN ->
                    speechHelper.speak("Your device is down. ${getMeetingMessage(meetings)}")
            }
        }
    }

    // Clean up when the screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable {
                speechHelper.speak("If you encounter difficulties, press anywhere to activate TalkBack. " +
                        "Press the top left corner to hear the page description.")
            },
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigateBack() },
                modifier = Modifier

                    .size(35.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.back_icon), // Replace with your actual drawable name
                    contentDescription = "Back",
                    modifier = Modifier.size(40.dp)
                )
            }

            // Microphone icon
            IconButton(
                onClick = {
                    speechHelper.speak(
                        "You are on the Maintenance page. \n" +
                                "Tap on the top left corner to exit this page.\n" +
                                "Tap once on buttons to explore functionalities using TalkBack or to read texts. "
                    )
                },
                modifier = Modifier

                    .size(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "Voice Guide",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))


        // Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable {
                    val statusMessage = when (deviceStatus) {
                        DeviceStatus.CONNECTED -> "Your device is Connected"
                        DeviceStatus.DISCONNECTED -> "Your device is Disconnected"
                        DeviceStatus.DOWN -> "Your device is Down"
                    }
                    speechHelper.speak(statusMessage)
                }
                .border(
                    width = 2.dp,
                    color = when (deviceStatus) {
                        DeviceStatus.CONNECTED -> Color(0xFF2E7D32)     // Green
                        DeviceStatus.DISCONNECTED -> Color(0xFFFF8800)  // Orange
                        DeviceStatus.DOWN -> Color(0xFFD32F2F)          // Red
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = when (deviceStatus) {
                        DeviceStatus.CONNECTED -> Color(0xFFE8F5E9)     // Light Green
                        DeviceStatus.DISCONNECTED -> Color(0xFFFFF3E0)  // Light Orange
                        DeviceStatus.DOWN -> Color(0xFFFFEBEE)          // Light Red
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (deviceStatus) {
                        DeviceStatus.CONNECTED -> "Your device is Connected"
                        DeviceStatus.DISCONNECTED -> "Your device is Disconnected"
                        DeviceStatus.DOWN -> "Your device is Down"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (deviceStatus) {
                        DeviceStatus.CONNECTED -> Color(0xFF2E7D32)     // Green
                        DeviceStatus.DISCONNECTED -> Color(0xFFFF8800)  // Orange
                        DeviceStatus.DOWN -> Color(0xFFD32F2F)          // Red
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status icon
                Icon(
                    painter = painterResource(
                        id = when (deviceStatus) {
                            DeviceStatus.CONNECTED -> R.drawable.ic_connected
                            DeviceStatus.DISCONNECTED -> R.drawable.ic_disconnected
                            DeviceStatus.DOWN -> R.drawable.ic_down
                        }
                    ),
                    contentDescription = "Status Icon",
                    modifier = Modifier.size(32.dp),
                    tint = when (deviceStatus) {
                        DeviceStatus.CONNECTED -> Color(0xFF2E7D32)     // Green
                        DeviceStatus.DISCONNECTED -> Color(0xFFFF8800)  // Orange
                        DeviceStatus.DOWN -> Color(0xFFD32F2F)          // Red
                    }
                )
            }
        }
                Spacer(modifier = Modifier.height(64.dp))

        // Meeting Information Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .border(
                    width = 2.dp,
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    val message = when {

                        deviceStatus == DeviceStatus.DOWN && meetings.isEmpty() -> "A notification was sent to the technician"
                        meetings.isEmpty() -> "You don't have any meeting with technician"
                        meetings.isNotEmpty() -> {
                            val meeting = meetings.first()
                            "You have a meeting with the technician on ${meeting.date} at ${meeting.time}"
                        }
                        else -> ""
                    }
                    speechHelper.speak(message)
                }
                .background(
                    color = Color(0xFFF0F4F8),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                deviceStatus == DeviceStatus.DOWN && meetings.isEmpty() -> {
                    Text(
                        text = "A notification was sent to the technician",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E),
                        textAlign = TextAlign.Center
                    )
                }
                meetings.isEmpty() -> {
                    Text(
                        text = "You don't have any meeting with technician",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E),
                        textAlign = TextAlign.Center
                    )
                }

                meetings.isNotEmpty() -> {
                    // Display the first meeting (for simplicity)
                    val meeting = meetings.first()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You have meeting with the technician",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = meeting.date,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = meeting.time,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))


    }
}

// Helper function to get appropriate meeting message for speech
private fun getMeetingMessage(meetings: List<Meeting>): String {
    return if (meetings.isEmpty()) {
        "You don't have any meeting with technician."
    } else {
        val meeting = meetings.first()
        "You have a meeting with technician on ${meeting.date} at ${meeting.time}."
    }
}

// Preview functions for different states
@Preview(showBackground = true)
@Composable
fun ConnectedWithMeetingPreview() {
    val meetings = listOf(Meeting("12.03.2025", "17:00 PM"))
    StatusMeetingScreen(
        context = LocalContext.current,
        onNavigateBack = {},
        deviceStatus = DeviceStatus.CONNECTED,
        meetings = meetings
    )
}

@Preview(showBackground = true)
@Composable
fun ConnectedNoMeetingPreview() {
    StatusMeetingScreen(
        context = LocalContext.current,
        onNavigateBack = {},
        deviceStatus = DeviceStatus.CONNECTED,
        meetings = emptyList()
    )
}

@Preview(showBackground = true)
@Composable
fun DisconnectedPreview() {
    StatusMeetingScreen(
        context = LocalContext.current,
        onNavigateBack = {},
        deviceStatus = DeviceStatus.DISCONNECTED,
        meetings = emptyList()
    )
}

@Preview(showBackground = true)
@Composable
fun DownWithNotificationPreview() {
    val meetings = listOf(Meeting("12.03.2025", "17:00 PM"))
    StatusMeetingScreen(
        context = LocalContext.current,
        onNavigateBack = {},
        deviceStatus = DeviceStatus.DOWN,
        meetings = meetings
    )
}
@Preview(showBackground = true)
@Composable
fun RepairScreenPreview() {
    val deviceStatus = remember { mutableStateOf(DeviceStatus.DOWN) }
    val meetings = remember {
        mutableStateOf(emptyList<Meeting>())

    }

    StatusMeetingScreen(
        context = LocalContext.current,
        onNavigateBack = { /* Your navigation logic */ },
        deviceStatus = deviceStatus.value,
        meetings = meetings.value
    )
}