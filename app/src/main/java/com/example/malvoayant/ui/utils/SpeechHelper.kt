package com.example.malvoayant.ui.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

/**
 * Helper class to manage Text-to-Speech functionality
 */
class SpeechHelper(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null

    /**
     * Initialize the TextToSpeech engine
     * @param onInitialized Callback to be executed when initialization is complete
     */
    fun initializeSpeech(onInitialized: () -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.ENGLISH
                onInitialized()
            } else {
                println("TextToSpeech initialization failed with status: $status")
            }
        }
    }


    /**
     * Speak the provided text
     * @param text The text to be spoken
     */
    fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * Shutdown the TextToSpeech engine to free resources
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}