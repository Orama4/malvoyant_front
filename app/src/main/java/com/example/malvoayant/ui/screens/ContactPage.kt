package com.example.malvoayant.ui.screens
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import kotlinx.coroutines.delay

// Data class for contact information
data class Contact(
    val name: String,
    val phoneNumber: String
)

@Composable
fun PhoneNumbersScreen(
    context: Context,
    navController: NavHostController
) {
    // Dummy contact data
    val contacts = remember {
        listOf(
            Contact("John", "0563552378"),
            Contact("John", "0563552378"),
            Contact("John", "0563552378"),
            Contact("John", "0563552378"),
            Contact("John", "0563552378")
        )
    }

    // Initialize SpeechHelper for text-to-speech functionality
    val speechHelper = remember { SpeechHelper(context) }

    // Initialize speech when the screen is launched
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            speechHelper.speak("Phone numbers page. This page shows contact information. To call a number, double tap on the contact.")
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),

        ) {
            // Back button
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(50.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.back_icon), // Replace with your actual drawable name
                    contentDescription = "Back",
                    modifier = Modifier.size(35.dp)
                )
            }

            // Microphone icon
            IconButton(
                onClick = {
                    speechHelper.speak(
                        "You are on the SOS page. \n" +
                                "Tap on the top left corner to exit this page.\n" +
                                "tap on any button to activate talkback and hear the contact name\n" +
                                "tap twice on any button to call the contact "
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(Color.White)
                    .size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "Voice Guide",

                    )
            }
        }

        // List of contacts
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),


            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(contacts) { contact ->
                ContactItem(
                    contact = contact,
                    context = context,
                    speechHelper = speechHelper
                )
            }
        }


    }
}

@Composable
fun ContactItem(
    contact: Contact,
    context: Context,
    speechHelper: SpeechHelper
) {
    // Track click count and timing
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val clickTimeout = 3000L // 3 seconds timeout to reset click count

    // Timer to reset clicks
    LaunchedEffect(clickCount) {
        if (clickCount > 0) {
            delay(clickTimeout)
            clickCount = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color(0xFF1A1A2E),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = Color(0xFFF0F4F8),
                shape = RoundedCornerShape(16.dp)
            ).height(90.dp)
            .clickable {
                val currentTime = System.currentTimeMillis()

                // Reset click count if timeout has passed
                if (currentTime - lastClickTime > clickTimeout) {
                    clickCount = 0
                }

                clickCount++
                lastClickTime = currentTime

                if (clickCount == 1) {
                    // First click: read the contact information
                    speechHelper.speak(
                        "Contact ${contact.name}, Phone number ${contact.phoneNumber.replace("", " ")}. " +
                                "Press again to call this number."
                    )
                } else if (clickCount >= 2) {
                    // Second click: initiate phone call
                    speechHelper.speak("Calling ${contact.name}")

                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${contact.phoneNumber}")
                    }
                    context.startActivity(intent)

                    // Reset click count after initiating call
                    clickCount = 0
                }
            }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phone icon
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Phone",
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF1A1A2E)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Contact information
            Text(
                text = "${contact.name}: ${contact.phoneNumber}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = Color(0xFF1A1A2E)

            )
        }
    }
}

