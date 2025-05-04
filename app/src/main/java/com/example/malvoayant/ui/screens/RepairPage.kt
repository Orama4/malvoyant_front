package com.example.malvoayant.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper

enum class DeviceStatus {
    CONNECTED, DISCONNECTED, DOWN
}

data class Meeting(val date: String, val time: String)

@Composable
fun StatusMeetingScreen(
    context: Context,
    navController: NavHostController,
    deviceStatus: DeviceStatus,
    meeting: Meeting? = null
) {
    val speechHelper = remember { SpeechHelper(context) }

    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            val statusMessage = when (deviceStatus) {
                DeviceStatus.CONNECTED -> "Your device is connected."
                DeviceStatus.DISCONNECTED -> "Your device is disconnected."
                DeviceStatus.DOWN -> "Your device is down."
            }
            val meetingMessage = meeting?.let { "You have a meeting with the technician on ${it.date} at ${it.time}." }
                ?: "You don't have any meeting with the technician."
            speechHelper.speak("$statusMessage $meetingMessage")
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechHelper.shutdown() }
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
            },
    ) {
        TopNavigationBar(speechHelper,navController)
        Spacer(modifier = Modifier.height(64.dp))
        StatusCard(deviceStatus, speechHelper)
        Spacer(modifier = Modifier.height(64.dp))
        MeetingInfoCard(deviceStatus, meeting, speechHelper)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun TopNavigationBar( speechHelper: SpeechHelper,  navController: NavHostController,) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),

    ) {
        IconButton(onClick = { navController.navigateUp() }, modifier = Modifier
            .align(Alignment.CenterStart)
            .size(50.dp)) {
            Image(
                painter = painterResource(id = R.drawable.back_icon),
                contentDescription = "Back",
                modifier = Modifier.size(35.dp)
            )
        }
        IconButton(onClick = {
            speechHelper.speak("You are on the Maintenance page. Tap on the top left corner to exit this page. " +
                    "Tap once on buttons to explore functionalities using TalkBack or to read texts.")
        },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(Color.White)
                .size(40.dp)) {
            Image(
                painter = painterResource(id = R.drawable.mic),
                contentDescription = "Voice Guide",

            )
        }
    }
}

@Composable
fun StatusCard(deviceStatus: DeviceStatus, speechHelper: SpeechHelper) {
    val statusMessage = when (deviceStatus) {
        DeviceStatus.CONNECTED -> "Your device is Connected"
        DeviceStatus.DISCONNECTED -> "Your device is Disconnected"
        DeviceStatus.DOWN -> "Your device is Down"
    }
    val statusColor = when (deviceStatus) {
        DeviceStatus.CONNECTED -> Color(0xFF2E7D32)
        DeviceStatus.DISCONNECTED -> Color(0xFFFF8800)
        DeviceStatus.DOWN -> Color(0xFFD32F2F)
    }
    val backgroundColor = when (deviceStatus) {
        DeviceStatus.CONNECTED -> Color(0xFFE8F5E9)
        DeviceStatus.DISCONNECTED -> Color(0xFFFFF3E0)
        DeviceStatus.DOWN -> Color(0xFFFFEBEE)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { speechHelper.speak(statusMessage) }
            .border(2.dp, statusColor, RoundedCornerShape(16.dp))
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = statusMessage, fontSize = 24.sp, fontWeight = FontWeight.Bold,fontFamily = PlusJakartaSans, color = statusColor)
            Spacer(modifier = Modifier.height(8.dp))
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
                tint = statusColor
            )
        }
    }
}

@Composable
fun MeetingInfoCard(deviceStatus: DeviceStatus, meeting: Meeting?, speechHelper: SpeechHelper) {
    val message = when {
        deviceStatus == DeviceStatus.DOWN && meeting == null -> "A notification was sent to the technician"
        meeting == null -> "You don't have any meeting with technician"
        else -> "You have a meeting with the technician on ${meeting?.date} at ${meeting?.time}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(16.dp))
            .clickable { speechHelper.speak(message) }
            .background(Color(0xFFF0F4F8), RoundedCornerShape(16.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            color = Color(0xFF1A1A2E),
            textAlign = TextAlign.Center
        )
    }
}

