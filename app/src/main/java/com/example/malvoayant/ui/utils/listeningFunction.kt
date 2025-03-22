package com.example.malvoayant.ui.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

fun startListening(context: Context, onResult: (String) -> Unit) {
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                val spokenText = it.firstOrNull() ?: ""
                onResult(spokenText)  // Send recognized text to UI
            }
        }

        override fun onError(error: Int) {
            Toast.makeText(context, "Speech Recognition Error: $error", Toast.LENGTH_SHORT).show()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    speechRecognizer.startListening(speechIntent)
}

fun fixSpokenEmail(text: String): String {
    val letterMap = mapOf(
        "a" to "a", "b" to "b", "c" to "c", "d" to "d", "e" to "e",
        "f" to "f", "g" to "g", "h" to "h", "i" to "i", "j" to "j",
        "k" to "k", "l" to "l", "m" to "m", "n" to "n", "o" to "o",
        "p" to "p", "q" to "q", "r" to "r", "s" to "s", "t" to "t",
        "u" to "u", "v" to "v", "w" to "w", "x" to "x", "y" to "y", "z" to "z",
        "underscore" to "_", "dot" to ".", "at" to "@", "dash" to "-"
    )

    return text.lowercase().split(" ").map { letterMap[it] ?: it }.joinToString("")
}


