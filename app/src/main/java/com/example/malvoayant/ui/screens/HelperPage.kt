package com.example.malvoayant.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone // Corrected Icon name
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle // Import TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.malvoayant.data.viewmodels.AuthViewModel
import com.example.malvoayant.data.viewmodels.ContactViewModel
import com.example.malvoayant.data.viewmodels.StepCounterViewModel
// Import YOUR AppColors and Font
import com.example.malvoayant.ui.theme.AppColors
import com.example.malvoayant.ui.theme.PlusJakartaSans
import com.example.malvoayant.ui.utils.SpeechHelper
import com.example.voicerecorder.VoiceRecorderButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class HelperStatus {
    CONFIRMED, PENDING, NOT_SET
}

@Composable
fun WowHelperButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.primary,
    contentColor: Color = Color.White, 
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    color:Color=AppColors.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
        ,
        shape = shape,
        border = BorderStroke(4.dp,color),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelperScreen(
    context: Context,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    contactViewModel: ContactViewModel,
    stepCounterViewModel: StepCounterViewModel
) {




    val scope = rememberCoroutineScope()
    val speechHelper = remember { SpeechHelper(context) }
    val doubleTapDelay = 500L
    var shareButtonClickJob by remember { mutableStateOf<Job?>(null) }

    var helperStatus by remember { mutableStateOf(HelperStatus.NOT_SET) }
    var helperEmail by remember { mutableStateOf("") }

    val helperID = contactViewModel.helperId.collectAsState()

    val hasHelper by contactViewModel.hasHelper.collectAsState()

    LaunchedEffect(Unit) {
        val user = authViewModel.getUserInfo()
        val userID = user?.id
        contactViewModel.checkIfUserHasHelper(userID)
    }

    LaunchedEffect(hasHelper) {
        helperStatus = when (hasHelper) {
            true -> HelperStatus.CONFIRMED
            false -> HelperStatus.NOT_SET
            else -> HelperStatus.NOT_SET
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
            shareButtonClickJob?.cancel()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Helper",
                        color = AppColors.darkBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AppColors.darkBlue,
                                        AppColors.darkBlue.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                },

            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
                .clickable {
                    speechHelper.speak(
                        "Helper page. Double tap on buttons to activate them."
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // UI Header - unchanged

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (helperStatus) {
                    HelperStatus.CONFIRMED -> {
                        ConfirmedHelperContent(
                            isPositionShared = stepCounterViewModel.isTracking,
                            onSharePositionClick = {
                                if (shareButtonClickJob?.isActive == true) {
                                    shareButtonClickJob?.cancel()
                                    shareButtonClickJob = null
                                     if(stepCounterViewModel.isTracking == true ){
                                         stepCounterViewModel.disconnect()
                                     }else {
                                         val user = authViewModel.getUserInfo()
                                         val userID = user?.id;
                                         if (userID != null) {
                                             helperID.value?.let {
                                                 stepCounterViewModel.connectToWebSocket(userID,
                                                     it
                                                 )
                                             }
                                         }
                                         stepCounterViewModel.startLocationTracking()


                                     }
                                    speechHelper.speak(
                                        if (stepCounterViewModel.isTracking) "Position sharing activated." else "Position sharing stopped."
                                    )
                                } else {
                                    speechHelper.speak(
                                        if (stepCounterViewModel.isTracking) "Stop sharing position button. Double tap to activate."
                                        else "Share position button. Double tap to activate."
                                    )
                                    shareButtonClickJob = scope.launch {
                                        delay(doubleTapDelay)
                                        shareButtonClickJob = null
                                    }
                                }
                            },
                            onCallHelperClick = {
                                speechHelper.speak("Calling your helper.")
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:777777777777")
                                }
                                context.startActivity(intent)
                            },
                            onRequestHelpClick = {
                                speechHelper.speak("Sending help request message.")
                                // Your message logic
                            }
                        )
                    }
                    HelperStatus.PENDING -> {
                        PendingHelperContent()
                    }
                    HelperStatus.NOT_SET -> {
                        NotSetHelperContent(
                            helperEmail = helperEmail,
                            onEmailChange = { helperEmail = it },
                            onRequestClick = {
                                if (helperEmail.isNotBlank() && helperEmail.contains("@")) {
                                    contactViewModel.assignHelperToEndUser(helperEmail)
                                    helperStatus = HelperStatus.PENDING
                                    speechHelper.speak("Request sent to $helperEmail.")
                                } else {
                                    speechHelper.speak("Please enter a valid email address first.")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/*
@Composable
fun HelperScreen(
    context: Context,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    contactViewModel: ContactViewModel
) {
    val scope = rememberCoroutineScope()
    val speechHelper = remember { SpeechHelper(context) }
    val doubleTapDelay = 500L
    var shareButtonClickJob by remember { mutableStateOf<Job?>(null) }

    var helperStatus by remember { mutableStateOf(HelperStatus.NOT_SET) }
    var helperEmail by remember { mutableStateOf("") }
    var isPositionShared by remember { mutableStateOf(false) }

    val hasHelper by contactViewModel.hasHelper.collectAsState()

    LaunchedEffect(Unit) {
        val user = authViewModel.getUserInfo()

        val userID = user?.id

            contactViewModel.checkIfUserHasHelper(userID)
    }

    LaunchedEffect(hasHelper) {
        helperStatus = when (hasHelper) {
            true -> HelperStatus.CONFIRMED
            false -> HelperStatus.NOT_SET
            else -> HelperStatus.NOT_SET
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
            shareButtonClickJob?.cancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .clickable {
                speechHelper.speak(
                    "Helper page. Double tap on buttons to activate them."
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // UI Header - unchanged

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (helperStatus) {
                HelperStatus.CONFIRMED -> {
                    ConfirmedHelperContent(
                        isPositionShared = isPositionShared,
                        onSharePositionClick = {
                            if (shareButtonClickJob?.isActive == true) {
                                shareButtonClickJob?.cancel()
                                shareButtonClickJob = null
                                isPositionShared = !isPositionShared
                                speechHelper.speak(
                                    if (isPositionShared) "Position sharing activated." else "Position sharing stopped."
                                )
                            } else {
                                speechHelper.speak(
                                    if (isPositionShared) "Stop sharing position button. Double tap to activate."
                                    else "Share position button. Double tap to activate."
                                )
                                shareButtonClickJob = scope.launch {
                                    delay(doubleTapDelay)
                                    shareButtonClickJob = null
                                }
                            }
                        },
                        onCallHelperClick = {
                            speechHelper.speak("Calling your helper.")
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:777777777777")
                            }
                            context.startActivity(intent)
                        },
                        onRequestHelpClick = {
                            speechHelper.speak("Sending help request message.")
                            // Your message logic
                        }
                    )
                }
                HelperStatus.PENDING -> {
                    PendingHelperContent()
                }
                HelperStatus.NOT_SET -> {
                    NotSetHelperContent(
                        helperEmail = helperEmail,
                        onEmailChange = { helperEmail = it },
                        onRequestClick = {
                            if (helperEmail.isNotBlank() && helperEmail.contains("@")) {

                                    contactViewModel.assignHelperToEndUser( helperEmail)
                                    helperStatus = HelperStatus.PENDING
                                    speechHelper.speak("Request sent to $helperEmail.")
                            } else {
                                speechHelper.speak("Please enter a valid email address first.")
                            }
                        }
                    )
                }
            }
        }
    }
}



@Composable
fun HelperScreen(
    context: Context,
    navController: NavHostController,
    contactViewModel: ContactViewModel
) {
    // State remains mostly the same
    var helperStatus by remember { mutableStateOf(HelperStatus.NOT_SET) } // Start as confirmed for demo
    var helperEmail by remember { mutableStateOf("") }
    var isPositionShared by remember { mutableStateOf(false) }

    // Double tap logic state
    val scope = rememberCoroutineScope()
    var shareButtonClickJob by remember { mutableStateOf<Job?>(null) }
    val doubleTapDelay = 500L // Delay for double tap detection

    // Initialize speech helper (unchanged)
    val speechHelper = remember { SpeechHelper(context) }

    // Initialize speech (unchanged logic)
    LaunchedEffect(Unit, helperStatus, isPositionShared) {
        speechHelper.initializeSpeech {
            val positionStatus = if (isPositionShared)
                "Position sharing is active."
            else
                "Position sharing is inactive."

            val helperMessage = when (helperStatus) {
                HelperStatus.CONFIRMED -> "Helper Confirmed. $positionStatus Use the buttons to interact."
                HelperStatus.PENDING -> "Waiting for helper confirmation."
                HelperStatus.NOT_SET -> "No helper set. Please provide your helper's email and request."
            }
            speechHelper.speak("Helper Screen. $helperMessage Double tap buttons to activate.")
        }
    }

    // Clean up (unchanged)
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
            shareButtonClickJob?.cancel() // Cancel coroutine job
        }
    }

    // Functions (unchanged logic)
    val callHelper = {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:777777777777") // Placeholder number
        }
        context.startActivity(intent)
    }

    val sendMessageToHelper = {
        speechHelper.speak("Sending help request message to your helper.")
        // Add actual in-app messaging logic here
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
             Color.White
            )
            .padding(16.dp)
            .clickable {
                speechHelper.speak(
                    "Helper page. Double tap on buttons to activate them."
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .size(50.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back_icon),
                    contentDescription = "Back",
                    modifier = Modifier.size(35.dp)
                )
            }

            // Page Title
            Text(
                text = "Helper",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans, // Your font
                color = AppColors.darkBlue // Your dark blue
            )

            // Voice assistance button
            IconButton(
                onClick = {
                    // Re-speak current status
                    val positionStatus = if (isPositionShared)
                        "Position sharing is active."
                    else
                        "Position sharing is inactive."
                    val helperMessage = when (helperStatus) {
                        HelperStatus.CONFIRMED -> "Helper Confirmed. $positionStatus Use buttons to interact."
                        HelperStatus.PENDING -> "Waiting for helper confirmation."
                        HelperStatus.NOT_SET -> "No helper set. Enter email and request."
                    }
                    speechHelper.speak(helperMessage + " Double tap buttons to activate.")
                },
                modifier = Modifier
                    .size(40.dp)
                    // Use White with transparency or your lightBlue for background
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "Voice Guide",
                )
            }
        }

        // --- Content Area (Dynamically changes) ---
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (helperStatus) {
                HelperStatus.CONFIRMED -> {
                    ConfirmedHelperContent(
                        isPositionShared = isPositionShared,
                        onSharePositionClick = {
                            // Double tap logic using Coroutines
                            if (shareButtonClickJob?.isActive == true) {
                                shareButtonClickJob?.cancel()
                                shareButtonClickJob = null
                                isPositionShared = !isPositionShared
                                val confirmationMessage = if (isPositionShared)
                                    "Position sharing activated."
                                else
                                    "Position sharing stopped."
                                speechHelper.speak(confirmationMessage)
                            } else {
                                val buttonText = if (isPositionShared)
                                    "Stop sharing position button."
                                else
                                    "Share position button."
                                speechHelper.speak("$buttonText Double tap to activate.")
                                shareButtonClickJob = scope.launch {
                                    delay(doubleTapDelay)
                                    shareButtonClickJob = null
                                }
                            }
                        },
                        onCallHelperClick = {
                            speechHelper.speak("Calling your helper.")
                            callHelper()
                        },
                        onRequestHelpClick = {
                            speechHelper.speak("Sending help request message.")
                            sendMessageToHelper()
                        }
                    )
                }
                HelperStatus.PENDING -> {
                    PendingHelperContent()
                }
                HelperStatus.NOT_SET -> {
                    NotSetHelperContent(
                        helperEmail = helperEmail,
                        onEmailChange = { helperEmail = it },
                        onRequestClick = {
                            if (helperEmail.isNotBlank() && "@" in helperEmail) { // Simple validation
                                helperStatus = HelperStatus.PENDING
                                speechHelper.speak("Request sent to $helperEmail.")
                                // Add logic to actually send the request
                            } else {
                                speechHelper.speak("Please enter a valid email address first.")
                            }
                        }
                    )
                }
            }
        }
    }
}
*/
@Composable
fun ConfirmedHelperContent(
    isPositionShared: Boolean,
    onSharePositionClick: () -> Unit,
    onCallHelperClick: () -> Unit,
    onRequestHelpClick: () -> Unit
) {
    // Animate background color change for the share button
    val shareButtonColor by animateColorAsState(
        // Use Primary (Orange) when ON, lightBlue when OFF
        targetValue = if (isPositionShared) AppColors.primary else AppColors.lightBlue,
        label = "ShareButtonColorAnimation"
    )
    val shareContentColor by animateColorAsState(
        // Use White text on Orange, darkBlue text on lightBlue
        targetValue = if (isPositionShared) Color.White else AppColors.darkBlue,
        label = "ShareContentColorAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Share Position Button --- (Uses WowHelperButton)
        WowHelperButton(
            text = if (isPositionShared) "Sharing Position" else "Share Position",
            icon = if (isPositionShared) Icons.Default.LocationOn else Icons.Default.LocationOff,
            onClick = onSharePositionClick,
            backgroundColor = shareButtonColor, // Animated color
            contentColor = shareContentColor, // Animated color
        )

        // --- Call Helper Button --- (Uses WowHelperButton with Primary color)
        WowHelperButton(
            text = "Call Helper",
            icon = Icons.Default.Phone,
            onClick = onCallHelperClick,
            backgroundColor = AppColors.primary, // Your primary orange
            contentColor = Color.White
        )

        // --- Request Help Button --- (Uses WowHelperButton with WritingBlue color)
        WowHelperButton(
            text = "Request Help",
            icon = Icons.Default.Send,
            onClick = onRequestHelpClick,
            // Use writingBlue for a secondary action color
            backgroundColor = AppColors.writingBlue,
            contentColor = Color.White ,
            color = AppColors.darkBlue// White text should contrast well with writingBlue
        )
    }
}

@Composable
fun PendingHelperContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(20.dp))
            // Use your lightBlue with transparency for background
            .background(Color.White)
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Pending Confirmation",
            textAlign = TextAlign.Center,
            fontFamily = PlusJakartaSans, // Your font
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AppColors.darkBlue // Your dark blue
        )
        Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            tint = AppColors.primary, // Your primary orange for the icon
            modifier = Modifier.size(60.dp)
        )
        Text(
            text = "Waiting for your designated helper to accept your request.",
            textAlign = TextAlign.Center,
            fontFamily = PlusJakartaSans, // Your font
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            // Use writingBlue or darkBlue for body text
            color = AppColors.writingBlue.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun NotSetHelperContent(
    helperEmail: String,
    onEmailChange: (String) -> Unit,
    onRequestClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set Up Your Helper",
            textAlign = TextAlign.Center,
            fontFamily = PlusJakartaSans, // Your font
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AppColors.darkBlue, // Your dark blue
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Styled TextField using your colors
        OutlinedTextField(
            value = helperEmail,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .border(
                    width = 4.dp,
                    color = AppColors.primary, // Couleur personnalisée
                    shape = RoundedCornerShape(20.dp)
                ),
            label = {
                Text(
                    "Helper's Email Address",
                    fontFamily = PlusJakartaSans,
                    fontSize = 20.sp
                )
            },
            placeholder = null,

            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent, // Supprime la bordure native
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = AppColors.primary,
                cursorColor = AppColors.primary,

                focusedTextColor = AppColors.darkBlue,
                unfocusedTextColor = AppColors.darkBlue
            ),
            textStyle = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = AppColors.darkBlue
            ),
            shape = RoundedCornerShape(16.dp)
        )


        VoiceRecorderButton(
                onVoiceInput = { recognizedText ->
                    onEmailChange(recognizedText)
                }
            )

        // --- Request Helper Button --- (Uses WowHelperButton with Primary color)
        WowHelperButton(
            text = "Send Request",
            icon = Icons.Default.CheckCircle,
            onClick = onRequestClick,
            backgroundColor = AppColors.primary, // Use your primary orange for main action
            contentColor = Color.White
        )
    }
}