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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.malvoayant.R
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.utils.SpeechHelper
import com.example.malvoayant.data.viewmodels.ContactViewModel
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
            speechHelper.speak("Page des contacts d'urgence. Cette page affiche vos contacts d'urgence. Pour appeler un contact, appuyez deux fois dessus.")
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
                speechHelper.speak("Contact ajouté avec succès")
            },
            onDismiss = {
                showAddContactDialog = false
                newContactName = ""
                newContactPhone = ""
                speechHelper.speak("Ajout de contact annulé")
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.darkBlue)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header avec le même style que la page de connexion
            HeaderSection(
                navController = navController,
                speechHelper = speechHelper
            )

            // Contenu principal avec fond blanc arrondi
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titre de la section
                Text(
                    text = "Mes Contacts d'Urgence",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = AppColors.darkBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            speechHelper.speak("Section des contacts d'urgence")
                        }
                )

                // Error message
                if (error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = error ?: "",
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(16.dp),
                            fontFamily = PlusJakartaSans
                        )
                    }
                }

                // Loading indicator
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = AppColors.darkBlue,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chargement des contacts...",
                                color = AppColors.darkBlue,
                                fontFamily = PlusJakartaSans
                            )
                        }
                    }
                }

                // List of contacts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (contacts.isEmpty() && !loading) {
                        item {
                            EmptyStateCard(speechHelper = speechHelper)
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

                    // Extra space at bottom for FAB
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // Floating action button avec style amélioré
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    showAddContactDialog = true
                    speechHelper.speak("Ouvrir le formulaire d'ajout de contact")
                },
                containerColor = AppColors.darkBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter un contact",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    navController: NavHostController,
    speechHelper: SpeechHelper
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        // Back button
        IconButton(
            onClick = {
                navController.navigateUp()
                speechHelper.speak("Retour à la page précédente")
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.back_icon),
                contentDescription = "Retour",
                modifier = Modifier.size(24.dp)
            )
        }

        // Title
        Text(
            text = "Contacts d'Urgence",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        // Help/Voice guide button
        IconButton(
            onClick = {
                speechHelper.speak(
                    "Vous êtes sur la page des contacts d'urgence. " +
                            "Appuyez sur le coin supérieur gauche pour revenir en arrière. " +
                            "Appuyez sur un contact pour entendre ses informations. " +
                            "Appuyez deux fois sur un contact pour l'appeler. " +
                            "Utilisez le bouton plus en bas à droite pour ajouter un nouveau contact."
                )
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.mic),
                contentDescription = "Guide vocal",
                modifier = Modifier.size(24.dp)
            )
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
    val clickTimeout = 3000L

    // Timer to reset clicks
    LaunchedEffect(clickCount) {
        if (clickCount > 0) {
            delay(clickTimeout)
            clickCount = 0
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp)
            )
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
                        "Contact ${contact.name}, numéro de téléphone ${contact.phoneNumber.replace("", " ")}. " +
                                "Appuyez à nouveau pour appeler ce numéro."
                    )
                } else if (clickCount >= 2) {
                    // Second click: initiate phone call
                    speechHelper.speak("Appel de ${contact.name}")

                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${contact.phoneNumber}")
                    }
                    context.startActivity(intent)

                    // Reset click count after initiating call
                    clickCount = 0
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AppColors.darkBlue.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            AppColors.darkBlue.copy(alpha = 0.1f),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Contact",
                        modifier = Modifier.size(28.dp),
                        tint = AppColors.darkBlue
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Contact information
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = contact.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = AppColors.darkBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.phoneNumber,
                        fontSize = 16.sp,
                        fontFamily = PlusJakartaSans,
                        color = AppColors.writingBlue
                    )
                }

                // Call button icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            AppColors.darkBlue.copy(alpha = 0.1f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Appeler",
                        modifier = Modifier.size(24.dp),
                        tint = AppColors.darkBlue
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(speechHelper: SpeechHelper) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                speechHelper.speak("Aucun contact d'urgence enregistré. Utilisez le bouton plus pour ajouter votre premier contact.")
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppColors.darkBlue.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun contact d'urgence",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = AppColors.darkBlue,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ajoutez vos premiers contacts d'urgence pour les avoir rapidement à portée de main",
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
                color = AppColors.writingBlue,
                textAlign = TextAlign.Center
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
                .padding(16.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            AppColors.darkBlue.copy(alpha = 0.1f),
                            RoundedCornerShape(36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = AppColors.darkBlue
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nouveau Contact d'Urgence",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = PlusJakartaSans,
                    color = AppColors.darkBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(
                            "Nom du contact",
                            fontFamily = PlusJakartaSans
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.darkBlue,
                        focusedLabelColor = AppColors.darkBlue,
                        cursorColor = AppColors.darkBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone field
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = {
                        Text(
                            "Numéro de téléphone",
                            fontFamily = PlusJakartaSans
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.darkBlue,
                        focusedLabelColor = AppColors.darkBlue,
                        cursorColor = AppColors.darkBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.darkBlue
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(AppColors.darkBlue, AppColors.darkBlue))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Annuler",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = name.isNotBlank() && phone.isNotBlank() && phone.length >= 6,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.darkBlue,
                            disabledContainerColor = AppColors.darkBlue.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Enregistrer",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}