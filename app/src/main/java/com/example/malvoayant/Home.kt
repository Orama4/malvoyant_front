
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import java.util.*

@Composable
fun HomeScreen( context: Context) {

    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    textToSpeech?.language = Locale.ENGLISH
    fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    LaunchedEffect(context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {

                // Speak after initialization is complete
                speak("Welcome to Irchad application. This page will help you navigate to the register or connection page. Press the minus button to go to the register page and the plus button to go to the connection page.")
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable {
                speak("If you encounter difficulties, press anywhere to activate TalkBack. " +
                        "Press the top left corner to hear the page description.")
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1B2A))
                    .padding(16.dp)
            ) {
                Text(
                    text = "WELCOME TO IRCHAD",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).clickable { speak("Welcome to Irchad") }


                )

                IconButton(
                    onClick = { speak("Welcome to Irchad application. This page will help you navigate to the register or connection page.") },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_btn_speak_now),
                        contentDescription = "Voice Guide",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Please choose to sign up or log in to your existing account",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp).clickable { speak("Please choose to sign up or log in to your existing account")}
                    )

            Spacer(modifier = Modifier.height(20.dp))

            // Bouton Register
            Button(
                onClick = {
                    speak("Register button, navigating to registration page.")
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE76F00)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "REGISTER 🧑", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "OR", fontSize = 18.sp,modifier = Modifier.clickable { speak("Or") })

            Spacer(modifier = Modifier.height(10.dp))

            // Bouton Login
            Button(
                onClick = {
                    speak("Login button, navigating to login page.")
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE76F00)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "LOGIN ➡️", fontSize = 18.sp)
            }
        }
    }
}
/*
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceRecognitionScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val TAG = "VoiceRecognition"

    var recognizedText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSpeechRecognizerAvailable by remember {
        mutableStateOf(SpeechRecognizer.isRecognitionAvailable(context))
    }
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            try {
                startListening(context, speechRecognizer) { text ->
                    recognizedText = text
                }
                isListening = true
                errorMessage = null
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition after permission granted", e)
                errorMessage = "Erreur: ${e.message}"
                isListening = false
                Toast.makeText(context, "Impossible de démarrer la reconnaissance vocale", Toast.LENGTH_SHORT).show()
            }
        } else {
            errorMessage = "Permission de microphone refusée"
            Toast.makeText(context, "Permission microphone nécessaire", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                Log.d(TAG, "SpeechRecognizer initialized successfully")
            } else {
                Log.e(TAG, "Speech recognition is not available on this device")
                errorMessage = "La reconnaissance vocale n'est pas disponible sur cet appareil"
                isSpeechRecognizerAvailable = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
            errorMessage = "Erreur d'initialisation: ${e.message}"
            isSpeechRecognizerAvailable = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying speech recognizer", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1B2A))
                    .padding(16.dp)
            ) {
                Text(
                    text = "RECONNAISSANCE VOCALE",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status message for debugging
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFECEC)
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Recognized text display
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(200.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = recognizedText.ifEmpty { "Ce que vous dites apparaîtra ici..." },
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = if (recognizedText.isEmpty()) Color.Gray else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Listening status indicator
            if (isListening) {
                Text(
                    text = "Écoute en cours...",
                    color = Color(0xFFE76F00),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Animation dots for listening indicator
                var dots by remember { mutableStateOf("") }
                LaunchedEffect(isListening) {
                    while (isListening) {
                        dots = when (dots) {
                            "" -> "."
                            "." -> ".."
                            ".." -> "..."
                            else -> ""
                        }
                        delay(500)
                    }
                }
                Text(
                    text = dots,
                    color = Color(0xFFE76F00),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Microphone button
            Button(
                onClick = {
                    if (isListening) {
                        try {
                            speechRecognizer?.stopListening()
                            isListening = false
                            Log.d(TAG, "Stopped listening")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error stopping speech recognition", e)
                            errorMessage = "Erreur d'arrêt: ${e.message}"
                        }
                    } else {
                        if (!isSpeechRecognizerAvailable) {
                            Toast.makeText(
                                context,
                                "La reconnaissance vocale n'est pas disponible sur cet appareil",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        if (hasRecordAudioPermission) {
                            try {
                                startListening(context, speechRecognizer) { text ->
                                    recognizedText = text
                                }
                                isListening = true
                                errorMessage = null
                                Log.d(TAG, "Started listening")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error starting speech recognition", e)
                                errorMessage = "Erreur de démarrage: ${e.message}"
                                Toast.makeText(
                                    context,
                                    "Erreur lors du démarrage de la reconnaissance vocale",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            Log.d(TAG, "Requesting RECORD_AUDIO permission")
                        }
                    }
                },
                modifier = Modifier
                    .size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFCC0000) else Color(0xFFE76F00),
                    disabledContainerColor = Color.Gray
                ),
                enabled = isSpeechRecognizerAvailable
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isListening)
                            android.R.drawable.ic_media_pause
                        else
                            android.R.drawable.ic_btn_speak_now
                    ),
                    contentDescription = if (isListening) "Arrêter l'écoute" else "Commencer l'écoute",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (!isSpeechRecognizerAvailable)
                    "Reconnaissance vocale indisponible"
                else if (isListening)
                    "Appuyez pour arrêter"
                else
                    "Appuyez pour commencer",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Check microphone availability button
            Button(
                onClick = {
                    val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
                    isSpeechRecognizerAvailable = isAvailable
                    if (isAvailable) {
                        Toast.makeText(context, "Reconnaissance vocale disponible", Toast.LENGTH_SHORT).show()
                        errorMessage = null
                        try {
                            if (speechRecognizer == null) {
                                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                            }
                        } catch (e: Exception) {
                            errorMessage = "Erreur de réinitialisation: ${e.message}"
                        }
                    } else {
                        Toast.makeText(context, "Reconnaissance vocale non disponible", Toast.LENGTH_SHORT).show()
                        errorMessage = "La reconnaissance vocale n'est pas disponible sur cet appareil"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Vérifier disponibilité microphone")
            }

            // Clear button
            if (recognizedText.isNotEmpty()) {
                Button(
                    onClick = { recognizedText = "" },
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF888888)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Effacer", fontSize = 16.sp)
                }
            }
        }
    }
}

fun startListening(
    context: Context,
    speechRecognizer: SpeechRecognizer?,
    onResult: (String) -> Unit
) {
    if (speechRecognizer == null) {
        throw IllegalStateException("SpeechRecognizer n'est pas initialisé")
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceRecognition", "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            Log.d("VoiceRecognition", "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Changement de volume détecté
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.d("VoiceRecognition", "onBufferReceived")
        }

        override fun onEndOfSpeech() {
            Log.d("VoiceRecognition", "onEndOfSpeech")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions insuffisantes"
                SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout réseau"
                SpeechRecognizer.ERROR_NO_MATCH -> "Aucune correspondance"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconnaissance occupée"
                SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout parole"
                else -> "Erreur inconnue"
            }
            Log.e("VoiceRecognition", "onError: $errorMessage")

            // Afficher un toast avec l'erreur
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        override fun onResults(results: Bundle?) {
            Log.d("VoiceRecognition", "onResults")
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                Log.d("VoiceRecognition", "Recognized: $recognizedText")
                onResult(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            Log.d("VoiceRecognition", "onPartialResults")
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                Log.d("VoiceRecognition", "Partial recognized: $recognizedText")
                onResult(recognizedText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d("VoiceRecognition", "onEvent: $eventType")
        }
    })

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR") // French language
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    try {
        speechRecognizer.startListening(intent)
    } catch (e: Exception) {
        Log.e("VoiceRecognition", "Error in startListening", e)
        throw e
    }
}
*/