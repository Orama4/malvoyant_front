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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import com.example.malvoayant.viewmodels.ContactViewModel
import kotlinx.coroutines.delay

// Data class for contact information
data class Contact(
    val name: String,
    val phoneNumber: String
)

@Composable
fun PhoneNumbersScreen(
    context: Context,
    navController: NavHostController,
    viewModel: ContactViewModel
) {
    // Initialize SpeechHelper for text-to-speech functionality
    val speechHelper = remember { SpeechHelper(context) }

    // Collect contacts from ViewModel
    val contacts by viewModel.contacts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // State for add contact dialog
    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }

    // Initialize speech when the screen is launched
    LaunchedEffect(Unit) {
        speechHelper.initializeSpeech {
            speechHelper.speak("Phone numbers page. This page shows emergency contacts. To call a number, double tap on the contact.")
            // Fetch contacts when screen loads
            viewModel.fetchEmergencyContacts()
        }
    }

    // Clean up when the screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
        }
    }

    // Show add contact dialog
    if (showAddContactDialog) {
        AddContactDialog(
            name = newContactName,
            phone = newContactPhone,
            onNameChange = { newContactName = it },
            onPhoneChange = { newContactPhone = it },
            onConfirm = {
                viewModel.addEmergencyContact(newContactName, newContactPhone)
                newContactName = ""
                newContactPhone = ""
                showAddContactDialog = false
            },
            onDismiss = {
                showAddContactDialog = false
                newContactName = ""
                newContactPhone = ""
            }
        )
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
                    painter = painterResource(id = R.drawable.back_icon),
                    contentDescription = "Back",
                    modifier = Modifier.size(35.dp)
                )
            }

            // Title
            Text(
                text = "Emergency Contacts",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.align(Alignment.Center)
            )

            // Microphone icon
            IconButton(
                onClick = {
                    speechHelper.speak(
                        "You are on the Emergency Contacts page. \n" +
                                "Tap on the top left corner to exit this page.\n" +
                                "Tap on any contact to activate talkback and hear the contact name.\n" +
                                "Tap twice on any contact to call them. \n" +
                                "Use the plus button at the bottom to add a new contact."
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

        // Error message
        if (error != null) {
            Text(
                text = error ?: "",
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // Loading indicator
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
            if (contacts.isEmpty() && !loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No emergency contacts yet. Add your first contact.",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(contacts) { contact ->
                    ContactItem(
                        contact = contact,
                        context = context,
                        speechHelper = speechHelper
                    )
                }
            }

            // Extra space at bottom
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating action button for adding new contacts
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    showAddContactDialog = true
                    speechHelper.speak("Add a new emergency contact")
                },
                containerColor = Color(0xFF1A1A2E),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact"
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
            )
            .height(90.dp)
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

@Composable
fun AddContactDialog(
    name: String,
    phone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Emergency Contact",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        enabled = name.isNotBlank() && phone.isNotBlank() && phone.length >= 10,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E))
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}